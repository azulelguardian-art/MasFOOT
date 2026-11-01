"""MasFOOT Live Finder.

Cruza los partidos del día con los catálogos de emisión AUTORIZADOS.
Reglas duras:
  * Solo se aceptan ofertas con URL https oficial.
  * Solo se reproduce en la app (EMBEDDED_*) si el proveedor marca integration_allowed;
    si no, se degrada a WEB_LINK (se abre la plataforma oficial).
  * No se infiere ni se inventa ninguna oferta: sin catálogo => sin retransmisión.
"""
from __future__ import annotations

import logging
from datetime import date, datetime, timezone
from typing import Protocol
from urllib.parse import urlparse

import httpx

from app.models import (AccessType, BroadcastOffer, Fixture, FixtureWithOffers,
                        PlayerType)
from app.normalize import fixture_dedup_key

log = logging.getLogger("masfoot.livefinder")

NO_BROADCAST = "NO HAY RETRANSMISIÓN LEGAL DISPONIBLE."


class BroadcastProvider(Protocol):
    """Adaptador de un catálogo autorizado. Añadir uno nuevo no toca el núcleo."""

    name: str

    async def offers_for_day(self, day: date, country: str) -> list[BroadcastOffer]: ...


class AuthorizedCatalogProvider:
    """Cliente genérico de un catálogo con el que exista acuerdo/API oficial.

    Contrato esperado (ver docs/CATALOGO_EMISIONES.md):
      GET {url}/offers?date=YYYY-MM-DD&country=ES   Authorization: Bearer <token>
      -> [{"home","away","kickoff_utc","provider","language","access_type",
           "access_condition","player_type","official_url","integration_allowed"}]
    """

    def __init__(self, url: str, token: str, client: httpx.AsyncClient | None = None):
        self.name = "catalogo-autorizado"
        self._url = url.rstrip("/")
        self._token = token
        self._client = client or httpx.AsyncClient(timeout=15)

    async def offers_for_day(self, day: date, country: str) -> list[BroadcastOffer]:
        r = await self._client.get(
            f"{self._url}/offers",
            params={"date": day.isoformat(), "country": country},
            headers={"Authorization": f"Bearer {self._token}"},
        )
        r.raise_for_status()
        offers = []
        for raw in r.json():
            offer = parse_offer(raw, country)
            if offer:
                offers.append(offer)
        return offers


def parse_offer(raw: dict, country: str) -> BroadcastOffer | None:
    try:
        url = raw["official_url"]
        if urlparse(url).scheme != "https":
            return None
        kickoff = datetime.fromisoformat(raw["kickoff_utc"].replace("Z", "+00:00"))
        player = PlayerType(raw["player_type"])
        allowed = bool(raw.get("integration_allowed", False))
        if player in (PlayerType.EMBEDDED_HLS, PlayerType.EMBEDDED_DASH) and not allowed:
            player = PlayerType.WEB_LINK  # sin permiso de integración: solo enlace oficial
        return BroadcastOffer(
            dedup_key=fixture_dedup_key(raw["home"], raw["away"], kickoff),
            provider=raw["provider"],
            country=country,
            language=raw["language"],
            access_type=AccessType(raw["access_type"]),
            access_condition=raw.get("access_condition"),
            player_type=player,
            official_url=url,
            integration_allowed=allowed,
            last_checked=datetime.now(timezone.utc),
        )
    except (KeyError, ValueError, TypeError):
        log.warning("Oferta de emisión descartada por inválida")
        return None


class LiveFinder:
    def __init__(self, providers: list[BroadcastProvider]):
        self._providers = providers

    async def find(self, fixtures: list[Fixture], day: date, country: str) -> list[FixtureWithOffers]:
        offers: list[BroadcastOffer] = []
        for p in self._providers:
            try:
                offers.extend(await p.offers_for_day(day, country))
            except Exception as exc:  # un proveedor caído no rompe al resto
                log.error("Proveedor %s falló: %s", getattr(p, "name", "?"), exc)
        by_key: dict[str, list[BroadcastOffer]] = {}
        for o in offers:
            by_key.setdefault(o.dedup_key, []).append(o)
        rank = {AccessType.FREE: 0, AccessType.FREE_REGISTRATION: 1, AccessType.SUBSCRIPTION: 2}
        result = []
        for fx in fixtures:
            fx_offers = sorted(by_key.get(fx.dedup_key, []), key=lambda o: rank[o.access_type])
            result.append(FixtureWithOffers(
                fixture=fx,
                offers=fx_offers,
                has_legal_broadcast=bool(fx_offers),
                message=None if fx_offers else NO_BROADCAST,
            ))
        return result


def filter_offers(items: list[FixtureWithOffers], mode: str = "all", language: str | None = None):
    """mode: all | free | free_registration | subscription. Nunca marca de pago como gratis."""
    wanted = {
        "all": None,
        "free": {AccessType.FREE},
        "free_registration": {AccessType.FREE_REGISTRATION},
        "subscription": {AccessType.SUBSCRIPTION},
    }[mode]
    out = []
    for it in items:
        offs = [o for o in it.offers
                if (wanted is None or o.access_type in wanted) and (language is None or o.language == language)]
        if offs or (mode == "all" and language is None):
            out.append(it.model_copy(update={
                "offers": offs,
                "has_legal_broadcast": bool(offs),
                "message": None if offs else NO_BROADCAST,
            }))
    return out

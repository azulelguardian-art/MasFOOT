"""Adaptadores de datos deportivos.

Proveedor principal: football-data.org (API v4, cabecera X-Auth-Token).
Segundo proveedor: API-Football (stub preparado; requiere API_FOOTBALL_KEY).
"""
from __future__ import annotations

from datetime import date, datetime, timedelta, timezone
from typing import Protocol

import httpx

from app.models import Competition, Fixture, MatchStatus, Team
from app.normalize import fixture_dedup_key

_STATUS = {
    "SCHEDULED": MatchStatus.SCHEDULED,
    "TIMED": MatchStatus.SCHEDULED,
    "IN_PLAY": MatchStatus.LIVE,
    "LIVE": MatchStatus.LIVE,
    "PAUSED": MatchStatus.PAUSED,
    "FINISHED": MatchStatus.FINISHED,
    "AWARDED": MatchStatus.FINISHED,
    "POSTPONED": MatchStatus.POSTPONED,
    "SUSPENDED": MatchStatus.POSTPONED,
    "CANCELLED": MatchStatus.POSTPONED,
}


class SportsProvider(Protocol):
    name: str

    async def fixtures_for_day(self, day: date) -> list[Fixture]: ...


def _parse_dt(s: str) -> datetime:
    return datetime.fromisoformat(s.replace("Z", "+00:00"))


def parse_football_data_match(m: dict, now: datetime | None = None) -> Fixture | None:
    """Valida y convierte un partido de football-data.org. Devuelve None si es inválido."""
    try:
        home, away = m["homeTeam"], m["awayTeam"]
        if not home.get("name") or not away.get("name"):
            return None  # p. ej. eliminatoria con equipos aún por decidir
        kickoff = _parse_dt(m["utcDate"])
        comp = m["competition"]
        ft = (m.get("score") or {}).get("fullTime") or {}
        return Fixture(
            dedup_key=fixture_dedup_key(home["name"], away["name"], kickoff),
            source="football-data.org",
            external_id=str(m["id"]),
            competition=Competition(
                external_id=str(comp["id"]),
                name=comp["name"],
                country=(m.get("area") or {}).get("name"),
                emblem_url=comp.get("emblem"),
            ),
            home=Team(name=home["name"], short_name=home.get("shortName"), crest_url=home.get("crest")),
            away=Team(name=away["name"], short_name=away.get("shortName"), crest_url=away.get("crest")),
            kickoff_utc=kickoff,
            status=_STATUS.get(m.get("status", ""), MatchStatus.OTHER),
            home_score=ft.get("home"),
            away_score=ft.get("away"),
            minute=m.get("minute"),
            venue=m.get("venue"),
            updated_at=now or datetime.now(timezone.utc),
        )
    except (KeyError, ValueError, TypeError):
        return None


class FootballDataProvider:
    name = "football-data.org"
    BASE = "https://api.football-data.org/v4"

    def __init__(self, api_key: str, client: httpx.AsyncClient | None = None):
        self._key = api_key
        self._client = client or httpx.AsyncClient(timeout=15)

    async def fixtures_for_day(self, day: date) -> list[Fixture]:
        if not self._key:
            raise RuntimeError("FOOTBALL_DATA_API_KEY no configurada")
        # dateTo es exclusivo en esta API: pedimos [día, día+1)
        r = await self._client.get(
            f"{self.BASE}/matches",
            params={"dateFrom": day.isoformat(), "dateTo": (day + timedelta(days=1)).isoformat()},
            headers={"X-Auth-Token": self._key},
        )
        r.raise_for_status()
        out = []
        for m in r.json().get("matches", []):
            fx = parse_football_data_match(m)
            if fx and fx.kickoff_utc.date() == day:
                out.append(fx)
        return sorted(out, key=lambda f: f.kickoff_utc)


class ApiFootballProvider:
    """Segundo proveedor, pendiente de implementar. No devuelve datos inventados."""

    name = "api-football"

    def __init__(self, api_key: str):
        self._key = api_key

    async def fixtures_for_day(self, day: date) -> list[Fixture]:
        raise NotImplementedError("Adaptador API-Football pendiente de implementar")

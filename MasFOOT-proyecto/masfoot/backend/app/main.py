from __future__ import annotations

import hmac
import logging
import time
from datetime import date, datetime, timezone

from fastapi import Depends, FastAPI, Header, HTTPException, Query

from app.config import settings
from app.live_finder import AuthorizedCatalogProvider, LiveFinder, filter_offers
from app.models import FixtureWithOffers
from app.providers.sports import FootballDataProvider

logging.basicConfig(level=logging.INFO)
app = FastAPI(title="MasFOOT API", version="0.1.0")

SPORTIUM_URL = "https://www.sportium.es/"
_sports = FootballDataProvider(settings.football_data_api_key)
_catalogs = (
    [AuthorizedCatalogProvider(settings.broadcast_catalog_url, settings.broadcast_catalog_token)]
    if settings.broadcast_catalog_url else []
)
_finder = LiveFinder(_catalogs)

# Caché en memoria con TTL: evita gastar cuota si los datos no han cambiado.
_cache: dict[tuple, tuple[float, object]] = {}


def require_key(x_api_key: str = Header(default="")):
    if not settings.masfoot_api_key or not hmac.compare_digest(x_api_key, settings.masfoot_api_key):
        raise HTTPException(status_code=401, detail="API key inválida")


async def _day(day: date, country: str) -> dict:
    key = (day, country)
    now = time.time()
    hit = _cache.get(key)
    live_ttl = settings.sync_live_interval
    if hit and now - hit[0] < live_ttl:
        return hit[1]  # type: ignore[return-value]
    try:
        fixtures = await _sports.fixtures_for_day(day)
    except Exception as exc:
        if hit:
            return {**hit[1], "stale": True}  # type: ignore[arg-type]
        raise HTTPException(status_code=502, detail=f"Fuente deportiva no disponible: {exc}")
    items = await _finder.find(fixtures, day, country)
    payload = {
        "source": _sports.name,
        "updated_at": datetime.now(timezone.utc).isoformat(),
        "coverage_note": "Cobertura limitada a las competiciones incluidas en el plan de la fuente de datos.",
        "catalogs_configured": len(_catalogs),
        "items": items,
        "stale": False,
    }
    _cache[key] = (now, payload)
    return payload


@app.get("/health")
async def health():
    return {"ok": True}


@app.get("/v1/matches", dependencies=[Depends(require_key)])
async def matches(
    day: date | None = None,
    country: str = Query(default=settings.default_country, min_length=2, max_length=2),
    free: str = Query(default="all", pattern="^(all|free|free_registration|subscription)$"),
    language: str | None = Query(default=None, pattern="^(es|fr|it|pt|ar|en)$"),
):
    data = await _day(day or datetime.now(timezone.utc).date(), country.upper())
    items: list[FixtureWithOffers] = filter_offers(data["items"], free, language)
    return {**data, "items": items}


@app.get("/v1/odds/{dedup_key}", dependencies=[Depends(require_key)])
async def odds(dedup_key: str):
    """Cuotas Sportium: sin integración autorizada NO se inventa ningún dato."""
    if not settings.sportium_api_url:
        return {"available": False, "reason": "Sin acceso autorizado a datos de Sportium",
                "sportium_url": SPORTIUM_URL, "cta": "APOSTAR EN SPORTIUM"}
    raise HTTPException(status_code=501, detail="Integración Sportium pendiente de contrato")

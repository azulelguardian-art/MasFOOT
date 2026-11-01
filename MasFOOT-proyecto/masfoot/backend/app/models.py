from __future__ import annotations

from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field


class MatchStatus(str, Enum):
    SCHEDULED = "SCHEDULED"
    LIVE = "LIVE"
    PAUSED = "PAUSED"
    FINISHED = "FINISHED"
    POSTPONED = "POSTPONED"
    OTHER = "OTHER"


class AccessType(str, Enum):
    FREE = "FREE"
    FREE_REGISTRATION = "FREE_REGISTRATION"
    SUBSCRIPTION = "SUBSCRIPTION"


class PlayerType(str, Enum):
    EMBEDDED_HLS = "EMBEDDED_HLS"
    EMBEDDED_DASH = "EMBEDDED_DASH"
    EXTERNAL_APP = "EXTERNAL_APP"
    WEB_LINK = "WEB_LINK"


class Team(BaseModel):
    name: str
    short_name: str | None = None
    crest_url: str | None = None


class Competition(BaseModel):
    external_id: str
    name: str
    country: str | None = None
    emblem_url: str | None = None


class Fixture(BaseModel):
    dedup_key: str
    source: str
    external_id: str
    competition: Competition
    home: Team
    away: Team
    kickoff_utc: datetime
    status: MatchStatus
    home_score: int | None = None
    away_score: int | None = None
    minute: int | None = None
    venue: str | None = None
    updated_at: datetime


class BroadcastOffer(BaseModel):
    dedup_key: str  # fixture al que pertenece
    provider: str
    country: str
    language: str = Field(pattern="^(es|fr|it|pt|ar|en)$")
    access_type: AccessType
    access_condition: str | None = None
    player_type: PlayerType
    official_url: str
    integration_allowed: bool = False
    last_checked: datetime


class FixtureWithOffers(BaseModel):
    fixture: Fixture
    offers: list[BroadcastOffer]
    has_legal_broadcast: bool
    message: str | None = None  # "NO HAY RETRANSMISIÓN LEGAL DISPONIBLE." cuando no hay ofertas

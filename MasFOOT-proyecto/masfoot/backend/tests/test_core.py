from datetime import date, datetime, timezone

import pytest

from app.live_finder import LiveFinder, NO_BROADCAST, filter_offers, parse_offer
from app.models import AccessType, PlayerType
from app.normalize import fixture_dedup_key, normalize_team_name
from app.providers.sports import parse_football_data_match

KICK = "2026-11-01T20:00:00Z"
MATCH = {
    "id": 1, "utcDate": KICK, "status": "IN_PLAY",
    "area": {"name": "Spain"},
    "competition": {"id": 2014, "name": "Primera Division", "emblem": "x"},
    "homeTeam": {"name": "Real Madrid CF", "shortName": "Real Madrid", "crest": "c1"},
    "awayTeam": {"name": "FC Barcelona", "shortName": "Barça", "crest": "c2"},
    "score": {"fullTime": {"home": 1, "away": 0}},
}


def offer_raw(**kw):
    base = {"home": "Real Madrid", "away": "Barcelona", "kickoff_utc": KICK, "provider": "Oficial TV",
            "language": "es", "access_type": "FREE", "player_type": "EMBEDDED_HLS",
            "official_url": "https://example.org/live", "integration_allowed": True}
    return {**base, **kw}


def test_normalize_names():
    assert normalize_team_name("Real Madrid CF") == normalize_team_name("Real Madrid")
    assert normalize_team_name("FC Barcelona") == "barcelona"


def test_dedup_key_across_providers():
    d = datetime(2026, 11, 1, 20, tzinfo=timezone.utc)
    assert fixture_dedup_key("Real Madrid CF", "FC Barcelona", d) == fixture_dedup_key("Real Madrid", "Barcelona", d)


def test_parse_match_ok_and_invalid():
    fx = parse_football_data_match(MATCH)
    assert fx.status.value == "LIVE" and fx.home_score == 1
    assert parse_football_data_match({"id": 2}) is None
    bad = {**MATCH, "homeTeam": {"name": None}}
    assert parse_football_data_match(bad) is None


def test_offer_rejects_http_and_downgrades_without_permission():
    assert parse_offer(offer_raw(official_url="http://x.org"), "ES") is None
    o = parse_offer(offer_raw(integration_allowed=False), "ES")
    assert o.player_type == PlayerType.WEB_LINK
    assert parse_offer(offer_raw(language="xx"), "ES") is None


class FakeProvider:
    name = "fake"

    def __init__(self, raws):
        self.raws = raws

    async def offers_for_day(self, day, country):
        return [o for o in (parse_offer(r, country) for r in self.raws) if o]


@pytest.mark.asyncio
async def test_finder_sorts_free_first_and_reports_none():
    fx = parse_football_data_match(MATCH)
    other = parse_football_data_match({**MATCH, "id": 9, "homeTeam": {"name": "Getafe"}, "awayTeam": {"name": "Elche"}})
    finder = LiveFinder([FakeProvider([offer_raw(access_type="SUBSCRIPTION"), offer_raw(access_type="FREE")])])
    res = await finder.find([fx, other], date(2026, 11, 1), "ES")
    assert [o.access_type for o in res[0].offers] == [AccessType.FREE, AccessType.SUBSCRIPTION]
    assert res[1].offers == [] and res[1].message == NO_BROADCAST


@pytest.mark.asyncio
async def test_free_filter_never_includes_paid():
    fx = parse_football_data_match(MATCH)
    finder = LiveFinder([FakeProvider([offer_raw(access_type="SUBSCRIPTION")])])
    res = await finder.find([fx], date(2026, 11, 1), "ES")
    assert filter_offers(res, "free") == []


@pytest.mark.asyncio
async def test_no_providers_means_no_broadcast():
    fx = parse_football_data_match(MATCH)
    res = await LiveFinder([]).find([fx], date(2026, 11, 1), "ES")
    assert not res[0].has_legal_broadcast

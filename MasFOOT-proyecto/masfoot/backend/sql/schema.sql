-- Esquema PostgreSQL de MasFOOT
-- No se almacenan enlaces privados ni credenciales de streaming.

CREATE TABLE IF NOT EXISTS countries (
    code        TEXT PRIMARY KEY,           -- ISO 3166-1 alpha-2 o código de la fuente
    name        TEXT NOT NULL,
    flag_url    TEXT
);

CREATE TABLE IF NOT EXISTS languages (
    code        TEXT PRIMARY KEY,           -- es, fr, it, pt, ar, en
    name        TEXT NOT NULL
);
INSERT INTO languages (code, name) VALUES
    ('es','Español'),('fr','Francés'),('it','Italiano'),
    ('pt','Portugués'),('ar','Árabe'),('en','Inglés')
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS data_sources (
    id          SERIAL PRIMARY KEY,
    name        TEXT UNIQUE NOT NULL,       -- football-data.org, api-football...
    kind        TEXT NOT NULL CHECK (kind IN ('sports','broadcast','odds')),
    daily_quota INTEGER,
    notes       TEXT
);

CREATE TABLE IF NOT EXISTS competitions (
    id          SERIAL PRIMARY KEY,
    source_id   INTEGER REFERENCES data_sources(id),
    external_id TEXT NOT NULL,
    name        TEXT NOT NULL,
    country_code TEXT REFERENCES countries(code),
    emblem_url  TEXT,
    UNIQUE (source_id, external_id)
);

CREATE TABLE IF NOT EXISTS teams (
    id          SERIAL PRIMARY KEY,
    source_id   INTEGER REFERENCES data_sources(id),
    external_id TEXT NOT NULL,
    name        TEXT NOT NULL,
    short_name  TEXT,
    normalized_name TEXT NOT NULL,          -- usado para deduplicar entre proveedores
    crest_url   TEXT,
    country_code TEXT REFERENCES countries(code),
    UNIQUE (source_id, external_id)
);
CREATE INDEX IF NOT EXISTS idx_teams_norm ON teams(normalized_name);

CREATE TABLE IF NOT EXISTS fixtures (
    id          SERIAL PRIMARY KEY,
    dedup_key   TEXT UNIQUE NOT NULL,       -- hash(equipo local normalizado, visitante, fecha UTC)
    source_id   INTEGER REFERENCES data_sources(id),
    external_id TEXT NOT NULL,
    competition_id INTEGER REFERENCES competitions(id),
    home_team_id INTEGER REFERENCES teams(id),
    away_team_id INTEGER REFERENCES teams(id),
    kickoff_utc TIMESTAMPTZ NOT NULL,
    status      TEXT NOT NULL,              -- SCHEDULED, LIVE, PAUSED, FINISHED, POSTPONED
    minute      INTEGER,
    home_score  INTEGER,
    away_score  INTEGER,
    venue       TEXT,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fixtures_kickoff ON fixtures(kickoff_utc);

CREATE TABLE IF NOT EXISTS fixture_events (
    id          SERIAL PRIMARY KEY,
    fixture_id  INTEGER REFERENCES fixtures(id) ON DELETE CASCADE,
    minute      INTEGER,
    type        TEXT NOT NULL,              -- GOAL, YELLOW, RED, SUBSTITUTION
    team_id     INTEGER REFERENCES teams(id),
    player_name TEXT,
    detail      TEXT
);

CREATE TABLE IF NOT EXISTS providers (
    id          SERIAL PRIMARY KEY,
    slug        TEXT UNIQUE NOT NULL,
    name        TEXT NOT NULL,
    official_url TEXT NOT NULL,
    allows_embedding BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS broadcasters (
    id          SERIAL PRIMARY KEY,
    provider_id INTEGER REFERENCES providers(id),
    name        TEXT NOT NULL,
    country_code TEXT REFERENCES countries(code)
);

CREATE TABLE IF NOT EXISTS broadcast_rights (
    id          SERIAL PRIMARY KEY,
    broadcaster_id INTEGER REFERENCES broadcasters(id),
    competition_id INTEGER REFERENCES competitions(id),
    country_code TEXT REFERENCES countries(code),
    valid_from  DATE,
    valid_to    DATE
);

CREATE TABLE IF NOT EXISTS broadcast_offers (
    id          SERIAL PRIMARY KEY,
    fixture_id  INTEGER REFERENCES fixtures(id) ON DELETE CASCADE,
    provider_id INTEGER REFERENCES providers(id),
    country_code TEXT NOT NULL,
    language_code TEXT REFERENCES languages(code),
    access_type TEXT NOT NULL CHECK (access_type IN ('FREE','FREE_REGISTRATION','SUBSCRIPTION')),
    access_condition TEXT,                  -- precio o condición, si se conoce
    player_type TEXT NOT NULL CHECK (player_type IN ('EMBEDDED_HLS','EMBEDDED_DASH','EXTERNAL_APP','WEB_LINK')),
    official_url TEXT NOT NULL,
    integration_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    availability TEXT NOT NULL DEFAULT 'AVAILABLE',
    last_checked TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_offers_fixture ON broadcast_offers(fixture_id);

CREATE TABLE IF NOT EXISTS favorites (
    id          SERIAL PRIMARY KEY,
    device_id   TEXT NOT NULL,
    kind        TEXT NOT NULL CHECK (kind IN ('TEAM','COMPETITION','FIXTURE')),
    ref_id      INTEGER NOT NULL,
    UNIQUE (device_id, kind, ref_id)
);

CREATE TABLE IF NOT EXISTS user_preferences (
    device_id   TEXT PRIMARY KEY,
    country_code TEXT,
    preferred_language TEXT REFERENCES languages(code),
    only_free   BOOLEAN NOT NULL DEFAULT FALSE,
    notify_start BOOLEAN NOT NULL DEFAULT TRUE,
    notify_goal BOOLEAN NOT NULL DEFAULT TRUE,
    notify_halftime BOOLEAN NOT NULL DEFAULT FALSE,
    notify_end  BOOLEAN NOT NULL DEFAULT TRUE,
    notify_broadcast BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS synchronization_logs (
    id          SERIAL PRIMARY KEY,
    source_id   INTEGER REFERENCES data_sources(id),
    started_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ,
    ok          BOOLEAN,
    items       INTEGER,
    error       TEXT
);

INSERT INTO data_sources (name, kind, daily_quota, notes) VALUES
    ('football-data.org','sports',NULL,'Plan gratuito: 10 llamadas/minuto, cobertura limitada de ligas'),
    ('api-football','sports',100,'Plan gratuito: 100 llamadas/día')
ON CONFLICT DO NOTHING;

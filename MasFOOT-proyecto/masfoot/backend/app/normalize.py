"""Normalización de nombres y claves de deduplicación entre proveedores."""
import hashlib
import re
import unicodedata

_NOISE = {"fc", "cf", "sc", "ac", "afc", "cd", "ud", "club", "de", "futbol", "football", "the"}


def normalize_team_name(name: str) -> str:
    s = unicodedata.normalize("NFKD", name)
    s = "".join(c for c in s if not unicodedata.combining(c)).lower()
    s = re.sub(r"[^a-z0-9 ]+", " ", s)
    tokens = [t for t in s.split() if t not in _NOISE]
    return " ".join(tokens)


def fixture_dedup_key(home: str, away: str, kickoff_utc) -> str:
    """Misma clave para el mismo partido venga de la fuente que venga (por fecha UTC)."""
    raw = f"{normalize_team_name(home)}|{normalize_team_name(away)}|{kickoff_utc.date().isoformat()}"
    return hashlib.sha1(raw.encode()).hexdigest()[:16]

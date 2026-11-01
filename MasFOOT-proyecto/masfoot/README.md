# MasFOOT

App Android de fútbol que localiza los partidos del día y sus **retransmisiones legales**
(prioriza las gratuitas). Licencia Apache 2.0. Idioma de la app: español.

> **Estado: en construcción (fase 2 parcial).** Lee [`STATUS.md`](STATUS.md) para saber qué
> está hecho, qué se ha probado de verdad y qué falta.

## Estructura

```
masfoot/
├── backend/            FastAPI: datos deportivos + MasFOOT Live Finder
│   ├── app/            código (config, models, normalize, providers, live_finder, main)
│   ├── sql/schema.sql  esquema PostgreSQL
│   └── tests/          pruebas pytest
├── docker-compose.yml  db (Postgres) + redis + api
├── .env.example        variables de entorno (copiar a .env)
└── LICENSE
```

## Arrancar el backend

```bash
cp .env.example .env        # rellena MASFOOT_API_KEY y FOOTBALL_DATA_API_KEY
docker compose up --build
curl -H "X-API-Key: <tu MASFOOT_API_KEY>" "http://localhost:8000/v1/matches?country=ES"
```

Pruebas sin Docker:

```bash
cd backend && pip install -r requirements.txt && python -m pytest -q
```

## Claves y proveedores externos

| Variable | Para qué | Dónde conseguirla |
|---|---|---|
| `FOOTBALL_DATA_API_KEY` | Calendario y resultados (proveedor principal) | https://www.football-data.org/client/register (plan gratuito: cobertura limitada) |
| `API_FOOTBALL_KEY` | Segundo proveedor (adaptador aún sin implementar) | https://www.api-football.com/ |
| `BROADCAST_CATALOG_URL` / `_TOKEN` | Catálogo de emisiones **autorizado** | Requiere acuerdo/API oficial con un proveedor; ver contrato abajo |
| `SPORTIUM_API_URL` / `_TOKEN` | Cuotas | Solo con integración autorizada con Sportium |

Las claves viven solo en el backend; la app Android nunca las contiene.

## Contrato del catálogo de emisiones

`LiveFinder` no extrae ni infiere enlaces de ninguna web. Solo consume catálogos con los que exista
permiso, que cumplan este contrato:

`GET {BROADCAST_CATALOG_URL}/offers?date=YYYY-MM-DD&country=ES` con `Authorization: Bearer <token>`

```json
[{"home":"...","away":"...","kickoff_utc":"2026-11-01T20:00:00Z","provider":"...",
  "language":"es","access_type":"FREE|FREE_REGISTRATION|SUBSCRIPTION",
  "access_condition":"...","player_type":"EMBEDDED_HLS|EMBEDDED_DASH|EXTERNAL_APP|WEB_LINK",
  "official_url":"https://...","integration_allowed":true}]
```

Reglas aplicadas: URL `https` obligatoria; sin `integration_allowed` el reproductor se degrada a enlace
oficial; si no hay oferta se muestra «NO HAY RETRANSMISIÓN LEGAL DISPONIBLE.»; una oferta de pago nunca
cuenta como gratuita. Para añadir otro proveedor basta implementar `BroadcastProvider`.

## Cuotas Sportium

Sin integración autorizada no se muestra ninguna cuota (nunca se inventan ni estiman). El backend devuelve
el enlace a https://www.sportium.es/ para el botón «APOSTAR EN SPORTIUM». La app no permite apostar.

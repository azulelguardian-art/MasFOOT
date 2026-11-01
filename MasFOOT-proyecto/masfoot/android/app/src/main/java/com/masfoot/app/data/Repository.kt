package com.masfoot.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.UnknownHostException
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

class ApiClient(private val http: OkHttpClient = OkHttpClient()) {
    suspend fun get(url: String, headers: Map<String, String>): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            r.body?.string() ?: throw IOException("Respuesta vacía")
        }
    }
}

class Repository(private val prefs: Prefs, private val api: ApiClient) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }

    suspend fun loadDay(day: LocalDate): Result<DayResponse> {
        val server = prefs.serverUrl.first().trim().trimEnd('/')
        val token = prefs.token.first().trim()
        val fdKey = prefs.fdKey.first().trim()
        val country = prefs.country.first().trim().ifBlank { "ES" }.uppercase()
        if (server.isBlank() && fdKey.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Configura en AJUSTES la URL de tu servidor MasFOOT o tu clave gratuita de football-data.org."
                )
            )
        }
        val cacheKey = "${if (server.isNotBlank()) "srv" else "fd"}_$day"
        return try {
            val data = if (server.isNotBlank()) fromServer(server, token, day, country) else fromFootballData(fdKey, day)
            prefs.cachePut(cacheKey, json.encodeToString(DayResponse.serializer(), data))
            Result.success(data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = prefs.cacheGet(cacheKey)?.let {
                runCatching { json.decodeFromString(DayResponse.serializer(), it) }.getOrNull()
            }
            if (cached != null) Result.success(cached.copy(stale = true)) else Result.failure(Exception(friendly(e)))
        }
    }

    private suspend fun fromServer(server: String, token: String, day: LocalDate, country: String): DayResponse {
        val body = api.get("$server/v1/matches?day=$day&country=$country", mapOf("X-API-Key" to token))
        return json.decodeFromString(DayResponse.serializer(), body)
    }

    /** Modo directo: sin backend no hay catálogo de emisiones, así que ningún partido muestra retransmisión. */
    private suspend fun fromFootballData(key: String, day: LocalDate): DayResponse {
        val url = "https://api.football-data.org/v4/matches?dateFrom=${day.minusDays(1)}&dateTo=${day.plusDays(1)}"
        val body = api.get(url, mapOf("X-Auth-Token" to key))
        val parsed = json.decodeFromString(FdResponse.serializer(), body)
        val items = parsed.matches.mapNotNull { it.toItem() }.filter { it.fixture.localDate() == day }
            .sortedBy { it.fixture.kickoffUtc }
        return DayResponse(
            source = "football-data.org (directo)",
            updatedAt = Instant.now().toString(),
            coverageNote = "Cobertura limitada a las competiciones incluidas en tu plan de football-data.org. " +
                "Sin catálogo de emisiones autorizado no se muestran retransmisiones.",
            items = items,
        )
    }

    private fun FdMatch.toItem(): FixtureWithOffers? {
        val h = homeTeam.name ?: return null
        val a = awayTeam.name ?: return null
        val st = when (status) {
            "IN_PLAY", "LIVE" -> "LIVE"
            "PAUSED" -> "PAUSED"
            "FINISHED", "AWARDED" -> "FINISHED"
            "POSTPONED", "SUSPENDED", "CANCELLED" -> "POSTPONED"
            "SCHEDULED", "TIMED" -> "SCHEDULED"
            else -> "OTHER"
        }
        val fixture = Fixture(
            dedupKey = id.toString(),
            source = "football-data.org",
            externalId = id.toString(),
            competition = Competition(competition.id.toString(), competition.name, area?.name, competition.emblem),
            home = Team(h, homeTeam.shortName, homeTeam.crest),
            away = Team(a, awayTeam.shortName, awayTeam.crest),
            kickoffUtc = utcDate,
            status = st,
            homeScore = score?.fullTime?.home,
            awayScore = score?.fullTime?.away,
            minute = minute,
            venue = venue,
        )
        return FixtureWithOffers(fixture = fixture, offers = emptyList(), hasLegalBroadcast = false, message = NO_BROADCAST)
    }

    private fun friendly(e: Exception): String = when {
        e is UnknownHostException -> "Sin conexión o servidor inaccesible."
        e.message?.contains("401") == true -> "Token o clave incorrectos (HTTP 401)."
        e.message?.contains("403") == true -> "Tu plan de la API no incluye estos datos (HTTP 403)."
        e.message?.contains("429") == true -> "Límite de llamadas de la API alcanzado (HTTP 429). Espera un minuto."
        e is IllegalArgumentException -> "La URL del servidor no es válida."
        else -> "No se pudieron cargar los partidos: ${e.message ?: e.javaClass.simpleName}"
    }
}

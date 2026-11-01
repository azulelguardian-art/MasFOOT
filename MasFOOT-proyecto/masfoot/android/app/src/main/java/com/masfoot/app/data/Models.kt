package com.masfoot.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---- Modelo propio de MasFOOT (mismo formato que devuelve el backend) ----

@Serializable
data class Team(
    val name: String,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("crest_url") val crestUrl: String? = null,
)

@Serializable
data class Competition(
    @SerialName("external_id") val externalId: String = "",
    val name: String,
    val country: String? = null,
    @SerialName("emblem_url") val emblemUrl: String? = null,
)

@Serializable
data class Fixture(
    @SerialName("dedup_key") val dedupKey: String,
    val source: String = "",
    @SerialName("external_id") val externalId: String = "",
    val competition: Competition,
    val home: Team,
    val away: Team,
    @SerialName("kickoff_utc") val kickoffUtc: String,
    val status: String,
    @SerialName("home_score") val homeScore: Int? = null,
    @SerialName("away_score") val awayScore: Int? = null,
    val minute: Int? = null,
    val venue: String? = null,
)

@Serializable
data class Offer(
    val provider: String,
    val country: String = "",
    val language: String,
    @SerialName("access_type") val accessType: String,
    @SerialName("access_condition") val accessCondition: String? = null,
    @SerialName("player_type") val playerType: String,
    @SerialName("official_url") val officialUrl: String,
    @SerialName("integration_allowed") val integrationAllowed: Boolean = false,
)

@Serializable
data class FixtureWithOffers(
    val fixture: Fixture,
    val offers: List<Offer> = emptyList(),
    @SerialName("has_legal_broadcast") val hasLegalBroadcast: Boolean = false,
    val message: String? = null,
)

@Serializable
data class DayResponse(
    val source: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("coverage_note") val coverageNote: String? = null,
    val items: List<FixtureWithOffers> = emptyList(),
    val stale: Boolean = false,
)

// ---- DTOs de football-data.org v4 (modo directo, sin backend) ----

@Serializable
data class FdResponse(val matches: List<FdMatch> = emptyList())

@Serializable
data class FdMatch(
    val id: Long,
    val utcDate: String,
    val status: String = "",
    val minute: Int? = null,
    val venue: String? = null,
    val area: FdArea? = null,
    val competition: FdComp,
    val homeTeam: FdTeam,
    val awayTeam: FdTeam,
    val score: FdScore? = null,
)

@Serializable data class FdArea(val name: String? = null)
@Serializable data class FdComp(val id: Long = 0, val name: String, val emblem: String? = null)
@Serializable data class FdTeam(val name: String? = null, val shortName: String? = null, val crest: String? = null)
@Serializable data class FdScore(val fullTime: FdFt? = null)
@Serializable data class FdFt(val home: Int? = null, val away: Int? = null)

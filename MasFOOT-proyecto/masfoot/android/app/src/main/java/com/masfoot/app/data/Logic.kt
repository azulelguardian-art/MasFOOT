package com.masfoot.app.data

import java.text.Normalizer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val LANGUAGES: Map<String, String> = linkedMapOf(
    "es" to "Español", "fr" to "Francés", "it" to "Italiano",
    "pt" to "Portugués", "ar" to "Árabe", "en" to "Inglés",
)

val LIVE_STATES = setOf("LIVE", "PAUSED")

const val NO_BROADCAST = "NO HAY RETRANSMISIÓN LEGAL DISPONIBLE."

fun zoned(utc: String): ZonedDateTime? =
    runCatching { OffsetDateTime.parse(utc).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull()

fun Fixture.localDate(): LocalDate? = zoned(kickoffUtc)?.toLocalDate()

fun Fixture.timeText(): String =
    zoned(kickoffUtc)?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--"

fun Fixture.dateTimeText(): String =
    zoned(kickoffUtc)?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: "—"

fun Fixture.isLive(): Boolean = status in LIVE_STATES

fun Fixture.statusText(): String = when (status) {
    "LIVE" -> if (minute != null) "EN DIRECTO · $minute'" else "EN DIRECTO"
    "PAUSED" -> "DESCANSO"
    "FINISHED" -> "FINALIZADO"
    "SCHEDULED" -> "PRÓXIMO"
    "POSTPONED" -> "APLAZADO"
    else -> "—"
}

fun accessLabel(accessType: String): String = when (accessType) {
    "FREE" -> "GRATIS"
    "FREE_REGISTRATION" -> "GRATIS CON REGISTRO"
    "SUBSCRIPTION" -> "SUSCRIPCIÓN"
    else -> accessType
}

fun String.plain(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

enum class BroadcastFilter(val label: String) {
    ALL("Todas las emisiones autorizadas"),
    FREE("SOLO EMISIONES GRATUITAS"),
    FREE_REG("Gratis con registro"),
    SUB("Suscripción necesaria");

    fun accepts(accessType: String): Boolean = when (this) {
        ALL -> true
        FREE -> accessType == "FREE"
        FREE_REG -> accessType == "FREE_REGISTRATION"
        SUB -> accessType == "SUBSCRIPTION"
    }
}

fun FixtureWithOffers.matchesQuery(q: String): Boolean {
    if (q.isBlank()) return true
    val needle = q.plain()
    val hay = listOfNotNull(
        fixture.home.name, fixture.home.shortName, fixture.away.name, fixture.away.shortName,
        fixture.competition.name, fixture.competition.country,
    ) + offers.map { it.provider }
    return hay.any { it.plain().contains(needle) }
}

/** Filtra sin marcar nunca como gratis una oferta de pago. */
fun filterItems(
    items: List<FixtureWithOffers>,
    filter: BroadcastFilter,
    language: String?,
    competition: String?,
    query: String,
): List<FixtureWithOffers> =
    items.mapNotNull { item ->
        if (competition != null && item.fixture.competition.name != competition) return@mapNotNull null
        if (!item.matchesQuery(query)) return@mapNotNull null
        val offers = item.offers.filter { filter.accepts(it.accessType) && (language == null || it.language == language) }
        if ((filter != BroadcastFilter.ALL || language != null) && offers.isEmpty()) return@mapNotNull null
        val restricted = filter != BroadcastFilter.ALL || language != null
        item.copy(
            offers = if (restricted) offers else item.offers,
            hasLegalBroadcast = (if (restricted) offers else item.offers).isNotEmpty(),
        )
    }.sortedBy { it.fixture.kickoffUtc }

/** Etiqueta del mejor acceso disponible para la tarjeta del partido. */
fun FixtureWithOffers.bestAccess(): String? = when {
    offers.any { it.accessType == "FREE" } -> "FREE"
    offers.any { it.accessType == "FREE_REGISTRATION" } -> "FREE_REGISTRATION"
    offers.any { it.accessType == "SUBSCRIPTION" } -> "SUBSCRIPTION"
    else -> null
}

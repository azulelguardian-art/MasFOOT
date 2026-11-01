package com.masfoot.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masfoot.app.MainViewModel
import com.masfoot.app.data.BroadcastFilter
import com.masfoot.app.data.LANGUAGES
import com.masfoot.app.data.filterItems
import com.masfoot.app.data.isLive
import com.masfoot.app.data.zoned
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, liveOnly: Boolean, onOpen: (String) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    val all = ui.data?.items.orEmpty()
    val shown = filterItems(all, ui.filter, ui.language, ui.competition, ui.query)
        .let { list -> if (liveOnly) list.filter { it.fixture.isLive() } else list }
    val live = shown.filter { it.fixture.isLive() }
    val finished = shown.filter { it.fixture.status == "FINISHED" }
    val upcoming = shown.filter { !it.fixture.isLive() && it.fixture.status != "FINISHED" }
    val languages = all.flatMap { it.offers.map { o -> o.language } }.distinct()
    val comps = all.map { it.fixture.competition.name }.distinct().sorted()

    fun isFav(item: com.masfoot.app.data.FixtureWithOffers): Boolean = "m:${item.fixture.dedupKey}" in favs

    PullToRefreshBox(isRefreshing = ui.loading, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text("MAS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                            Text(
                                "FOOT",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Text(
                            ui.day.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", Locale("es", "ES"))),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    if (ui.day != LocalDate.now()) {
                        TextButton(onClick = { vm.setDay(LocalDate.now()) }) { Text("HOY") }
                    }
                    IconButton(onClick = { showPicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendario")
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = ui.query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Buscar equipo, competición, país o canal") },
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BroadcastFilter.entries.toList()) { f ->
                        FilterChip(selected = ui.filter == f, onClick = { vm.setFilter(f) }, label = { Text(f.label) })
                    }
                }
            }
            if (languages.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(selected = ui.language == null, onClick = { vm.setLanguage(null) }, label = { Text("Todos los idiomas") })
                        }
                        items(languages) { code ->
                            FilterChip(
                                selected = ui.language == code,
                                onClick = { vm.setLanguage(code) },
                                label = { Text(LANGUAGES[code] ?: code) },
                            )
                        }
                    }
                }
            }
            if (comps.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(comps) { c ->
                            FilterChip(
                                selected = ui.competition == c,
                                onClick = { vm.setCompetition(if (ui.competition == c) null else c) },
                                label = { Text(c) },
                            )
                        }
                    }
                }
            }

            val data = ui.data
            if (data != null) {
                item {
                    val time = zoned(data.updatedAt)?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "—"
                    Text(
                        "Fuente: ${data.source} · Actualizado a las $time" +
                            if (data.stale) " · SIN CONEXIÓN: datos guardados" else "",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    data.coverageNote?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                }
            }
            if (ui.error != null && data == null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(ui.error ?: "", fontWeight = FontWeight.SemiBold)
                            Button(onClick = { vm.refresh() }) { Text("REINTENTAR") }
                        }
                    }
                }
            }
            if (data != null && shown.isEmpty()) {
                item {
                    Text(
                        if (liveOnly) "No hay partidos en directo ahora mismo." else "No hay partidos con estos filtros.",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            if (live.isNotEmpty()) {
                item { SectionTitle("EN DIRECTO (${live.size})") }
                items(live, key = { it.fixture.dedupKey }) { m ->
                    MatchCard(m, isFav(m), { vm.toggleFavorite("m:${m.fixture.dedupKey}") }, { onOpen(m.fixture.dedupKey) })
                }
            }
            if (!liveOnly) {
                if (upcoming.isNotEmpty()) {
                    item { SectionTitle("PRÓXIMOS PARTIDOS (${upcoming.size})") }
                    items(upcoming, key = { it.fixture.dedupKey }) { m ->
                        MatchCard(m, isFav(m), { vm.toggleFavorite("m:${m.fixture.dedupKey}") }, { onOpen(m.fixture.dedupKey) })
                    }
                }
                if (finished.isNotEmpty()) {
                    item { SectionTitle("FINALIZADOS (${finished.size})") }
                    items(finished, key = { it.fixture.dedupKey }) { m ->
                        MatchCard(m, isFav(m), { vm.toggleFavorite("m:${m.fixture.dedupKey}") }, { onOpen(m.fixture.dedupKey) })
                    }
                }
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = ui.day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.setDay(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("ACEPTAR") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("CANCELAR") } },
        ) { DatePicker(state = state) }
    }
}

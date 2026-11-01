package com.masfoot.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masfoot.app.MainViewModel
import com.masfoot.app.data.matchesQuery
import com.masfoot.app.data.plain
import java.time.LocalDate

@Composable
fun SearchScreen(vm: MainViewModel, onOpen: (String) -> Unit, onGoHome: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    var q by rememberSaveable { mutableStateOf("") }

    val items = ui.data?.items.orEmpty()
    val needle = q.trim().plain()
    val dateMatch = Regex("""\d{4}-\d{2}-\d{2}""").matchEntire(q.trim())

    val matches = if (needle.isBlank()) emptyList() else items.filter { it.matchesQuery(q) }.sortedBy { it.fixture.kickoffUtc }
    val teams = if (needle.isBlank()) emptyList() else
        items.flatMap { listOf(it.fixture.home.name, it.fixture.away.name) }.distinct().filter { it.plain().contains(needle) }
    val comps = if (needle.isBlank()) emptyList() else
        items.map { it.fixture.competition.name }.distinct().filter { it.plain().contains(needle) }
    val countries = if (needle.isBlank()) emptyList() else
        items.mapNotNull { it.fixture.competition.country }.distinct().filter { it.plain().contains(needle) }
    val providers = if (needle.isBlank()) emptyList() else
        items.flatMap { it.offers.map { o -> o.provider } }.distinct().filter { it.plain().contains(needle) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("BUSCAR", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
        item {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Equipo, partido, competición, país, canal o fecha") },
            )
        }
        item {
            Text(
                "La búsqueda se hace sobre los partidos del día cargado (${ui.day}). " +
                    "Para otra fecha, escribe AAAA-MM-DD.",
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (dateMatch != null) {
            item {
                Button(onClick = {
                    runCatching { LocalDate.parse(q.trim()) }.getOrNull()?.let {
                        vm.setDay(it)
                        onGoHome()
                    }
                }) { Text("IR A LA FECHA ${q.trim()}") }
            }
        }
        if (needle.isNotBlank() && matches.isEmpty() && teams.isEmpty() && comps.isEmpty() && countries.isEmpty() && providers.isEmpty() && dateMatch == null) {
            item { Text("Sin resultados.", fontWeight = FontWeight.SemiBold) }
        }
        if (matches.isNotEmpty()) {
            item { SectionTitle("PARTIDOS (${matches.size})") }
            items(matches, key = { it.fixture.dedupKey }) { m ->
                MatchCard(m, "m:${m.fixture.dedupKey}" in favs, { vm.toggleFavorite("m:${m.fixture.dedupKey}") }, { onOpen(m.fixture.dedupKey) })
            }
        }
        if (comps.isNotEmpty()) {
            item { SectionTitle("COMPETICIONES (toca para filtrar el inicio)") }
            items(comps) { c ->
                Text(
                    c,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().clickable { vm.setCompetition(c); onGoHome() }.padding(vertical = 8.dp),
                )
            }
        }
        if (teams.isNotEmpty()) {
            item { SectionTitle("EQUIPOS") }
            items(teams) { Text(it, modifier = Modifier.padding(vertical = 2.dp)) }
        }
        if (countries.isNotEmpty()) {
            item { SectionTitle("PAÍSES") }
            items(countries) { Text(it, modifier = Modifier.padding(vertical = 2.dp)) }
        }
        if (providers.isNotEmpty()) {
            item { SectionTitle("CANALES Y PROVEEDORES") }
            items(providers) { Text(it, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
}

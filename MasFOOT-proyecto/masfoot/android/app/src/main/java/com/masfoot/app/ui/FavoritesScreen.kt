package com.masfoot.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masfoot.app.MainViewModel

@Composable
fun FavoritesScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val all = ui.data?.items.orEmpty()

    val relevant = all.filter {
        val f = it.fixture
        "m:${f.dedupKey}" in favs || "t:${f.home.name}" in favs || "t:${f.away.name}" in favs ||
            "c:${f.competition.name}" in favs
    }.sortedBy { it.fixture.kickoffUtc }

    fun label(key: String): String = when {
        key.startsWith("t:") -> "Equipo · ${key.removePrefix("t:")}"
        key.startsWith("c:") -> "Competición · ${key.removePrefix("c:")}"
        key.startsWith("m:") -> {
            val k = key.removePrefix("m:")
            all.firstOrNull { it.fixture.dedupKey == k }?.let { "Partido · ${it.fixture.home.name} - ${it.fixture.away.name}" }
                ?: "Partido guardado (no está en el día cargado)"
        }
        else -> key
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("FAVORITOS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
        if (favs.isEmpty()) {
            item {
                Text(
                    "Todavía no tienes favoritos. Pulsa ★ en un partido, o abre su ficha para guardar " +
                        "equipos y competiciones.",
                )
            }
        }
        if (relevant.isNotEmpty()) {
            item { SectionTitle("TUS FAVORITOS EN EL DÍA CARGADO (${ui.day})") }
            items(relevant, key = { it.fixture.dedupKey }) { m ->
                MatchCard(m, "m:${m.fixture.dedupKey}" in favs, { vm.toggleFavorite("m:${m.fixture.dedupKey}") }, { onOpen(m.fixture.dedupKey) })
            }
        } else if (favs.isNotEmpty()) {
            item { Text("Ninguno de tus favoritos juega el día cargado.") }
        }
        if (favs.isNotEmpty()) {
            item { SectionTitle("GUARDADOS") }
            items(favs.sorted()) { key ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label(key), modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.toggleFavorite(key) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Quitar de favoritos")
                    }
                }
            }
        }
    }
}

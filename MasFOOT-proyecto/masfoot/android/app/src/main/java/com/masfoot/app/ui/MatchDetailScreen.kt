package com.masfoot.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masfoot.app.MainViewModel
import com.masfoot.app.data.LANGUAGES
import com.masfoot.app.data.NO_BROADCAST
import com.masfoot.app.data.Offer
import com.masfoot.app.data.dateTimeText
import com.masfoot.app.data.statusText

private const val SPORTIUM_URL = "https://www.sportium.es/"

/** Abre solo enlaces https (proveedor oficial). */
private fun openOfficial(ctx: Context, url: String) {
    if (!url.startsWith("https://")) {
        Toast.makeText(ctx, "Enlace no seguro, no se abre.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, "No hay aplicación para abrir este enlace.", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MatchDetailScreen(
    vm: MainViewModel,
    key: String,
    onBack: () -> Unit,
    onPlay: (url: String, mime: String, title: String) -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val item = ui.data?.items?.firstOrNull { it.fixture.dedupKey == key }
    var lang by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(item?.fixture?.competition?.name ?: "Partido", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        if (item == null) {
            Box(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
                Text("Este partido ya no está en la lista cargada. Vuelve atrás y actualiza.")
            }
            return@Scaffold
        }
        val f = item.fixture
        val title = "${f.home.name} - ${f.away.name}"
        val langs = item.offers.map { it.language }.distinct()
        val shown = item.offers.filter { lang == null || it.language == lang }

        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Crest(f.home.crestUrl, 64.dp)
                            Text(f.home.name, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            if (f.homeScore != null && f.awayScore != null) "${f.homeScore} - ${f.awayScore}" else "vs",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Crest(f.away.crestUrl, 64.dp)
                            Text(f.away.name, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(f.statusText(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text("${f.dateTimeText()} (hora local)", style = MaterialTheme.typography.bodySmall)
                    Text(
                        listOfNotNull(f.competition.name, f.competition.country).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    f.venue?.let { Text("Estadio: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }

            SectionTitle("FAVORITOS")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = "m:$key" in favs,
                    onClick = { vm.toggleFavorite("m:$key") },
                    label = { Text("Partido") },
                )
                FilterChip(
                    selected = "t:${f.home.name}" in favs,
                    onClick = { vm.toggleFavorite("t:${f.home.name}") },
                    label = { Text(f.home.shortName ?: f.home.name) },
                )
                FilterChip(
                    selected = "t:${f.away.name}" in favs,
                    onClick = { vm.toggleFavorite("t:${f.away.name}") },
                    label = { Text(f.away.shortName ?: f.away.name) },
                )
                FilterChip(
                    selected = "c:${f.competition.name}" in favs,
                    onClick = { vm.toggleFavorite("c:${f.competition.name}") },
                    label = { Text(f.competition.name) },
                )
            }

            SectionTitle("RETRANSMISIONES")
            if (item.offers.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(NO_BROADCAST, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "MasFOOT solo muestra emisiones de catálogos de proveedores autorizados. " +
                                "Ahora mismo ninguno informa de este partido.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            } else {
                Box {
                    Button(onClick = { menu = true }) {
                        Text("SELECCIONAR IDIOMA" + (lang?.let { " · ${LANGUAGES[it] ?: it}" } ?: ""))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Todos los idiomas") }, onClick = { lang = null; menu = false })
                        langs.forEach { code ->
                            DropdownMenuItem(
                                text = { Text(LANGUAGES[code] ?: code) },
                                onClick = { lang = code; menu = false },
                            )
                        }
                    }
                }
                shown.forEach { offer ->
                    OfferCard(offer) {
                        val embedded = offer.integrationAllowed &&
                            (offer.playerType == "EMBEDDED_HLS" || offer.playerType == "EMBEDDED_DASH")
                        if (embedded) {
                            onPlay(offer.officialUrl, if (offer.playerType == "EMBEDDED_DASH") "dash" else "hls", title)
                        } else {
                            openOfficial(ctx, offer.officialUrl)
                        }
                    }
                }
            }

            SectionTitle("EVENTOS, ESTADÍSTICAS Y ALINEACIONES")
            Text(
                "No disponibles con la fuente de datos actual (el plan gratuito de football-data.org no los incluye).",
                style = MaterialTheme.typography.bodySmall,
            )

            SectionTitle("CUOTAS SPORTIUM")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "No hay acceso autorizado a los datos de Sportium, por lo que MasFOOT no muestra cuotas " +
                            "(nunca se inventan ni se estiman).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { openOfficial(ctx, SPORTIUM_URL) }) { Text("APOSTAR EN SPORTIUM") }
                    Text(
                        "Se abre la web oficial. MasFOOT no permite apostar. +18 · Juega con responsabilidad.",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferCard(offer: Offer, onWatch: () -> Unit) {
    val embedded = offer.integrationAllowed &&
        (offer.playerType == "EMBEDDED_HLS" || offer.playerType == "EMBEDDED_DASH")
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(offer.provider, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AccessBadge(offer.accessType)
            }
            Text(
                "Idioma: ${LANGUAGES[offer.language] ?: offer.language}" +
                    if (offer.country.isNotBlank()) " · País: ${offer.country}" else "",
                style = MaterialTheme.typography.bodySmall,
            )
            offer.accessCondition?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = onWatch) { Text(if (embedded) "VER DIRECTO" else "ABRIR PLATAFORMA OFICIAL") }
        }
    }
}

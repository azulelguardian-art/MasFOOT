package com.masfoot.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.masfoot.app.data.FixtureWithOffers
import com.masfoot.app.data.Team
import com.masfoot.app.data.bestAccess
import com.masfoot.app.data.isLive
import com.masfoot.app.data.statusText
import com.masfoot.app.data.timeText

@Composable
fun Crest(url: String?, size: Dp = 40.dp) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier.size(size).clip(RoundedCornerShape(6.dp)),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun AccessBadge(access: String?) {
    val (text, bg) = when (access) {
        "FREE" -> "RETRANSMISIÓN GRATUITA" to Color(0xFF0B3D1E)
        "FREE_REGISTRATION" -> "GRATIS CON REGISTRO" to Color(0xFF8A5A00)
        "SUBSCRIPTION" -> "SUSCRIPCIÓN NECESARIA" to Color(0xFF333333)
        else -> "SIN RETRANSMISIÓN LEGAL" to Color(0xFF7A1F1F)
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun TeamSide(team: Team, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Crest(team.crestUrl, 40.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            team.shortName ?: team.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MatchCard(item: FixtureWithOffers, favorite: Boolean, onFav: () -> Unit, onClick: () -> Unit) {
    val f = item.fixture
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (f.isLive()) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull(f.competition.name, f.competition.country).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onFav, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (favorite) "Quitar de favoritos" else "Añadir a favoritos",
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TeamSide(f.home, Modifier.weight(1f))
                Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val hasScore = f.homeScore != null && f.awayScore != null
                    Text(
                        if (hasScore) "${f.homeScore} - ${f.awayScore}" else f.timeText(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        f.statusText(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TeamSide(f.away, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            AccessBadge(item.bestAccess())
        }
    }
}

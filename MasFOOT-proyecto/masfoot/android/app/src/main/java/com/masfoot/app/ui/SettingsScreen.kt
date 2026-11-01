package com.masfoot.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masfoot.app.MainViewModel
import kotlinx.coroutines.flow.first

@Composable
fun SettingsScreen(vm: MainViewModel, onTestPlayer: () -> Unit) {
    val dark by vm.prefs.dark.collectAsStateWithLifecycle(false)
    var server by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var fdKey by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("ES") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        server = vm.prefs.serverUrl.first()
        token = vm.prefs.token.first()
        fdKey = vm.prefs.fdKey.first()
        country = vm.prefs.country.first()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("AJUSTES", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

        Text("FUENTE DE DATOS", fontWeight = FontWeight.ExtraBold)
        Text(
            "Opción A (recomendada): URL de tu servidor MasFOOT (backend) y su token. El servidor añade el " +
                "catálogo de retransmisiones autorizadas.\n" +
                "Opción B (modo directo): tu clave gratuita de football-data.org. Muestra partidos reales, " +
                "pero sin retransmisiones.\n" +
                "Las claves se guardan solo en este dispositivo, nunca dentro del APK.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = server, onValueChange = { server = it; saved = false }, label = { Text("URL del servidor (https://…)") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = token, onValueChange = { token = it; saved = false }, label = { Text("Token del servidor (X-API-Key)") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = fdKey, onValueChange = { fdKey = it; saved = false }, label = { Text("Clave football-data.org") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = country, onValueChange = { country = it.take(2); saved = false }, label = { Text("País (2 letras, p. ej. ES)") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { vm.saveSettings(server, token, fdKey, country); saved = true }) {
            Text("GUARDAR Y ACTUALIZAR")
        }
        if (saved) Text("Guardado. Actualizando partidos…", style = MaterialTheme.typography.labelMedium)

        Text("APARIENCIA", fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Modo oscuro", modifier = Modifier.weight(1f))
            Switch(checked = dark, onCheckedChange = { vm.setDark(it) })
        }

        Text("REPRODUCTOR", fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Comprueba el reproductor integrado (Media3, HLS) con un stream público de prueba que no es un partido.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = onTestPlayer) { Text("PROBAR REPRODUCTOR") }

        Text("ACERCA DE", fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
        Text(
            "MasFOOT 0.1.0 · Código abierto (Apache 2.0).\n" +
                "Solo muestra retransmisiones de proveedores autorizados. No hay streams no oficiales.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

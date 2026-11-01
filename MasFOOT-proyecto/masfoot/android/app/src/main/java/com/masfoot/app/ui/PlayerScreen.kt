package com.masfoot.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun describe(e: PlaybackException): String = when (e.errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Sin conexión. Reintentando…"
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        "El proveedor rechazó la petición (posible geobloqueo o autenticación requerida)."
    else -> "No se pudo reproducir (${e.errorCodeName})."
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(url: String, mime: String, title: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val activity = ctx.findActivity()
    var error by remember { mutableStateOf<String?>(null) }
    var network by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var autoRetries by remember { mutableIntStateOf(0) }
    var full by remember { mutableStateOf(false) }

    val exo = remember { ExoPlayer.Builder(ctx).build() }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    exo.seekToDefaultPosition()
                    exo.prepare()
                    return
                }
                network = e.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    e.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                error = describe(e)
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    error = null
                    autoRetries = 0
                }
            }
        }
        exo.addListener(listener)
        onDispose {
            exo.removeListener(listener)
            exo.release()
        }
    }

    LaunchedEffect(url, attempt) {
        val builder = MediaItem.Builder().setUri(url)
        when (mime) {
            "hls" -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            "dash" -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
        }
        exo.setMediaItem(builder.build())
        exo.prepare()
        exo.playWhenReady = true
    }

    // Recuperación de conexión: hasta 3 reintentos automáticos si el fallo es de red.
    LaunchedEffect(error) {
        if (error != null && network && autoRetries < 3) {
            delay(4000)
            autoRetries++
            attempt++
        }
    }

    LaunchedEffect(full) {
        val a = activity ?: return@LaunchedEffect
        a.requestedOrientation =
            if (full) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val c = WindowCompat.getInsetsController(a.window, a.window.decorView)
        if (full) {
            c.hide(WindowInsetsCompat.Type.systemBars())
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            c.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.let {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(it.window, it.window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                PlayerView(c).apply {
                    this.player = exo
                    useController = true
                    setShowSubtitleButton(true)
                }
            },
        )
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().background(Color(0x99000000)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = { full = !full }) {
                Icon(
                    if (full) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Pantalla completa",
                    tint = Color.White,
                )
            }
        }
        error?.let { msg ->
            Column(
                Modifier.align(Alignment.Center).padding(24.dp).background(Color(0xCC000000)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(msg, color = Color.White, textAlign = TextAlign.Center)
                Button(onClick = { error = null; autoRetries = 0; attempt++ }) { Text("REINTENTAR") }
            }
        }
    }
}

package com.masfoot.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.masfoot.app.data.isLive
import com.masfoot.app.ui.FavoritesScreen
import com.masfoot.app.ui.HomeScreen
import com.masfoot.app.ui.MasFootTheme
import com.masfoot.app.ui.MatchDetailScreen
import com.masfoot.app.ui.PlayerScreen
import com.masfoot.app.ui.SearchScreen
import com.masfoot.app.ui.SettingsScreen

private const val TEST_STREAM = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "INICIO", Icons.Default.Home),
    Tab("live", "DIRECTO", Icons.Default.LiveTv),
    Tab("search", "BUSCAR", Icons.Default.Search),
    Tab("favorites", "FAVORITOS", Icons.Default.Favorite),
    Tab("settings", "AJUSTES", Icons.Default.Settings),
)

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dark by vm.prefs.dark.collectAsStateWithLifecycle(false)
            MasFootTheme(dark) { MasFootRoot(vm) }
        }
    }
}

@Composable
private fun MasFootRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route
    val ui by vm.ui.collectAsStateWithLifecycle()
    val liveCount = ui.data?.items?.count { it.fixture.isLive() } ?: 0

    fun goTab(r: String) = nav.navigate(r) {
        popUpTo("home") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    fun openMatch(key: String) = nav.navigate("match/$key")

    fun play(url: String, mime: String, title: String) =
        nav.navigate("player?url=${Uri.encode(url)}&mime=$mime&title=${Uri.encode(title)}")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (route in TABS.map { it.route }) {
                NavigationBar(containerColor = Color(0xFF0B3D1E)) {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { goTab(tab.route) },
                            icon = {
                                BadgedBox(badge = {
                                    if (tab.route == "live" && liveCount > 0) Badge { Text("$liveCount") }
                                }) { Icon(tab.icon, contentDescription = tab.label) }
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontWeight = if (tab.route == "live") FontWeight.ExtraBold else FontWeight.Normal,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF2EE66B),
                                selectedTextColor = Color(0xFF2EE66B),
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White,
                                indicatorColor = Color(0xFF0F6B2F),
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm, liveOnly = false, onOpen = ::openMatch) }
            composable("live") { HomeScreen(vm, liveOnly = true, onOpen = ::openMatch) }
            composable("search") { SearchScreen(vm, onOpen = ::openMatch, onGoHome = { goTab("home") }) }
            composable("favorites") { FavoritesScreen(vm, onOpen = ::openMatch) }
            composable("settings") {
                SettingsScreen(vm, onTestPlayer = { play(TEST_STREAM, "hls", "Stream de prueba") })
            }
            composable("match/{key}", arguments = listOf(navArgument("key") { type = NavType.StringType })) { e ->
                MatchDetailScreen(
                    vm,
                    key = e.arguments?.getString("key").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onPlay = ::play,
                )
            }
            composable(
                "player?url={url}&mime={mime}&title={title}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType; defaultValue = "" },
                    navArgument("mime") { type = NavType.StringType; defaultValue = "auto" },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { e ->
                PlayerScreen(
                    url = e.arguments?.getString("url").orEmpty(),
                    mime = e.arguments?.getString("mime").orEmpty(),
                    title = e.arguments?.getString("title").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

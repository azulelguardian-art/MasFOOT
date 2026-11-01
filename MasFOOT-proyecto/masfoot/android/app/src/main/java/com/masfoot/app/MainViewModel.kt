package com.masfoot.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.masfoot.app.data.ApiClient
import com.masfoot.app.data.BroadcastFilter
import com.masfoot.app.data.DayResponse
import com.masfoot.app.data.FixtureWithOffers
import com.masfoot.app.data.Prefs
import com.masfoot.app.data.Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UiState(
    val day: LocalDate = LocalDate.now(),
    val loading: Boolean = false,
    val data: DayResponse? = null,
    val error: String? = null,
    val query: String = "",
    val filter: BroadcastFilter = BroadcastFilter.ALL,
    val language: String? = null,
    val competition: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    private val repo = Repository(prefs, ApiClient())

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    val favorites: StateFlow<Set<String>> =
        prefs.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private var job: Job? = null

    init {
        refresh()
        // Actualización automática cada minuto mientras se ve el día de hoy.
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (_ui.value.day == LocalDate.now()) refresh(silent = true)
            }
        }
    }

    fun refresh(silent: Boolean = false) {
        job?.cancel()
        job = viewModelScope.launch {
            if (!silent) _ui.update { it.copy(loading = true, error = null) }
            val result = repo.loadDay(_ui.value.day)
            _ui.update { s ->
                result.fold(
                    onSuccess = { s.copy(loading = false, data = it, error = null) },
                    onFailure = { s.copy(loading = false, data = null, error = it.message ?: "Error desconocido") },
                )
            }
        }
    }

    fun setDay(day: LocalDate) {
        _ui.update { it.copy(day = day, data = null, competition = null, language = null) }
        refresh()
    }

    fun setQuery(q: String) = _ui.update { it.copy(query = q) }
    fun setFilter(f: BroadcastFilter) = _ui.update { it.copy(filter = f) }
    fun setLanguage(l: String?) = _ui.update { it.copy(language = l) }
    fun setCompetition(c: String?) = _ui.update { it.copy(competition = c) }

    fun toggleFavorite(key: String) {
        viewModelScope.launch { prefs.toggleFavorite(key) }
    }

    fun saveSettings(server: String, token: String, fdKey: String, country: String) {
        viewModelScope.launch {
            prefs.saveConnection(server, token, fdKey, country)
            refresh()
        }
    }

    fun setDark(v: Boolean) {
        viewModelScope.launch { prefs.setDark(v) }
    }

    fun find(key: String): FixtureWithOffers? =
        _ui.value.data?.items?.firstOrNull { it.fixture.dedupKey == key }
}

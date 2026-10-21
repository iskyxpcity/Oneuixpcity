package com.alohaoneui.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.alohaoneui.app.data.repository.SettingsRepository
import com.alohaoneui.app.data.repository.SettingsRepositoryImpl
import com.alohaoneui.app.alohaOneUIApp
import com.alohaoneui.app.ui.screen.home.HomeUiState
import com.alohaoneui.app.ui.screen.home.getManagerVersion
import com.alohaoneui.app.ui.util.checkNewVersion
import com.alohaoneui.app.ui.util.update.LatestVersionInfo

/** AlohaOneUI homepage state and self-update data. */
class HomeViewModel(
    private val settingsRepo: SettingsRepository = SettingsRepositoryImpl(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val baseState = withContext(Dispatchers.IO) { buildState() }
            _uiState.update { baseState }
            if (baseState.checkUpdateEnabled) {
                val version = withContext(Dispatchers.IO) { checkNewVersion() }
                _uiState.update { it.copy(latestVersionInfo = version) }
            }
        }
    }

    private fun buildState(): HomeUiState {
        val managerVersion = getManagerVersion(alohaOneUIApp)
        return HomeUiState(
            checkUpdateEnabled = settingsRepo.checkUpdate,
            latestVersionInfo = LatestVersionInfo(),
            currentManagerVersionName = managerVersion.versionName,
        )
    }
}

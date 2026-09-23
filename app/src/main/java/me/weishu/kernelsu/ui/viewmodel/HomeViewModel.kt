package me.weishu.kernelsu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.data.repository.SettingsRepository
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.screen.home.HomeUiState
import me.weishu.kernelsu.ui.screen.home.getManagerVersion
import me.weishu.kernelsu.ui.util.checkNewVersion
import me.weishu.kernelsu.ui.util.module.LatestVersionInfo

/** Pure OneuiXP homepage state. No kernel, su or SELinux backend. */
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
        val managerVersion = getManagerVersion(ksuApp)
        return HomeUiState(
            checkUpdateEnabled = settingsRepo.checkUpdate,
            latestVersionInfo = LatestVersionInfo(),
            currentManagerVersionCode = managerVersion.versionCode,
        )
    }
}

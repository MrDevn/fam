package com.fam.aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fam.aware.R
import com.fam.aware.data.local.SettingsRepository
import com.fam.aware.data.model.AppPermission
import com.fam.aware.data.model.PermissionReport
import com.fam.aware.data.model.SupervisionAnalyzer
import com.fam.aware.data.model.SupervisionReport
import com.fam.aware.data.model.UiState
import com.fam.aware.data.repository.PermissionRepository
import com.fam.aware.data.repository.SupervisionRepository
import com.fam.aware.util.describe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Количество шагов первичной настройки. */
const val ONBOARDING_PAGE_COUNT = 3

/**
 * Состояние экрана первичной настройки.
 *
 * @param page текущая страница мастера.
 * @param supervision результат диагностики родительского контроля.
 * @param permission фактическое состояние единственного runtime-разрешения.
 */
data class OnboardingUiState(
    val page: Int = 0,
    val pageCount: Int = ONBOARDING_PAGE_COUNT,
    val supervision: UiState<SupervisionReport> = UiState.Loading,
    val permission: PermissionReport? = null,
) {
    val isFirstPage: Boolean get() = page == 0
    val isLastPage: Boolean get() = page == pageCount - 1
}

/**
 * ViewModel мастера первичной настройки.
 *
 * Отвечает только за состояние экрана: вся работа с системой — в репозиториях.
 */
class OnboardingViewModel(
    private val supervisionRepository: SupervisionRepository,
    private val permissionRepository: PermissionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _completed = MutableStateFlow(settingsRepository.onboardingCompleted)
    val onboardingCompleted: StateFlow<Boolean> = _completed.asStateFlow()

    /**
     * Значение `shouldShowRequestPermissionRationale` после последнего ответа пользователя.
     * Хранится здесь, потому что ViewModel не имеет доступа к Activity.
     */
    private var lastRationaleVisible: Boolean = false

    init {
        refresh()
    }

    /** Запускает диагностику устройства. Вызывается только из UI, никогда не по таймеру. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(supervision = UiState.Loading) }
            val outcome = runCatching { supervisionRepository.collect() }
            val permission = currentPermissionReport()
            outcome.fold(
                onSuccess = { raw ->
                    _uiState.update {
                        it.copy(
                            supervision = UiState.Success(SupervisionAnalyzer.analyze(raw)),
                            permission = permission,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            supervision = UiState.Error(R.string.state_error_title, error.describe()),
                            permission = permission,
                        )
                    }
                },
            )
        }
    }

    fun setPage(page: Int) {
        _uiState.update { it.copy(page = page.coerceIn(0, it.pageCount - 1)) }
    }

    fun nextPage() {
        _uiState.update { state ->
            if (state.isLastPage) state else state.copy(page = state.page + 1)
        }
    }

    fun previousPage() {
        _uiState.update { state ->
            if (state.isFirstPage) state else state.copy(page = state.page - 1)
        }
    }

    /**
     * Вызывается непосредственно перед показом системного диалога.
     *
     * Факт запроса запоминается, чтобы приложение могло отличить «ещё не спрашивали»
     * от «отклонено навсегда» и больше не дёргать пользователя автоматически.
     */
    fun onPermissionRequestStarted() {
        settingsRepository.markPermissionRequested(AppPermission.NOTIFICATIONS)
        publishPermission()
    }

    /** Вызывается после ответа пользователя на системный диалог. */
    fun onPermissionResult(shouldShowRationale: Boolean) {
        lastRationaleVisible = shouldShowRationale
        publishPermission()
    }

    /** Пользователь вернулся из системных настроек — перечитываем состояние. */
    fun onResumed() {
        publishPermission()
    }

    fun complete() {
        settingsRepository.setOnboardingCompleted()
        _completed.value = true
    }

    private fun publishPermission() {
        _uiState.update { it.copy(permission = currentPermissionReport()) }
    }

    private fun currentPermissionReport(): PermissionReport =
        permissionRepository.report(AppPermission.NOTIFICATIONS, lastRationaleVisible)
}

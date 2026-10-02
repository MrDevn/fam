package com.fam.aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fam.aware.R
import com.fam.aware.data.local.SettingsRepository
import com.fam.aware.data.model.AppPermission
import com.fam.aware.data.model.DeviceSnapshot
import com.fam.aware.data.model.PermissionReport
import com.fam.aware.data.model.SupervisionAnalyzer
import com.fam.aware.data.model.UiState
import com.fam.aware.data.notification.NotificationPublisher
import com.fam.aware.data.repository.PermissionRepository
import com.fam.aware.data.repository.SupervisionRepository
import com.fam.aware.util.describe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Состояние главного экрана.
 *
 * @param check состояние диагностики устройства (Loading / Success / Error / Permission denied).
 * @param delivery состояние отправки тестового уведомления.
 */
data class HomeUiState(
    val check: UiState<DeviceSnapshot> = UiState.Loading,
    val delivery: UiState<Unit> = UiState.Idle,
) {
    val snapshot: DeviceSnapshot? get() = (check as? UiState.Success)?.data
    val permission: PermissionReport? get() = snapshot?.permission
}

/** ViewModel главного экрана. */
class HomeViewModel(
    private val supervisionRepository: SupervisionRepository,
    private val permissionRepository: PermissionRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationPublisher: NotificationPublisher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastRationaleVisible: Boolean = false

    init {
        runCheck()
    }

    /** Полная проверка устройства. Запускается по инициативе пользователя или при старте экрана. */
    fun runCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(check = UiState.Loading) }
            val outcome = runCatching { supervisionRepository.collect() }
            outcome.fold(
                onSuccess = { raw ->
                    val report = SupervisionAnalyzer.analyze(raw)
                    val permission = currentPermissionReport()
                    val snapshot = DeviceSnapshot(
                        supervision = report,
                        permission = permission,
                        checkedAtMillis = System.currentTimeMillis(),
                    )
                    _uiState.update { it.copy(check = UiState.Success(snapshot)) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(check = UiState.Error(R.string.state_error_title, error.describe()))
                    }
                },
            )
        }
    }

    /**
     * Отправляет тестовое уведомление.
     *
     * Если разрешения нет, операция завершается состоянием [UiState.PermissionDenied]
     * и приложение НЕ пытается запросить разрешение само — пользователю
     * показываются системные настройки или инструкция для родителя.
     */
    fun sendTestNotification() {
        viewModelScope.launch {
            _uiState.update { it.copy(delivery = UiState.Loading) }

            val report = currentPermissionReport()
            if (!report.isUsable) {
                _uiState.update { it.copy(delivery = UiState.PermissionDenied(report)) }
                return@launch
            }

            val outcome = withContext(Dispatchers.Default) {
                notificationPublisher.publishTestNotification()
            }
            outcome.fold(
                onSuccess = {
                    _uiState.update { it.copy(delivery = UiState.Success(Unit)) }
                },
                onFailure = { error ->
                    val denied = error is SecurityException
                    _uiState.update {
                        it.copy(
                            delivery = if (denied) {
                                UiState.PermissionDenied(currentPermissionReport())
                            } else {
                                UiState.Error(R.string.state_error_title, error.describe())
                            },
                        )
                    }
                },
            )
        }
    }

    /** Фиксирует факт запроса разрешения перед показом системного диалога. */
    fun onPermissionRequestStarted() {
        settingsRepository.markPermissionRequested(AppPermission.NOTIFICATIONS)
        refreshPermissionOnly()
    }

    fun onPermissionResult(shouldShowRationale: Boolean) {
        lastRationaleVisible = shouldShowRationale
        refreshPermissionOnly()
    }

    /** Пользователь вернулся из системных настроек. */
    fun onResumed() {
        refreshPermissionOnly()
    }

    private fun refreshPermissionOnly() {
        val permission = currentPermissionReport()
        _uiState.update { state ->
            val snapshot = state.snapshot
            if (snapshot == null) {
                state.copy(delivery = UiState.Idle)
            } else {
                state.copy(
                    check = UiState.Success(snapshot.copy(permission = permission)),
                    delivery = if (state.delivery is UiState.PermissionDenied) UiState.Idle else state.delivery,
                )
            }
        }
    }

    private fun currentPermissionReport(): PermissionReport =
        permissionRepository.report(AppPermission.NOTIFICATIONS, lastRationaleVisible)
}

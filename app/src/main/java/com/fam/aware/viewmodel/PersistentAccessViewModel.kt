package com.fam.aware.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fam.aware.R
import com.fam.aware.data.model.AccessDuration
import com.fam.aware.data.model.PersistentAccessStatus
import com.fam.aware.data.model.UiState
import com.fam.aware.data.repository.PersistentAccessRepository
import com.fam.aware.util.describe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Состояние экрана режима постоянного доступа.
 *
 * @param status результат перечитывания всех источников решения.
 * @param durations доступные сроки локального подтверждения.
 * @param selectedDuration выбранный срок.
 * @param localConfirmDialogVisible показан ли диалог явного подтверждения.
 * @param notice одноразовое сообщение для пользователя (ошибка или отказ).
 */
data class PersistentAccessUiState(
    val status: UiState<PersistentAccessStatus> = UiState.Loading,
    val durations: List<AccessDuration> = emptyList(),
    val selectedDuration: AccessDuration = AccessDuration.TWELVE_HOURS,
    val localConfirmDialogVisible: Boolean = false,
    val notice: Int? = null,
)

/**
 * ViewModel режима постоянного доступа.
 *
 * Никакой самодеятельности: режим включается только после решения
 * администратора устройства или явного подтверждения, и всегда может быть
 * отключён — пользователем, по истечении срока или родителем.
 */
class PersistentAccessViewModel(
    private val persistentAccessRepository: PersistentAccessRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PersistentAccessUiState(durations = persistentAccessRepository.durations),
    )
    val uiState: StateFlow<PersistentAccessUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Перечитывает ограничение администратора, локальное подтверждение и срок. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = UiState.Loading) }
            val outcome = runCatching { persistentAccessRepository.status() }
            _uiState.update { state ->
                outcome.fold(
                    onSuccess = { status -> state.copy(status = UiState.Success(status)) },
                    onFailure = { error ->
                        state.copy(
                            status = UiState.Error(R.string.state_error_title, error.describe()),
                        )
                    },
                )
            }
        }
    }

    fun selectDuration(duration: AccessDuration) {
        _uiState.update { it.copy(selectedDuration = duration) }
    }

    /**
     * Создаёт Intent официального диалога согласования с родителем.
     *
     * @return `null`, если в системе нет провайдера ограничений — тогда
     *   запрашивать подтверждение не у кого, и UI показывает инструкцию.
     */
    fun createParentApprovalIntent(): Intent? = persistentAccessRepository.createParentApprovalIntent()

    fun markApprovalRequested() {
        persistentAccessRepository.markApprovalRequested()
        refresh()
    }

    fun cancelApprovalRequest() {
        persistentAccessRepository.cancelApprovalRequest()
        refresh()
    }

    /** Обрабатывает ответ родителя из системного диалога. */
    fun onApprovalResult(approved: Boolean?) {
        val duration = _uiState.value.selectedDuration
        when (approved) {
            true -> persistentAccessRepository.onOfficialApprovalResult(true, duration)
            false -> {
                persistentAccessRepository.onOfficialApprovalResult(false, duration)
                _uiState.update { it.copy(notice = R.string.pa_notice_parent_declined) }
            }

            null -> _uiState.update { it.copy(notice = R.string.pa_notice_no_response) }
        }
        refresh()
    }

    fun showLocalConfirmDialog() {
        _uiState.update { it.copy(localConfirmDialogVisible = true) }
    }

    fun dismissLocalConfirmDialog() {
        _uiState.update { it.copy(localConfirmDialogVisible = false) }
    }

    /**
     * Включает режим по локальному подтверждению.
     *
     * Подтверждение всегда ограничено по сроку и не даёт приложению никаких
     * системных привилегий.
     */
    fun confirmLocally() {
        persistentAccessRepository.confirmLocally(_uiState.value.selectedDuration)
        _uiState.update { it.copy(localConfirmDialogVisible = false) }
        refresh()
    }

    /** Выключает режим и снимает постоянную отметку. */
    fun revoke() {
        persistentAccessRepository.revoke()
        _uiState.update { it.copy(notice = R.string.pa_notice_revoked) }
        refresh()
    }

    fun consumeNotice() {
        _uiState.update { it.copy(notice = null) }
    }
}

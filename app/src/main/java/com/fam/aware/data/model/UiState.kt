package com.fam.aware.data.model

import androidx.annotation.StringRes

/**
 * Единый контракт состояний экрана.
 *
 * Четыре состояния из требований (Loading / Success / Error / Permission denied)
 * описаны одним sealed-интерфейсом, поэтому UI обязан обработать каждое из них.
 * [Idle] добавлен для действий, которые пользователь ещё не запускал.
 */
sealed interface UiState<out T> {

    /** Действие ещё не запускалось. */
    data object Idle : UiState<Nothing>

    /** Идёт асинхронная работа. */
    data object Loading : UiState<Nothing>

    /** Данные получены. */
    data class Success<T>(val data: T) : UiState<T>

    /**
     * Операция завершилась ошибкой.
     *
     * @param messageRes ресурс заголовка ошибки.
     * @param detail техническое описание причины (текст исключения), может отсутствовать.
     */
    data class Error(
        @StringRes val messageRes: Int,
        val detail: String? = null,
    ) : UiState<Nothing>

    /**
     * Операция не выполнена, потому что требуемое разрешение отсутствует,
     * отклонено пользователем или заблокировано политикой устройства.
     */
    data class PermissionDenied(val report: PermissionReport) : UiState<Nothing>

    val isLoading: Boolean get() = this is Loading
}

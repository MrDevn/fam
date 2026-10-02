package com.fam.aware.data.model

/**
 * Состояние runtime-разрешения с точки зрения приложения.
 *
 * Важно для устройств с родительским контролем: состояния [BLOCKED_BY_POLICY]
 * и [DENIED_PERMANENTLY] никогда не приводят к автоматическому повторному запросу.
 */
enum class PermissionState {
    /** Разрешение выдано системой. */
    GRANTED,

    /** Приложение ещё ни разу не показывало системный диалог. */
    NOT_REQUESTED,

    /** Пользователь отклонил запрос, диалог можно показать ещё раз. */
    DENIED,

    /** Пользователь отклонил запрос с выбором «Больше не спрашивать». */
    DENIED_PERMANENTLY,

    /**
     * Разрешение отозвано политикой устройства: родительский контроль,
     * Device Owner или Profile Owner. Определяется официальным API
     * `PackageManager.isPermissionRevokedByPolicy`.
     */
    BLOCKED_BY_POLICY,

    /**
     * На этой версии Android runtime-запрос не применяется
     * (например, POST_NOTIFICATIONS ниже API 33 выдаётся при установке).
     */
    NOT_APPLICABLE,
    ;

    /** Запрашивать разрешение повторно запрещено — нужен внешний интерфейс (настройки/родитель). */
    val reRequestForbidden: Boolean
        get() = this == BLOCKED_BY_POLICY || this == DENIED_PERMANENTLY
}

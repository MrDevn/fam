package com.fam.aware.data.model

/**
 * Полный снимок состояния одного разрешения.
 *
 * @param permission разрешение, к которому относится снимок.
 * @param state вычисленное состояние.
 * @param featureEnabledInSystem разрешение выдано, но системный переключатель
 *   функции выключен (для уведомлений — `NotificationManager.areNotificationsEnabled`).
 * @param revokedByPolicy разрешение отозвано политикой устройства.
 * @param wasRequested приложение уже показывало системный диалог хотя бы один раз.
 * @param rationaleVisible `shouldShowRequestPermissionRationale` на момент проверки.
 */
data class PermissionReport(
    val permission: AppPermission,
    val state: PermissionState,
    val featureEnabledInSystem: Boolean,
    val revokedByPolicy: Boolean,
    val wasRequested: Boolean,
    val rationaleVisible: Boolean,
) {
    /** Функция реально доступна пользователю. */
    val isUsable: Boolean
        get() = (state == PermissionState.GRANTED || state == PermissionState.NOT_APPLICABLE) &&
            featureEnabledInSystem

    /** Функция недоступна именно из-за политики устройства, а не из-за выбора пользователя. */
    val isBlockedBySupervision: Boolean
        get() = revokedByPolicy || state == PermissionState.BLOCKED_BY_POLICY

    /** Пользователь сам отклонил разрешение (без участия политики). */
    val isDeniedByUser: Boolean
        get() = !isBlockedBySupervision &&
            (state == PermissionState.DENIED || state == PermissionState.DENIED_PERMANENTLY)
}

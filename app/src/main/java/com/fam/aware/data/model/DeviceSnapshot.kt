package com.fam.aware.data.model

/**
 * Результат одной полной проверки устройства.
 *
 * @param supervision отчёт о родительском контроле и ограничениях.
 * @param permission состояние единственного runtime-разрешения приложения.
 * @param checkedAtMillis момент завершения проверки (для подписи «Проверено: …»).
 */
data class DeviceSnapshot(
    val supervision: SupervisionReport,
    val permission: PermissionReport,
    val checkedAtMillis: Long,
)

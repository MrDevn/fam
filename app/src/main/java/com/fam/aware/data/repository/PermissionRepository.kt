package com.fam.aware.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fam.aware.data.local.SettingsRepository
import com.fam.aware.data.model.AppPermission
import com.fam.aware.data.model.PermissionReport
import com.fam.aware.data.model.PermissionState

/**
 * Читает фактическое состояние разрешений.
 *
 * Репозиторий ничего не запрашивает и не показывает диалогов: запрос разрешения —
 * это всегда явное действие пользователя в UI-слое.
 */
interface PermissionRepository {

    /**
     * Снимок состояния разрешения.
     *
     * @param permission проверяемое разрешение.
     * @param rationaleVisible значение `shouldShowRequestPermissionRationale`
     *   из Activity; `false`, если Activity недоступна.
     */
    fun report(permission: AppPermission, rationaleVisible: Boolean): PermissionReport

    /**
     * Отозвано ли разрешение политикой устройства (родительский контроль, DPM).
     * Официальный механизм: `PackageManager.isPermissionRevokedByPolicy`.
     */
    fun isRevokedByPolicy(permission: AppPermission): Boolean
}

class DefaultPermissionRepository(
    private val context: Context,
    private val settings: SettingsRepository,
) : PermissionRepository {

    override fun report(permission: AppPermission, rationaleVisible: Boolean): PermissionReport {
        val revokedByPolicy = isRevokedByPolicy(permission)
        val featureEnabled = isFeatureEnabledInSystem(permission)
        val wasRequested = settings.wasPermissionRequested(permission)
        val runtimeApplicable = Build.VERSION.SDK_INT >= permission.runtimeSinceApi

        val state = when {
            revokedByPolicy -> PermissionState.BLOCKED_BY_POLICY

            !runtimeApplicable -> {
                // На старых версиях разрешение выдаётся при установке, диалога нет.
                if (featureEnabled) PermissionState.NOT_APPLICABLE else PermissionState.DENIED_PERMANENTLY
            }

            ContextCompat.checkSelfPermission(context, permission.manifestName) ==
                PackageManager.PERMISSION_GRANTED -> PermissionState.GRANTED

            !wasRequested -> PermissionState.NOT_REQUESTED

            rationaleVisible -> PermissionState.DENIED

            else -> PermissionState.DENIED_PERMANENTLY
        }

        return PermissionReport(
            permission = permission,
            state = state,
            featureEnabledInSystem = featureEnabled,
            revokedByPolicy = revokedByPolicy,
            wasRequested = wasRequested,
            rationaleVisible = rationaleVisible,
        )
    }

    override fun isRevokedByPolicy(permission: AppPermission): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            runCatching {
                context.packageManager.isPermissionRevokedByPolicy(permission.manifestName, context.packageName)
            }.getOrDefault(false)

    /**
     * Даже при выданном разрешении системный переключатель может быть выключен
     * (пользователем или политикой), поэтому проверяем его отдельно.
     */
    private fun isFeatureEnabledInSystem(permission: AppPermission): Boolean = when (permission) {
        AppPermission.NOTIFICATIONS -> runCatching {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }.getOrDefault(false)
    }
}

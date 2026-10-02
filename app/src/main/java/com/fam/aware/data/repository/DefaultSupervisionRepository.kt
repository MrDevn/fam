package com.fam.aware.data.repository

import android.content.Context
import android.content.RestrictionsManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserManager
import androidx.annotation.RequiresApi
import com.fam.aware.data.model.RawSupervisionData
import com.fam.aware.data.model.UserRestrictionKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Реализация на официальных API Android.
 *
 * Каждое обращение к системе обёрнуто в [runCatching]: на устройствах с жёстким
 * родительским контролем часть вызовов может быть запрещена, и это не должно
 * приводить к падению экрана — вместо этого факт просто не попадает в отчёт.
 */
class DefaultSupervisionRepository(
    private val context: Context,
) : SupervisionRepository {

    private val userManager: UserManager? = context.getSystemService(UserManager::class.java)
    private val restrictionsManager: RestrictionsManager? =
        context.getSystemService(RestrictionsManager::class.java)
    private val packageManager: PackageManager = context.packageManager

    override val inspectedUserRestrictions: List<UserRestrictionKey> = UserRestrictionKey.entries.toList()

    override suspend fun collect(): RawSupervisionData = withContext(Dispatchers.Default) {
        RawSupervisionData(
            managedProfile = isManagedProfile(),
            privateProfile = isPrivateProfile(),
            secondaryUser = isSecondaryUser(),
            demoUser = isDemoUser(),
            quietMode = isQuietMode(),
            restrictionsProviderPresent = hasRestrictionsProvider(),
            appDisabledByAdmin = isApplicationDisabledByPolicy(),
            activeUserRestrictions = collectActiveUserRestrictions(),
            appRestrictions = collectAppRestrictions(),
            installingPackage = resolveInstallingPackage(),
        )
    }

    private fun isManagedProfile(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && runCatching { userManager?.isManagedProfile == true }
            .getOrDefault(false)

    private fun isPrivateProfile(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            runCatching { userManager?.let { it.isProfile && !it.isManagedProfile } == true }
                .getOrDefault(false)

    private fun isSecondaryUser(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && runCatching { userManager?.isSystemUser == false }
            .getOrDefault(false)

    private fun isDemoUser(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && runCatching { userManager?.isDemoUser == true }
            .getOrDefault(false)

    private fun isQuietMode(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            runCatching { userManager?.isQuietModeEnabled(Process.myUserHandle()) == true }
                .getOrDefault(false)

    private fun hasRestrictionsProvider(): Boolean =
        runCatching { restrictionsManager?.hasRestrictionsProvider() == true }.getOrDefault(false)

    private fun isApplicationDisabledByPolicy(): Boolean = runCatching {
        packageManager.getApplicationEnabledSetting(context.packageName) !=
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    }.getOrDefault(false)

    private fun collectActiveUserRestrictions(): List<UserRestrictionKey> = inspectedUserRestrictions.filter {
        runCatching { userManager?.hasUserRestriction(it.key) == true }.getOrDefault(false)
    }

    private fun collectAppRestrictions(): Map<String, String> = runCatching {
        restrictionsManager?.applicationRestrictions?.toStringMap().orEmpty()
    }.getOrDefault(emptyMap())

    private fun resolveInstallingPackage(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { installingPackageApi30() }.getOrNull()
        } else {
            runCatching { packageManager.getInstallerPackageName(context.packageName) }.getOrNull()
        }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun installingPackageApi30(): String? =
        packageManager.getInstallSourceInfo(context.packageName).installingPackageName

    private fun Bundle.toStringMap(): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        for (key in keySet()) {
            val value: Any? = runCatching { get(key) }.getOrNull()
            if (value != null) result[key] = value.toString()
        }
        return result
    }
}

package com.fam.aware.data.model

import androidx.annotation.StringRes
import com.fam.aware.R

/**
 * Ключи `UserManager.DISALLOW_*`, которые приложение проверяет.
 *
 * Проверяются только публичные константы через `UserManager.hasUserRestriction`,
 * никаких привилегий для этого не требуется.
 */
enum class UserRestrictionKey(
    val key: String,
    @StringRes val labelRes: Int,
) {
    INSTALL_APPS("android.os.UserManager.DISALLOW_INSTALL_APPS", R.string.restriction_disallow_install_apps),
    UNINSTALL_APPS("android.os.UserManager.DISALLOW_UNINSTALL_APPS", R.string.restriction_disallow_uninstall_apps),
    APPS_CONTROL("android.os.UserManager.DISALLOW_APPS_CONTROL", R.string.restriction_disallow_apps_control),
    FUN("android.os.UserManager.DISALLOW_FUN", R.string.restriction_disallow_fun),
    ADJUST_VOLUME("android.os.UserManager.DISALLOW_ADJUST_VOLUME", R.string.restriction_disallow_adjust_volume),
    CONTENT_SUGGESTIONS(
        "android.os.UserManager.DISALLOW_CONTENT_SUGGESTIONS",
        R.string.restriction_disallow_content_suggestions,
    ),
    SHARE_INTO_MANAGED_PROFILE(
        "android.os.UserManager.DISALLOW_SHARE_INTO_MANAGED_PROFILE",
        R.string.restriction_disallow_share_into_managed_profile,
    ),
}

/** Характер находки: реальное ограничение или просто справочная информация. */
enum class SignalKind { RESTRICTION, INFO }

/**
 * Одна запись диагностики.
 *
 * @param id стабильный идентификатор (используется как `key` у Compose и в тестах).
 * @param titleRes заголовок записи.
 * @param detailRes ресурс пояснения; аргументом служит [detailArg].
 * @param detailArg аргумент для [detailRes], если строка содержит `%1$s`.
 * @param detail свободный текст без форматирования (например, ключ ограничения).
 */
data class SupervisionSignal(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val detailRes: Int? = null,
    val detailArg: String? = null,
    val detail: String? = null,
    val kind: SignalKind = SignalKind.RESTRICTION,
)

/**
 * Сырые данные, собранные с устройства официальными API.
 *
 * Класс намеренно не зависит от Android, поэтому преобразование в отчёт
 * можно проверять обычными JVM-тестами.
 */
data class RawSupervisionData(
    val managedProfile: Boolean,
    val privateProfile: Boolean,
    val secondaryUser: Boolean,
    val demoUser: Boolean,
    val quietMode: Boolean,
    val restrictionsProviderPresent: Boolean,
    val appDisabledByAdmin: Boolean,
    val activeUserRestrictions: List<UserRestrictionKey>,
    val appRestrictions: Map<String, String>,
    val installingPackage: String?,
) {
    companion object {
        val EMPTY = RawSupervisionData(
            managedProfile = false,
            privateProfile = false,
            secondaryUser = false,
            demoUser = false,
            quietMode = false,
            restrictionsProviderPresent = false,
            appDisabledByAdmin = false,
            activeUserRestrictions = emptyList(),
            appRestrictions = emptyMap(),
            installingPackage = null,
        )
    }
}

/** Итоговый отчёт, который отображается в UI. */
data class SupervisionReport(
    val signals: List<SupervisionSignal>,
    val isSupervised: Boolean,
) {
    val restrictions: List<SupervisionSignal>
        get() = signals.filter { it.kind == SignalKind.RESTRICTION }

    val info: List<SupervisionSignal>
        get() = signals.filter { it.kind == SignalKind.INFO }

    companion object {
        val CLEAN = SupervisionReport(signals = emptyList(), isSupervised = false)
    }
}

/**
 * Чистая функция превращения сырых данных в отчёт.
 *
 * Логика вынесена отдельно от Android-зависимого кода, чтобы её можно было
 * покрыть модульными тестами без эмулятора.
 */
object SupervisionAnalyzer {

    fun analyze(raw: RawSupervisionData): SupervisionReport {
        val signals = buildList {
            if (raw.appDisabledByAdmin) {
                add(
                    SupervisionSignal(
                        id = "app_disabled_by_admin",
                        titleRes = R.string.supervision_signal_app_disabled,
                        detailRes = R.string.supervision_signal_app_disabled_hint,
                    ),
                )
            }
            if (raw.restrictionsProviderPresent) {
                add(
                    SupervisionSignal(
                        id = "restrictions_provider",
                        titleRes = R.string.supervision_signal_restrictions_provider,
                        detailRes = R.string.supervision_signal_restrictions_provider_hint,
                    ),
                )
            }
            if (raw.managedProfile) {
                add(
                    SupervisionSignal(
                        id = "managed_profile",
                        titleRes = R.string.supervision_signal_managed_profile,
                        detailRes = R.string.supervision_signal_managed_profile_hint,
                    ),
                )
            }
            if (raw.privateProfile) {
                add(
                    SupervisionSignal(
                        id = "private_profile",
                        titleRes = R.string.supervision_signal_private_profile,
                        detailRes = R.string.supervision_signal_private_profile_hint,
                    ),
                )
            }
            if (raw.quietMode) {
                add(
                    SupervisionSignal(
                        id = "quiet_mode",
                        titleRes = R.string.supervision_signal_quiet_mode,
                        detailRes = R.string.supervision_signal_quiet_mode_hint,
                    ),
                )
            }
            if (raw.demoUser) {
                add(
                    SupervisionSignal(
                        id = "demo_user",
                        titleRes = R.string.supervision_signal_demo_user,
                        detailRes = R.string.supervision_signal_demo_user_hint,
                    ),
                )
            }
            raw.activeUserRestrictions.forEach { restriction ->
                add(
                    SupervisionSignal(
                        id = "user_restriction_${restriction.name.lowercase()}",
                        titleRes = restriction.labelRes,
                        detail = restriction.key,
                    ),
                )
            }
            if (raw.appRestrictions.isNotEmpty()) {
                add(
                    SupervisionSignal(
                        id = "app_restrictions",
                        titleRes = R.string.supervision_signal_app_restrictions,
                        detailRes = R.string.supervision_signal_app_restrictions_hint,
                        detailArg = raw.appRestrictions.entries
                            .sortedBy { it.key }
                            .joinToString(separator = ", ") { (key, value) -> "$key = $value" },
                    ),
                )
            }
            if (raw.secondaryUser) {
                add(
                    SupervisionSignal(
                        id = "secondary_user",
                        titleRes = R.string.supervision_signal_secondary_user,
                        detailRes = R.string.supervision_signal_secondary_user_hint,
                        kind = SignalKind.INFO,
                    ),
                )
            }
            val installer = raw.installingPackage?.takeIf { it.isNotBlank() }
            add(
                SupervisionSignal(
                    id = "installed_by",
                    titleRes = R.string.supervision_signal_installed_by,
                    detailRes = if (installer != null) {
                        R.string.supervision_signal_installed_by_hint
                    } else {
                        R.string.supervision_signal_installed_by_unknown_hint
                    },
                    detailArg = installer,
                    kind = SignalKind.INFO,
                ),
            )
        }

        return SupervisionReport(
            signals = signals,
            isSupervised = signals.any { it.kind == SignalKind.RESTRICTION },
        )
    }
}

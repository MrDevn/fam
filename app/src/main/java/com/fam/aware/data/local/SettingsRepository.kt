package com.fam.aware.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.fam.aware.data.model.AppPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Небольшое локальное хранилище настроек приложения.
 *
 * Намеренно реализовано на [SharedPreferences]: подключение Room или DataStore
 * ради четырёх флагов — лишняя зависимость.
 */
interface SettingsRepository {

    /** Проходил ли пользователь первичную настройку. */
    val onboardingCompleted: Boolean

    /** Включены ли цвета Material You (динамическая палитра из обоев). */
    val dynamicColor: StateFlow<Boolean>

    /** Запрашивалось ли разрешение хотя бы один раз. */
    fun wasPermissionRequested(permission: AppPermission): Boolean

    fun setOnboardingCompleted()

    fun setDynamicColorEnabled(enabled: Boolean)

    /**
     * Фиксирует факт показа системного диалога.
     *
     * Нужно, чтобы отличать «ещё не спрашивали» от «спросили и получили отказ
     * с пометкой "больше не спрашивать"»: `shouldShowRequestPermissionRationale`
     * возвращает `false` в обоих случаях.
     */
    fun markPermissionRequested(permission: AppPermission)
}

class DefaultSettingsRepository(context: Context) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val dynamicColorState = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))

    override val onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    override val dynamicColor: StateFlow<Boolean> = dynamicColorState.asStateFlow()

    override fun wasPermissionRequested(permission: AppPermission): Boolean =
        prefs.getBoolean(KEY_REQUESTED_PREFIX + permission.name, false)

    override fun setOnboardingCompleted() {
        prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, true) }
    }

    override fun setDynamicColorEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DYNAMIC_COLOR, enabled) }
        dynamicColorState.value = enabled
    }

    override fun markPermissionRequested(permission: AppPermission) {
        prefs.edit { putBoolean(KEY_REQUESTED_PREFIX + permission.name, true) }
    }

    private companion object {
        const val PREFS_NAME = "fam_aware_prefs"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
        const val KEY_REQUESTED_PREFIX = "permission_requested_"
    }
}

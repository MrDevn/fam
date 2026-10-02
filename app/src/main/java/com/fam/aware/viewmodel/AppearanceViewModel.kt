package com.fam.aware.viewmodel

import androidx.lifecycle.ViewModel
import com.fam.aware.data.local.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel оформления.
 *
 * Держит единственную пользовательскую настройку темы — включены ли цвета
 * Material You. Тёмная тема при этом остаётся неизменной.
 */
class AppearanceViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val dynamicColor: StateFlow<Boolean> = settingsRepository.dynamicColor

    fun setDynamicColorEnabled(enabled: Boolean) {
        settingsRepository.setDynamicColorEnabled(enabled)
    }
}

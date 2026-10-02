package com.fam.aware.di

import androidx.lifecycle.CreationExtras
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fam.aware.viewmodel.AppearanceViewModel
import com.fam.aware.viewmodel.HomeViewModel
import com.fam.aware.viewmodel.OnboardingViewModel

/**
 * Единая фабрика ViewModel.
 *
 * Хранит ссылку только на [AppContainer]: UI-слой не знает, как создаются
 * репозитории, а слой данных ничего не знает про ViewModel.
 */
class FamViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = when {
        modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> OnboardingViewModel(
            supervisionRepository = container.supervisionRepository,
            permissionRepository = container.permissionRepository,
            settingsRepository = container.settingsRepository,
        )

        modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(
            supervisionRepository = container.supervisionRepository,
            permissionRepository = container.permissionRepository,
            settingsRepository = container.settingsRepository,
            notificationPublisher = container.notificationPublisher,
        )

        modelClass.isAssignableFrom(AppearanceViewModel::class.java) -> AppearanceViewModel(
            settingsRepository = container.settingsRepository,
        )

        else -> error("Unsupported ViewModel: ${modelClass.name}")
    } as T

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        throw IllegalStateException("Use create(modelClass, extras) — CreationExtras are required")
}

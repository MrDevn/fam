package com.fam.aware.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.fam.aware.FamApp
import com.fam.aware.ui.home.HomeScreen
import com.fam.aware.ui.onboarding.OnboardingScreen
import com.fam.aware.ui.persistent.PersistentAccessScreen
import com.fam.aware.ui.theme.Motion
import com.fam.aware.viewmodel.AppearanceViewModel

/** Экраны приложения. Навигационная библиотека не подключается — их три. */
internal enum class RootDestination { ONBOARDING, HOME, PERSISTENT_ACCESS }

/**
 * Корень навигации.
 *
 * Простой [AnimatedContent] полностью закрывает задачу переключения трёх экранов
 * и не добавляет зависимостей. Предиктивный «Назад» обрабатывается системно,
 * а на экране режима постоянного доступа есть явный [androidx.activity.compose.BackHandler].
 */
@Composable
fun AppRoot(
    appearanceViewModel: AppearanceViewModel,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val application = LocalContext.current.applicationContext as FamApp
    val settingsRepository = remember(application) { application.container.settingsRepository }

    var destination by remember(settingsRepository) {
        mutableStateOf(
            if (settingsRepository.onboardingCompleted) {
                RootDestination.HOME
            } else {
                RootDestination.ONBOARDING
            },
        )
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            // Вперёд — сдвиг влево, назад — сдвиг вправо.
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (
                fadeIn(tween(Motion.DurationLong)) +
                    slideInHorizontally(tween(Motion.DurationLong)) { width -> width / 6 * direction }
                ) togetherWith (
                    fadeOut(tween(Motion.DurationShort)) +
                        slideOutHorizontally(tween(Motion.DurationShort)) { width -> -width / 6 * direction }
                    )
        },
        label = "root-navigation",
    ) { target ->
        when (target) {
            RootDestination.ONBOARDING -> OnboardingScreen(
                viewModelFactory = viewModelFactory,
                onFinished = { destination = RootDestination.HOME },
            )

            RootDestination.HOME -> HomeScreen(
                viewModelFactory = viewModelFactory,
                appearanceViewModel = appearanceViewModel,
                onOpenSetup = { destination = RootDestination.ONBOARDING },
                onOpenPersistentAccess = { destination = RootDestination.PERSISTENT_ACCESS },
            )

            RootDestination.PERSISTENT_ACCESS -> PersistentAccessScreen(
                viewModelFactory = viewModelFactory,
                onBack = { destination = RootDestination.HOME },
            )
        }
    }
}

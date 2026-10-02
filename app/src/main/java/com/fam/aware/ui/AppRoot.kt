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
import androidx.lifecycle.ViewModelProvider
import com.fam.aware.FamApp
import com.fam.aware.ui.home.HomeScreen
import com.fam.aware.ui.onboarding.OnboardingScreen
import com.fam.aware.ui.theme.Motion
import com.fam.aware.viewmodel.AppearanceViewModel

/**
 * Корень навигации.
 *
 * Навигационная библиотека не подключается: в приложении два экрана,
 * и простой [AnimatedContent] полностью закрывает задачу, не добавляя зависимостей.
 */
@Composable
fun AppRoot(
    appearanceViewModel: AppearanceViewModel,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as FamApp
    val settingsRepository = remember(application) { application.container.settingsRepository }

    var showOnboarding by remember(settingsRepository) {
        mutableStateOf(!settingsRepository.onboardingCompleted)
    }

    AnimatedContent(
        targetState = showOnboarding,
        transitionSpec = {
            val direction = if (targetState) -1 else 1
            (
                fadeIn(tween(Motion.DurationLong)) +
                    slideInHorizontally(tween(Motion.DurationLong)) { width -> width / 6 * direction }
                ) togetherWith (
                    fadeOut(tween(Motion.DurationShort)) +
                        slideOutHorizontally(tween(Motion.DurationShort)) { width -> -width / 6 * direction }
                    )
        },
        label = "root-navigation",
    ) { onboarding ->
        if (onboarding) {
            OnboardingScreen(
                viewModelFactory = viewModelFactory,
                onFinished = { showOnboarding = false },
            )
        } else {
            HomeScreen(
                viewModelFactory = viewModelFactory,
                appearanceViewModel = appearanceViewModel,
                onOpenSetup = { showOnboarding = true },
            )
        }
    }
}

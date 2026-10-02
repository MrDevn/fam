package com.fam.aware.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.fam.aware.FamApp
import com.fam.aware.di.FamViewModelFactory

/**
 * Создаёт и запоминает фабрику ViewModel для текущего экрана.
 *
 * Контейнер зависимостей берётся из [FamApp], поэтому UI не знает
 * о конкретных реализациях репозиториев.
 */
@Composable
fun rememberViewModelFactory(): ViewModelProvider.Factory {
    val context = LocalContext.current
    return remember(context) {
        val application = context.applicationContext as FamApp
        FamViewModelFactory(application.container)
    }
}

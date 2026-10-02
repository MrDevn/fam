package com.fam.aware

import android.app.Application
import com.fam.aware.di.AppContainer

/**
 * Точка входа приложения.
 *
 * Здесь не регистрируются никакие фоновые сервисы, accessibility-сервисы,
 * администраторы устройства или receivers — только контейнер зависимостей.
 */
class FamApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

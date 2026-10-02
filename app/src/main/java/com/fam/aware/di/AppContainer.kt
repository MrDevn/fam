package com.fam.aware.di

import android.content.Context
import com.fam.aware.data.local.DefaultSettingsRepository
import com.fam.aware.data.local.SettingsRepository
import com.fam.aware.data.notification.DefaultNotificationPublisher
import com.fam.aware.data.notification.NotificationPublisher
import com.fam.aware.data.repository.DefaultPermissionRepository
import com.fam.aware.data.repository.DefaultSupervisionRepository
import com.fam.aware.data.repository.PermissionRepository
import com.fam.aware.data.repository.SupervisionRepository

/**
 * Ручной «контейнер зависимостей».
 *
 * Hilt/Dagger сюда не добавлены сознательно: зависимостей пять, и отдельный
 * фреймворк только усложнил бы сборку и увеличил размер APK.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val settingsRepository: SettingsRepository by lazy { DefaultSettingsRepository(appContext) }

    val supervisionRepository: SupervisionRepository by lazy { DefaultSupervisionRepository(appContext) }

    val permissionRepository: PermissionRepository by lazy {
        DefaultPermissionRepository(appContext, settingsRepository)
    }

    val notificationPublisher: NotificationPublisher by lazy { DefaultNotificationPublisher(appContext) }
}

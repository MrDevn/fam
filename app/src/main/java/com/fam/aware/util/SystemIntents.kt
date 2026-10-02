package com.fam.aware.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Переходы в системные экраны.
 *
 * Приложение никогда не меняет системные настройки само: оно может только
 * предложить пользователю (или родителю) открыть официальный экран Android.
 * Каждый вызов защищён от отсутствия обработчика.
 */
object SystemIntents {

    /** Справка Google Family Link — единственный внешний URL в приложении. */
    const val PARENT_HELP_URL = "https://support.google.com/families/"

    /** Экран «О приложении» в системных настройках. */
    fun openAppDetailsSettings(context: Context): Boolean = launch(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        },
    )

    /** Экран настроек уведомлений конкретного приложения (API 26+). */
    fun openNotificationSettings(context: Context): Boolean = launch(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )

    /** Общий список приложений — запасной вариант, если точный экран недоступен. */
    fun openAppsSettings(context: Context): Boolean = launch(
        context,
        Intent(Settings.ACTION_SETTINGS).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        },
    )

    /** Официальная справка по родительскому контролю Google Family Link. */
    fun openParentHelp(context: Context): Boolean = launch(
        context,
        Intent(Intent.ACTION_VIEW, Uri.parse(PARENT_HELP_URL)),
    )

    private fun launch(context: Context, intent: Intent): Boolean = runCatching {
        val safeIntent = if (context is android.app.Activity) {
            intent
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(safeIntent)
    }.recoverCatching {
        if (it is ActivityNotFoundException) {
            runCatching {
                context.startActivity(
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setComponent(null).setPackage(null),
                )
            }.isSuccess
        } else {
            false
        }
    }.getOrDefault(false)
}

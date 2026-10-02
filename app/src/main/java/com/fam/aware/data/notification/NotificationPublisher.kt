package com.fam.aware.data.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fam.aware.R

/**
 * Шлюз к уведомлениям — единственная подсистема, требующая разрешения.
 *
 * Приложение использует только локальные уведомления: ни foreground-сервисов,
 * ни wakelock, ни keep-alive механизмов здесь нет.
 */
interface NotificationPublisher {

    /** Создаёт каналы уведомлений, если их ещё нет. Безопасно вызывать многократно. */
    fun ensureChannels()

    /**
     * Пытается показать тестовое уведомление.
     *
     * @return `Result.failure` с [SecurityException], если разрешения нет,
     *   и с [IllegalStateException], если система всё равно не принимает уведомление.
     */
    fun publishTestNotification(): Result<Unit>

    /**
     * Показывает постоянную отметку о том, что режим постоянного доступа активен.
     *
     * Уведомление сделано неснимаемым специально: активный режим должен быть
     * виден и пользователю, и родителю. Оно исчезает сразу, как только режим
     * отключается — пользователем, по истечении срока или администратором.
     */
    fun publishPersistentAccessNotice(title: String, body: String): Result<Unit>

    /** Снимает постоянную отметку режима. */
    fun cancelPersistentAccessNotice()
}

class DefaultNotificationPublisher(
    private val context: Context,
) : NotificationPublisher {

    private val notificationManager: NotificationManager? =
        context.getSystemService(NotificationManager::class.java)

    private val notificationManagerCompat: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    override fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            createChannelIfMissing(
                id = CHANNEL_ID,
                name = context.getString(R.string.notification_channel_name),
                description = context.getString(R.string.notification_channel_description),
                importance = NotificationManager.IMPORTANCE_DEFAULT,
            )
            createChannelIfMissing(
                id = PERSISTENT_ACCESS_CHANNEL_ID,
                name = context.getString(R.string.notification_pa_channel_name),
                description = context.getString(R.string.notification_pa_channel_description),
                importance = NotificationManager.IMPORTANCE_LOW,
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun publishTestNotification(): Result<Unit> {
        ensureChannels()
        val denied = checkNotificationAvailability()
        if (denied != null) return Result.failure(denied)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_test_title))
            .setContentText(context.getString(R.string.notification_test_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .build()

        return runCatching {
            notificationManagerCompat.notify(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    override fun publishPersistentAccessNotice(title: String, body: String): Result<Unit> {
        ensureChannels()
        val denied = checkNotificationAvailability()
        if (denied != null) return Result.failure(denied)

        val notification = NotificationCompat.Builder(context, PERSISTENT_ACCESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return runCatching {
            notificationManagerCompat.notify(PERSISTENT_ACCESS_NOTIFICATION_ID, notification)
        }
    }

    override fun cancelPersistentAccessNotice() {
        runCatching { notificationManagerCompat.cancel(PERSISTENT_ACCESS_NOTIFICATION_ID) }
    }

    /**
     * Явная проверка перед отправкой: приложение не надеется на удачу
     * и не пытается обойти отказ системы.
     */
    private fun checkNotificationAvailability(): Exception? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return SecurityException("POST_NOTIFICATIONS is not granted")
        }
        if (!runCatching { notificationManagerCompat.areNotificationsEnabled() }.getOrDefault(false)) {
            return IllegalStateException(context.getString(R.string.notification_blocked_body))
        }
        return null
    }

    private fun createChannelIfMissing(
        id: String,
        name: String,
        description: String,
        importance: Int,
    ) {
        if (notificationManager?.getNotificationChannel(id) != null) return
        val channel = NotificationChannel(id, name, importance).apply {
            this.description = description
        }
        notificationManager?.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "fam_aware_status"
        const val PERSISTENT_ACCESS_CHANNEL_ID = "fam_aware_persistent_access"
        const val NOTIFICATION_ID = 1001
        const val PERSISTENT_ACCESS_NOTIFICATION_ID = 1002
    }
}

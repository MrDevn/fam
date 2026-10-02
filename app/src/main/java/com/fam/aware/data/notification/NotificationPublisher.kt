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

/** Отправляет локальное уведомление — единственная функция, требующая разрешения. */
interface NotificationPublisher {

    /** Создаёт канал уведомлений, если его ещё нет. Безопасно вызывать многократно. */
    fun ensureChannel()

    /**
     * Пытается показать тестовое уведомление.
     *
     * @return `Result.failure` с [SecurityException], если разрешения нет,
     *   и с [IllegalStateException], если система всё равно не принимает уведомление.
     */
    fun publishTestNotification(): Result<Unit>
}

class DefaultNotificationPublisher(
    private val context: Context,
) : NotificationPublisher {

    private val notificationManager: NotificationManager? =
        context.getSystemService(NotificationManager::class.java)

    override fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val existing = notificationManager?.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return@runCatching
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    override fun publishTestNotification(): Result<Unit> {
        ensureChannel()

        // Явная проверка перед отправкой: приложение не надеется на удачу
        // и не пытается обойти отказ системы.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure(SecurityException("POST_NOTIFICATIONS is not granted"))
        }
        if (!runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }
                .getOrDefault(false)
        ) {
            return Result.failure(IllegalStateException(context.getString(R.string.notification_blocked_body)))
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_test_title))
            .setContentText(context.getString(R.string.notification_test_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val CHANNEL_ID = "fam_aware_status"
        const val NOTIFICATION_ID = 1001
    }
}

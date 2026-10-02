package com.fam.aware.data.repository

import android.content.Context
import android.content.Intent
import android.content.RestrictionsManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.fam.aware.R
import com.fam.aware.data.model.AccessDuration
import com.fam.aware.data.model.ApprovalSource
import com.fam.aware.data.model.PersistentAccessInput
import com.fam.aware.data.model.PersistentAccessPolicy
import com.fam.aware.data.model.PersistentAccessStatus
import com.fam.aware.data.model.SupervisionAnalyzer
import com.fam.aware.data.notification.NotificationPublisher
import com.fam.aware.data.model.sourceLabel
import com.fam.aware.util.TimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ключ ограничения приложения, которое родитель или администратор устройства
 * выставляет через официальный механизм Android app restrictions
 * (`RestrictionsManager` / `ApplicationRestrictionsController` / MDM).
 *
 * Значение объявлено в `res/xml/app_restrictions.xml` и отдаётся системе
 * через [com.fam.aware.receiver.AppRestrictionsReceiver].
 */
const val RESTRICTION_KEY_PERSISTENT_ACCESS = "persistentAccessAllowed"

/**
 * Хранение и вычисление состояния «режима постоянного доступа».
 *
 * Используются только официальные механизмы:
 *  - app restrictions (`RestrictionsManager.getApplicationRestrictions`) — решение администратора;
 *  - local approval (`RestrictionsManager.createLocalApprovalIntent`) — подтверждение родителя;
 *  - обычное локальное хранилище — подтверждение, данное на самом устройстве, всегда со сроком.
 *
 * Приложение не запрашивает Device Owner / Profile Owner, не использует
 * `DevicePolicyManager` и не мешает родителю отозвать разрешение или удалить приложение.
 */
interface PersistentAccessRepository {

    /** Доступные сроки действия локального подтверждения. */
    val durations: List<AccessDuration>

    /** Перечитывает все источники и возвращает актуальный статус режима. */
    suspend fun status(): PersistentAccessStatus

    /**
     * Intent официального диалога согласования с родителем
     * или `null`, если в системе нет провайдера ограничений.
     */
    fun createParentApprovalIntent(): Intent?

    /** Фиксирует, что запрос родителю отправлен и ответ ещё не получен. */
    fun markApprovalRequested()

    /** Отменяет неотвеченный запрос родителю. */
    fun cancelApprovalRequest()

    /** Обрабатывает ответ системного диалога согласования. */
    fun onOfficialApprovalResult(approved: Boolean, duration: AccessDuration)

    /**
     * Локальное подтверждение на устройстве без Family Link.
     * Не даёт системных привилегий и всегда ограничено по сроку.
     */
    fun confirmLocally(duration: AccessDuration)

    /** Полностью выключает режим и снимает постоянную отметку. */
    fun revoke()
}

class DefaultPersistentAccessRepository(
    private val context: Context,
    private val supervisionRepository: SupervisionRepository,
    private val notificationPublisher: NotificationPublisher,
) : PersistentAccessRepository {

    private val restrictionsManager: RestrictionsManager? =
        context.getSystemService(RestrictionsManager::class.java)

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override val durations: List<AccessDuration> = AccessDuration.entries.toList()

    override suspend fun status(): PersistentAccessStatus = withContext(Dispatchers.Default) {
        val input = PersistentAccessInput(
            adminAllows = readAdminRestriction(),
            approvedAtMillis = approvedAt(),
            expiresAtMillis = expiresAt(),
            approvalSource = storedSource(),
            pendingApproval = prefs.getBoolean(KEY_PENDING_APPROVAL, false),
            notificationsUsable = notificationsUsable(),
            approvalRequestAvailable = createParentApprovalIntent() != null,
            supervised = isDeviceSupervised(),
            nowMillis = System.currentTimeMillis(),
        )
        PersistentAccessPolicy.evaluate(input).also { syncNotice(it) }
    }

    override fun createParentApprovalIntent(): Intent? = runCatching {
        restrictionsManager?.createLocalApprovalIntent()
    }.getOrNull()

    override fun markApprovalRequested() {
        prefs.edit { putBoolean(KEY_PENDING_APPROVAL, true) }
    }

    override fun cancelApprovalRequest() {
        prefs.edit { putBoolean(KEY_PENDING_APPROVAL, false) }
    }

    override fun onOfficialApprovalResult(approved: Boolean, duration: AccessDuration) {
        if (approved) {
            grant(ApprovalSource.OFFICIAL_APPROVAL, duration)
        } else {
            revoke()
        }
    }

    override fun confirmLocally(duration: AccessDuration) {
        grant(ApprovalSource.LOCAL_ATTESTATION, duration)
    }

    override fun revoke() {
        prefs.edit {
            putBoolean(KEY_PENDING_APPROVAL, false)
            remove(KEY_APPROVED_AT)
            remove(KEY_EXPIRES_AT)
            remove(KEY_SOURCE)
        }
        notificationPublisher.cancelPersistentAccessNotice()
    }

    private fun grant(source: ApprovalSource, duration: AccessDuration) {
        val now = System.currentTimeMillis()
        prefs.edit {
            putBoolean(KEY_PENDING_APPROVAL, false)
            putLong(KEY_APPROVED_AT, now)
            putLong(KEY_EXPIRES_AT, now + duration.hours * MILLIS_PER_HOUR)
            putString(KEY_SOURCE, source.name)
        }
    }

    /**
     * Читает решение администратора устройства.
     *
     * `null` означает «администратор этот ключ не задавал» — это не то же самое,
     * что запрет, поэтому состояние вычисляется дальше по локальным данным.
     */
    private fun readAdminRestriction(): Boolean? = runCatching {
        val bundle = restrictionsManager?.applicationRestrictions ?: return@runCatching null
        if (!bundle.containsKey(RESTRICTION_KEY_PERSISTENT_ACCESS)) return@runCatching null
        when (val raw = bundle.get(RESTRICTION_KEY_PERSISTENT_ACCESS)) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> raw.equals("true", ignoreCase = true)
            else -> null
        }
    }.getOrNull()

    private suspend fun isDeviceSupervised(): Boolean = runCatching {
        SupervisionAnalyzer.analyze(supervisionRepository.collect()).isSupervised
    }.getOrDefault(false)

    private fun notificationsUsable(): Boolean {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val systemEnabled = runCatching {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }.getOrDefault(false)
        return runtimeGranted && systemEnabled
    }

    private fun approvedAt(): Long? =
        prefs.getLong(KEY_APPROVED_AT, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }

    private fun expiresAt(): Long? =
        prefs.getLong(KEY_EXPIRES_AT, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }

    private fun storedSource(): ApprovalSource {
        val name = prefs.getString(KEY_SOURCE, null) ?: return ApprovalSource.NONE
        return runCatching { ApprovalSource.valueOf(name) }.getOrDefault(ApprovalSource.NONE)
    }

    /**
     * Держит постоянную отметку режима в актуальном состоянии.
     *
     * Если режим не активен — отметка снимается. Если активна, но уведомления
     * выключены, политика и так не позволит режиму работать.
     */
    private fun syncNotice(status: PersistentAccessStatus) {
        if (!status.isActive) {
            notificationPublisher.cancelPersistentAccessNotice()
            return
        }
        val expiresAt = status.expiresAtMillis
        val body = if (status.source == ApprovalSource.ADMIN_RESTRICTION || expiresAt == null) {
            context.getString(R.string.notification_pa_body_admin)
        } else {
            context.getString(
                R.string.notification_pa_body_until,
                TimeFormatter.format(expiresAt),
                context.getString(sourceLabel(status.source)),
            )
        }
        notificationPublisher.publishPersistentAccessNotice(
            title = context.getString(R.string.notification_pa_title),
            body = body,
        )
    }

    private companion object {
        const val PREFS_NAME = "fam_aware_prefs"
        const val KEY_PENDING_APPROVAL = "pa_pending_approval"
        const val KEY_APPROVED_AT = "pa_approved_at"
        const val KEY_EXPIRES_AT = "pa_expires_at"
        const val KEY_SOURCE = "pa_source"
        const val MILLIS_PER_HOUR = 60L * 60L * 1000L
    }
}

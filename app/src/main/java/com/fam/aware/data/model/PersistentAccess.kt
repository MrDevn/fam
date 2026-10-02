package com.fam.aware.data.model

import androidx.annotation.StringRes
import com.fam.aware.R

/**
 * Состояния «режима постоянного доступа».
 *
 * Режим — это внутреннее состояние самого приложения: оно отключает собственные
 * напоминания и показывает постоянную отметку о том, что режим активен.
 * Режим НЕ изменяет системные лимиты экранного времени — это может сделать
 * только родитель в Family Link или администратор устройства.
 */
enum class PersistentAccessState {
    /** Режим выключен, подтверждение не запрашивалось. */
    DISABLED,

    /** Запрос отправлен родителю через официальный механизм согласования. */
    PENDING_APPROVAL,

    /** Режим активен: есть разрешение администратора или действующее подтверждение. */
    ACTIVE,

    /** Срок локального подтверждения истёк, нужно новое подтверждение. */
    EXPIRED,

    /** Администратор устройства явно запретил режим. Локально не переопределяется. */
    REVOKED_BY_ADMIN,

    /**
     * Режим недоступен, потому что уведомления выключены.
     *
     * Требование прозрачности: активный режим обязан быть виден пользователю
     * и родителю, поэтому без разрешения на уведомления он не включается.
     */
    UNAVAILABLE_NO_NOTIFICATIONS,
}

/** Кто именно разрешил режим. */
enum class ApprovalSource {
    /** Разрешения нет. */
    NONE,

    /** Администратор устройства через официальный механизм app restrictions. */
    ADMIN_RESTRICTION,

    /** Родитель подтвердил через системный диалог согласования (RestrictionsManager). */
    OFFICIAL_APPROVAL,

    /**
     * Локальное подтверждение на устройстве без Family Link.
     * Не даёт никаких системных привилегий и всегда ограничено по сроку.
     */
    LOCAL_ATTESTATION,
}

/** Строковый ресурс с названием источника подтверждения. */
@StringRes
fun sourceLabel(source: ApprovalSource): Int = when (source) {
    ApprovalSource.NONE -> R.string.pa_source_none
    ApprovalSource.ADMIN_RESTRICTION -> R.string.pa_source_admin
    ApprovalSource.OFFICIAL_APPROVAL -> R.string.pa_source_official
    ApprovalSource.LOCAL_ATTESTATION -> R.string.pa_source_local
}

/** Срок действия локального подтверждения. */
enum class AccessDuration(
    val hours: Int,
    @StringRes val labelRes: Int,
) {
    ONE_HOUR(1, R.string.duration_1_hour),
    SIX_HOURS(6, R.string.duration_6_hours),
    TWELVE_HOURS(12, R.string.duration_12_hours),
    TWENTY_FOUR_HOURS(24, R.string.duration_24_hours),
}

/**
 * Входные данные для решения о состоянии режима.
 *
 * Класс не зависит от Android, поэтому правила можно проверять JVM-тестами.
 *
 * @param adminAllows значение ограничения `persistentAccessAllowed`, заданного
 *   администратором; `null`, если администратор его не задавал.
 * @param approvedAtMillis момент локального подтверждения, если оно есть.
 * @param expiresAtMillis момент истечения локального подтверждения.
 * @param approvalSource источник действующего подтверждения.
 * @param pendingApproval отправлен ли запрос родителю и ответ ещё не получен.
 * @param notificationsUsable можно ли показать постоянную отметку режима.
 * @param approvalRequestAvailable доступен ли официальный диалог согласования
 *   (`RestrictionsManager.createLocalApprovalIntent` вернул не null).
 * @param supervised находится ли устройство под управлением родителя/администратора.
 * @param nowMillis текущее время.
 */
data class PersistentAccessInput(
    val adminAllows: Boolean?,
    val approvedAtMillis: Long?,
    val expiresAtMillis: Long?,
    val approvalSource: ApprovalSource,
    val pendingApproval: Boolean,
    val notificationsUsable: Boolean,
    val approvalRequestAvailable: Boolean,
    val supervised: Boolean,
    val nowMillis: Long,
) {
    companion object {
        val INACTIVE = PersistentAccessInput(
            adminAllows = null,
            approvedAtMillis = null,
            expiresAtMillis = null,
            approvalSource = ApprovalSource.NONE,
            pendingApproval = false,
            notificationsUsable = true,
            approvalRequestAvailable = false,
            supervised = false,
            nowMillis = 0L,
        )
    }
}

/** Итоговый статус режима для UI. */
data class PersistentAccessStatus(
    val state: PersistentAccessState,
    val source: ApprovalSource,
    val approvedAtMillis: Long?,
    val expiresAtMillis: Long?,
    val adminRestrictionSet: Boolean,
    val approvalRequestAvailable: Boolean,
    val supervised: Boolean,
    val notificationsUsable: Boolean,
) {
    val isActive: Boolean get() = state == PersistentAccessState.ACTIVE

    /**
     * Администратор явно запретил режим — приложение не предлагает ни повторный
     * запрос, ни локальное подтверждение, только инструкцию для родителя.
     */
    val isForbiddenByAdmin: Boolean get() = state == PersistentAccessState.REVOKED_BY_ADMIN

    /** Можно ли включить режим локальным подтверждением. */
    val canActivateLocally: Boolean
        get() = !isForbiddenByAdmin &&
            state != PersistentAccessState.ACTIVE &&
            state != PersistentAccessState.PENDING_APPROVAL &&
            notificationsUsable

    /** Можно ли запросить официальное подтверждение родителя. */
    val canRequestParentApproval: Boolean
        get() = !isForbiddenByAdmin &&
            state != PersistentAccessState.ACTIVE &&
            approvalRequestAvailable
}

/**
 * Правила перехода состояний режима постоянного доступа.
 *
 * Приоритет всегда у администратора устройства: если ограничение
 * `persistentAccessAllowed` выставлено в `false`, режим выключен и локально
 * не включается. Приложение при этом ничего не пытается обойти.
 */
object PersistentAccessPolicy {

    fun evaluate(input: PersistentAccessInput): PersistentAccessStatus = when {
        input.adminAllows == false -> status(input, PersistentAccessState.REVOKED_BY_ADMIN, ApprovalSource.NONE)

        input.adminAllows == true -> status(
            input = input,
            state = PersistentAccessState.ACTIVE,
            source = ApprovalSource.ADMIN_RESTRICTION,
            expiresAtMillis = null,
        )

        !input.notificationsUsable -> status(
            input,
            PersistentAccessState.UNAVAILABLE_NO_NOTIFICATIONS,
            ApprovalSource.NONE,
        )

        input.approvedAtMillis != null && input.expiresAtMillis != null -> {
            if (input.nowMillis <= input.expiresAtMillis) {
                status(input, PersistentAccessState.ACTIVE, input.approvalSource)
            } else {
                // Срок истёк — подтверждение больше не действует.
                status(input, PersistentAccessState.EXPIRED, ApprovalSource.NONE)
            }
        }

        input.pendingApproval -> status(input, PersistentAccessState.PENDING_APPROVAL, ApprovalSource.NONE)

        else -> status(input, PersistentAccessState.DISABLED, ApprovalSource.NONE)
    }

    private fun status(
        input: PersistentAccessInput,
        state: PersistentAccessState,
        source: ApprovalSource,
        expiresAtMillis: Long? = input.expiresAtMillis,
    ): PersistentAccessStatus = PersistentAccessStatus(
        state = state,
        source = source,
        approvedAtMillis = if (state == PersistentAccessState.ACTIVE) input.approvedAtMillis else null,
        expiresAtMillis = if (state == PersistentAccessState.ACTIVE) expiresAtMillis else null,
        adminRestrictionSet = input.adminAllows != null,
        approvalRequestAvailable = input.approvalRequestAvailable,
        supervised = input.supervised,
        notificationsUsable = input.notificationsUsable,
    )
}

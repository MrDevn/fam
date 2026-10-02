package com.fam.aware.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты правил режима постоянного доступа.
 *
 * Главные инварианты:
 *  - запрет администратора всегда побеждает локальное подтверждение;
 *  - без положительного подтверждения режим не активируется;
 *  - локальное подтверждение обязательно истекает;
 *  - без уведомлений режим недоступен (требование прозрачности).
 */
class PersistentAccessPolicyTest {

    private val now = 1_000_000L
    private val hour = 60L * 60L * 1000L

    @Test
    fun `admin prohibition overrides any local approval`() {
        val status = PersistentAccessPolicy.evaluate(
            PersistentAccessInput.INACTIVE.copy(
                adminAllows = false,
                approvedAtMillis = now - hour,
                expiresAtMillis = now + hour,
                approvalSource = ApprovalSource.LOCAL_ATTESTATION,
                nowMillis = now,
            ),
        )

        assertEquals(PersistentAccessState.REVOKED_BY_ADMIN, status.state)
        assertFalse(status.isActive)
        assertTrue(status.isForbiddenByAdmin)
        assertFalse(status.canActivateLocally)
        assertFalse(status.canRequestParentApproval)
    }

    @Test
    fun `admin approval activates the mode without expiry`() {
        val status = PersistentAccessPolicy.evaluate(
            PersistentAccessInput.INACTIVE.copy(adminAllows = true, nowMillis = now),
        )

        assertEquals(PersistentAccessState.ACTIVE, status.state)
        assertEquals(ApprovalSource.ADMIN_RESTRICTION, status.source)
        assertEquals(null, status.expiresAtMillis)
        assertTrue(status.isActive)
    }

    @Test
    fun `mode is disabled while nothing is approved`() {
        val status = PersistentAccessPolicy.evaluate(
            PersistentAccessInput.INACTIVE.copy(nowMillis = now),
        )

        assertEquals(PersistentAccessState.DISABLED, status.state)
        assertFalse(status.isActive)
    }

    @Test
    fun `local approval is active only until it expires`() {
        val active = PersistentAccessPolicy.evaluate(
            PersistentAccessInput.INACTIVE.copy(
                approvedAtMillis = now - hour,
                expiresAtMillis = now + hour,
                approvalSource = ApprovalSource.LOCAL_ATTESTATION,
                nowMillis = now,
            ),
        )
        val expired = PersistentAccessPolicy.evaluate(
            PersistentAccessInput.INACTIVE.copy(
                approvedAtMillis = now - 2 * hour,
                expiresAtMillis = now - hour,
                approvalSource = ApprovalSource.LOCAL_ATTESTATION,
                nowMillis = now,
            ),
        )

        assertEquals(PersistentAccessState.ACTIVE, active.state)
        assertEquals(ApprovalSource.LOCAL_ATTESTATION, active.source)
        assertEquals(PersistentAccessState.EXPIRED, expired.state)
        assertFalse(expired.isActive)
    }

    @Test
    fun `official parent approval is reported as its own source`() {
        val status = PersistentAccessPolicy.evaluate(
            PersistentAccessInput.INACTIVE.copy(
                approvedAtMillis = now,
                expiresAtMillis = now + 12 * hour,
                approvalSource = ApprovalSource.OFFICIAL_APPROVAL,
                nowMillis = now,
            ),
        )

        assertEquals(ApprovalSource.OFFICIAL_APPROVAL, status.source)
        assertTrue(status.isActive)
    }

    @Test
    fun `mode is unavailable without notification permission`() {
        val status = PersistentAccessPolicy.evaluate(
            PersistentAccessInput.INACTIVE.copy(
                notificationsUsable = false,
                approvedAtMillis = now,
                expiresAtMillis = now + hour,
                approvalSource = ApprovalSource.OFFICIAL_APPROVAL,
                nowMillis = now,
            ),
        )

        assertEquals(PersistentAccessState.UNAVAILABLE_NO_NOTIFICATIONS, status.state)
        assertFalse(status.canActivateLocally)
    }

    @Test
    fun `pending approval does not activate the mode`() {
        val status = PersistentAccessPolicy.evaluate(
            PersistentAccessInput.INACTIVE.copy(pendingApproval = true, nowMillis = now),
        )

        assertEquals(PersistentAccessState.PENDING_APPROVAL, status.state)
        assertFalse(status.isActive)
    }

    @Test
    fun `parent approval can only be requested when a provider exists`() {
        val withProvider = PersistentAccessPolicy.evaluate(
            PersistentAccessInput.INACTIVE.copy(approvalRequestAvailable = true, nowMillis = now),
        )
        val withoutProvider = PersistentAccessPolicy.evaluate(
            PersistentAccessInput.INACTIVE.copy(approvalRequestAvailable = false, nowMillis = now),
        )

        assertTrue(withProvider.canRequestParentApproval)
        assertFalse(withoutProvider.canRequestParentApproval)
        // Локальное подтверждение доступно в обоих случаях — оно ничего системного не даёт.
        assertTrue(withProvider.canActivateLocally)
        assertTrue(withoutProvider.canActivateLocally)
    }

    @Test
    fun `active mode cannot be re-confirmed locally`() {
        val status = PersistentAccessPolicy.evaluate(
            PersistentAccessInput.INACTIVE.copy(
                approvedAtMillis = now,
                expiresAtMillis = now + hour,
                approvalSource = ApprovalSource.LOCAL_ATTESTATION,
                nowMillis = now,
            ),
        )

        assertTrue(status.isActive)
        assertFalse(status.canActivateLocally)
    }
}

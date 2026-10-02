package com.fam.aware.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты правил, по которым приложение решает, можно ли снова просить разрешение.
 *
 * Ключевое требование: при блокировке политикой или при выборе
 * «Больше не спрашивать» автоматический повторный запрос запрещён.
 */
class PermissionPolicyTest {

    @Test
    fun `policy block forbids re-request`() {
        assertTrue(PermissionState.BLOCKED_BY_POLICY.reRequestForbidden)
    }

    @Test
    fun `permanent denial forbids re-request`() {
        assertTrue(PermissionState.DENIED_PERMANENTLY.reRequestForbidden)
    }

    @Test
    fun `simple denial still allows an explicit user-initiated request`() {
        assertFalse(PermissionState.DENIED.reRequestForbidden)
        assertFalse(PermissionState.NOT_REQUESTED.reRequestForbidden)
    }

    @Test
    fun `policy revocation is treated as supervision block even when granted`() {
        val report = report(state = PermissionState.GRANTED, revokedByPolicy = true)

        assertTrue(report.isBlockedBySupervision)
        assertFalse(report.isDeniedByUser)
    }

    @Test
    fun `granted permission with disabled system toggle is not usable`() {
        val grantedButSilent = report(
            state = PermissionState.GRANTED,
            featureEnabledInSystem = false,
        )
        val grantedAndActive = report(
            state = PermissionState.GRANTED,
            featureEnabledInSystem = true,
        )

        assertFalse(grantedButSilent.isUsable)
        assertTrue(grantedAndActive.isUsable)
    }

    @Test
    fun `user denial is not reported as supervision block`() {
        val report = report(state = PermissionState.DENIED_PERMANENTLY)

        assertTrue(report.isDeniedByUser)
        assertFalse(report.isBlockedBySupervision)
    }

    @Test
    fun `not applicable state is usable when the system toggle is on`() {
        val report = report(
            state = PermissionState.NOT_APPLICABLE,
            featureEnabledInSystem = true,
        )

        assertTrue(report.isUsable)
        assertFalse(report.isDeniedByUser)
    }

    private fun report(
        state: PermissionState,
        featureEnabledInSystem: Boolean = true,
        revokedByPolicy: Boolean = false,
    ): PermissionReport = PermissionReport(
        permission = AppPermission.NOTIFICATIONS,
        state = state,
        featureEnabledInSystem = featureEnabledInSystem,
        revokedByPolicy = revokedByPolicy,
        wasRequested = true,
        rationaleVisible = false,
    )
}

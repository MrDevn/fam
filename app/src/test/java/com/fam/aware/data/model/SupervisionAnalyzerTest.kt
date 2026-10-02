package com.fam.aware.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты чистой логики диагностики.
 *
 * Проверяют главное требование: приложение корректно определяет ограничения,
 * но никогда не помечает «чистое» устройство как заблокированное.
 */
class SupervisionAnalyzerTest {

    @Test
    fun `clean device is not reported as supervised`() {
        val report = SupervisionAnalyzer.analyze(
            RawSupervisionData.EMPTY.copy(installingPackage = "com.android.vending"),
        )

        assertFalse(report.isSupervised)
        assertTrue(report.restrictions.isEmpty())
        // Источник установки — справочная информация, а не ограничение.
        assertEquals(1, report.info.size)
        assertEquals("installed_by", report.info.first().id)
    }

    @Test
    fun `active restrictions provider counts as supervision`() {
        val report = SupervisionAnalyzer.analyze(
            RawSupervisionData.EMPTY.copy(restrictionsProviderPresent = true),
        )

        assertTrue(report.isSupervised)
        assertTrue(report.restrictions.any { it.id == "restrictions_provider" })
    }

    @Test
    fun `managed profile and user restrictions are reported`() {
        val report = SupervisionAnalyzer.analyze(
            RawSupervisionData.EMPTY.copy(
                managedProfile = true,
                activeUserRestrictions = listOf(
                    UserRestrictionKey.INSTALL_APPS,
                    UserRestrictionKey.FUN,
                ),
            ),
        )

        assertTrue(report.isSupervised)
        val ids = report.restrictions.map { it.id }
        assertTrue(ids.contains("managed_profile"))
        assertTrue(ids.contains("user_restriction_install_apps"))
        assertTrue(ids.contains("user_restriction_fun"))
    }

    @Test
    fun `application restrictions are merged into a single signal`() {
        val report = SupervisionAnalyzer.analyze(
            RawSupervisionData.EMPTY.copy(
                appRestrictions = mapOf("canUseNotifications" to "false"),
            ),
        )

        val signal = report.restrictions.single { it.id == "app_restrictions" }
        assertEquals("canUseNotifications = false", signal.detailArg)
    }

    @Test
    fun `disabled-by-admin signal is reported first`() {
        val report = SupervisionAnalyzer.analyze(
            RawSupervisionData.EMPTY.copy(
                appDisabledByAdmin = true,
                demoUser = true,
            ),
        )

        assertEquals("app_disabled_by_admin", report.restrictions.first().id)
        assertTrue(report.isSupervised)
    }

    @Test
    fun `missing installation source does not break the report`() {
        val report = SupervisionAnalyzer.analyze(RawSupervisionData.EMPTY)

        assertFalse(report.isSupervised)
        assertEquals(1, report.signals.size)
        assertNull(report.signals.first().detailArg)
    }
}

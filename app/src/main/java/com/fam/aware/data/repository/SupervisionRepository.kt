package com.fam.aware.data.repository

import com.fam.aware.data.model.RawSupervisionData
import com.fam.aware.data.model.UserRestrictionKey

/**
 * Сбор фактов об ограничениях устройства.
 *
 * Используются только публичные API Android, не требующие привилегий:
 * `UserManager`, `RestrictionsManager`, `PackageManager`.
 * Приложение ничего не меняет и не обходит — только читает состояние.
 */
interface SupervisionRepository {

    /**
     * Собирает сырые данные об ограничениях.
     *
     * Метод может бросить исключение, если система запретит один из вызовов;
     * отдельные недоступные проверки внутри реализации глушатся и не ломают отчёт.
     */
    suspend fun collect(): RawSupervisionData

    /** Список проверяемых пользовательских ограничений. */
    val inspectedUserRestrictions: List<UserRestrictionKey>
}

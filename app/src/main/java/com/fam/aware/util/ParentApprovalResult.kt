package com.fam.aware.util

import android.content.Intent
import android.content.RestrictionsManager

/**
 * Разбор ответа системного диалога согласования с родителем.
 *
 * Используется только официальный результат
 * `RestrictionsManager.createLocalApprovalIntent()`: приложение не пытается
 * ни подделать ответ, ни продолжить работу без него.
 */
object ParentApprovalResult {

    /**
     * @return `true` — родитель разрешил, `false` — отказал или произошла ошибка,
     *   `null` — ответа нет (диалог не возвращал bundle).
     */
    fun from(intent: Intent?): Boolean? {
        val bundle = runCatching {
            intent?.getBundleExtra(RestrictionsManager.EXTRA_RESPONSE_BUNDLE)
        }.getOrNull() ?: return null

        if (!bundle.containsKey(RestrictionsManager.RESPONSE_KEY_RESULT)) return null

        return runCatching {
            bundle.getInt(RestrictionsManager.RESPONSE_KEY_RESULT) == RestrictionsManager.RESULT_APPROVED
        }.getOrNull()
    }
}

package com.fam.aware.data.model

import androidx.annotation.StringRes
import com.fam.aware.R

/**
 * Перечисление всех runtime-разрешений, которые приложение когда-либо запрашивает.
 *
 * Приложение намеренно ограничивается одним разрешением: чем меньше прав,
 * тем выше шанс штатной работы под родительским контролем и тем проще
 * родителю принять решение о выдаче.
 */
enum class AppPermission(
    /** Имя разрешения ровно так, как оно объявлено в AndroidManifest.xml. */
    val manifestName: String,
    /** Минимальный API, на котором разрешение запрашивается в runtime. */
    val runtimeSinceApi: Int,
    @StringRes val titleRes: Int,
    @StringRes val technicalNameRes: Int,
    @StringRes val reasonRes: Int,
    @StringRes val ifDeniedRes: Int,
) {
    NOTIFICATIONS(
        manifestName = "android.permission.POST_NOTIFICATIONS",
        runtimeSinceApi = 33,
        titleRes = R.string.perm_notifications_title,
        technicalNameRes = R.string.perm_notifications_technical_name,
        reasonRes = R.string.perm_notifications_why,
        ifDeniedRes = R.string.perm_notifications_if_denied,
    );

    companion object {
        val ALL: List<AppPermission> = entries.toList()
    }
}

package com.fam.aware.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** Доступна ли динамическая палитра Material You на текущем устройстве. */
val LocalDynamicColorSupported = staticCompositionLocalOf { false }

/**
 * Приложение всегда использует тёмную тему — это требование дизайна и одновременно
 * способ не запрашивать лишних системных данных.
 *
 * @param dynamicColor включать ли Material You (палитру из обоев, Android 12+).
 *   Управляется пользователем, по умолчанию выключено.
 */
@Composable
fun FamilyAwareTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = if (dynamicColor && supportsDynamicColor) {
        dynamicDarkColorScheme(context)
    } else {
        DarkColors
    }

    CompositionLocalProvider(LocalDynamicColorSupported provides supportsDynamicColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FamilyAwareTypography,
            content = content,
        )
    }
}

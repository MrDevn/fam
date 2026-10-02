package com.fam.aware.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Единые параметры движения.
 *
 * Значения соответствуют рекомендациям Material 3 (emphasized / standard),
 * чтобы анимации выглядели согласованно на всех экранах.
 */
object Motion {
    private val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    private val EmphasizedEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    const val DurationShort = 150
    const val DurationMedium = 250
    const val DurationLong = 400

    fun <T> standard(delayMillis: Int = 0) = tween<T>(
        durationMillis = DurationMedium,
        delayMillis = delayMillis,
        easing = StandardEasing,
    )

    fun <T> emphasized(delayMillis: Int = 0) = tween<T>(
        durationMillis = DurationLong,
        delayMillis = delayMillis,
        easing = EmphasizedEasing,
    )
}

/** Сетка отступов приложения. */
object Dimens {
    val ScreenPadding: Dp = 20.dp
    val CardPadding: Dp = 18.dp
    val ItemGap: Dp = 12.dp
    val SectionGap: Dp = 24.dp
    val CardCornerRadius: Dp = 22.dp
    val IconSize: Dp = 22.dp
}

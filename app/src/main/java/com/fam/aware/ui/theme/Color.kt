package com.fam.aware.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Базовая тёмная палитра приложения (используется, если Material You выключен).
private val Mint = Color(0xFF6FE3C4)
private val MintDim = Color(0xFF00513F)
private val MintBright = Color(0xFF8CFFDE)
private val MintDeep = Color(0xFF00382C)

private val Sage = Color(0xFFA9C9C0)
private val SageContainer = Color(0xFF2A4A43)
private val SageDeep = Color(0xFF14332C)
private val SageBright = Color(0xFFC5E9DE)

private val Sky = Color(0xFFA6C8E8)
private val SkyContainer = Color(0xFF24486A)
private val SkyDeep = Color(0xFF0B3150)
private val SkyBright = Color(0xFFD3E4FF)

private val Background = Color(0xFF0B0E13)
private val SurfaceBase = Color(0xFF0B0E13)
private val SurfaceLowest = Color(0xFF070A0E)
private val SurfaceLow = Color(0xFF10151B)
private val SurfaceMid = Color(0xFF151B22)
private val SurfaceHigh = Color(0xFF1B222A)
private val SurfaceHighest = Color(0xFF212932)

private val OnSurface = Color(0xFFE2E6EC)
private val OnSurfaceVariant = Color(0xFFC3C9D1)
private val SurfaceVariant = Color(0xFF2A3138)

private val ErrorColor = Color(0xFFFFB4AB)
private val OnError = Color(0xFF690005)
private val ErrorContainer = Color(0xFF93000A)
private val OnErrorContainer = Color(0xFFFFDAD6)

private val Outline = Color(0xFF7C868F)
private val OutlineVariant = Color(0xFF333B44)

private val InverseSurface = Color(0xFFE2E6EC)
private val InverseOnSurface = Color(0xFF1B222A)
private val InversePrimary = Color(0xFF006B54)

val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = MintDeep,
    primaryContainer = MintDim,
    onPrimaryContainer = MintBright,
    inversePrimary = InversePrimary,
    secondary = Sage,
    onSecondary = SageDeep,
    secondaryContainer = SageContainer,
    onSecondaryContainer = SageBright,
    tertiary = Sky,
    onTertiary = SkyDeep,
    tertiaryContainer = SkyContainer,
    onTertiaryContainer = SkyBright,
    background = Background,
    onBackground = OnSurface,
    surface = SurfaceBase,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = Mint,
    surfaceContainerLowest = SurfaceLowest,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = SurfaceMid,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHighest,
    surfaceBright = SurfaceHighest,
    surfaceDim = SurfaceLowest,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    error = ErrorColor,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = Outline,
    outlineVariant = OutlineVariant,
    scrim = Color.Black,
)

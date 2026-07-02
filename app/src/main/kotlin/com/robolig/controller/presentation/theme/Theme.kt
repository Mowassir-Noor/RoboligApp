@file:Suppress("FunctionName")

package com.robolig.controller.presentation.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RoboligColorScheme =
    darkColorScheme(
        primary = Color(0xFFFF6B6B),
        onPrimary = Color(0xFF111111),
        primaryContainer = Color(0xFF422626),
        onPrimaryContainer = Color(0xFFFFDAD7),
        secondary = Color(0xFF6E6E6E),
        onSecondary = Color(0xFFF6F6F6),
        tertiary = Color(0xFF84D2A5),
        background = Color(0xFF111111),
        onBackground = Color(0xFFF4F4F4),
        surface = Color(0xFF1A1A1A),
        onSurface = Color(0xFFF4F4F4),
        surfaceVariant = Color(0xFF242424),
        onSurfaceVariant = Color(0xFFC8C8C8),
        outline = Color(0xFF3C3C3C),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF1A0D0D),
    )

private val RoboligTypography =
    Typography(
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                letterSpacing = 0.2.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                letterSpacing = 0.2.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                letterSpacing = 0.3.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                letterSpacing = 0.3.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                letterSpacing = 0.25.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                letterSpacing = 0.25.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
            ),
    )

private val RoboligShapes =
    Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    )

data class RoboligSpacing(
    val page: Dp,
    val section: Dp,
    val panel: Dp,
    val item: Dp,
    val compact: Dp,
    val railWidth: Dp,
    val telemetryHeight: Dp,
    val joystickSize: Dp,
)

private val LocalRoboligSpacing =
    staticCompositionLocalOf {
        RoboligSpacing(
            page = 18.dp,
            section = 16.dp,
            panel = 14.dp,
            item = 10.dp,
            compact = 6.dp,
            railWidth = 74.dp,
            telemetryHeight = 72.dp,
            joystickSize = 162.dp,
        )
    }

object RoboligTheme {
    val spacing: RoboligSpacing
        @Composable get() = LocalRoboligSpacing.current
}

@Composable
fun RoboligTheme(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val spacing =
        when {
            configuration.screenWidthDp >= 1600 ->
                scaledSpacing(1.25f)
            configuration.screenWidthDp >= 1280 ->
                scaledSpacing(1.1f)
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                configuration.screenWidthDp <= 960 ->
                RoboligSpacing(
                    page = 12.dp,
                    section = 12.dp,
                    panel = 12.dp,
                    item = 8.dp,
                    compact = 4.dp,
                    railWidth = 94.dp,
                    telemetryHeight = 64.dp,
                    joystickSize = 134.dp,
                )
            else ->
                scaledSpacing(1f)
        }

    CompositionLocalProvider(LocalRoboligSpacing provides spacing) {
        MaterialTheme(
            colorScheme = RoboligColorScheme,
            typography = RoboligTypography,
            shapes = RoboligShapes,
            content = content,
        )
    }
}

private fun scaledSpacing(scale: Float): RoboligSpacing =
    RoboligSpacing(
        page = 18.dp * scale,
        section = 16.dp * scale,
        panel = 14.dp * scale,
        item = 10.dp * scale,
        compact = 6.dp * scale,
        railWidth = 88.dp * scale,
        telemetryHeight = 72.dp * scale,
        joystickSize = 162.dp * scale,
    )

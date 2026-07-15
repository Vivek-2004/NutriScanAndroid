package com.health.nutriscan.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Premium Health & Wellness Palette
val NutriPrimary = Color(0xFF10B981) // Emerald Green
val NutriPrimaryDark = Color(0xFF059669)
val NutriPrimaryLight = Color(0xFFD1FAE5)
val NutriSecondary = Color(0xFF0D9488) // Deep Teal
val NutriSecondaryDark = Color(0xFF0F766E)

// UI Background & Surface Colors (Slate/Zinc system)
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val OutlineLight = Color(0xFFE2E8F0)

val BackgroundDark = Color(0xFF0B0F19)
val SurfaceDark = Color(0xFF151D30)
val OutlineDark = Color(0xFF222F4D)

// Risk Level Colors
val RiskHigh = Color(0xFFEF4444) // Bright Red
val RiskHighContainerLight = Color(0xFFFEE2E2)
val RiskHighContainerDark = Color(0xFF450A0A)

val RiskMedium = Color(0xFFF59E0B) // Amber Orange
val RiskMediumContainerLight = Color(0xFFFEF3C7)
val RiskMediumContainerDark = Color(0xFF451A03)

val RiskLow = Color(0xFF10B981) // Emerald Green
val RiskLowContainerLight = Color(0xFFD1FAE5)
val RiskLowContainerDark = Color(0xFF022C22)

// Premium Gradient Brushes for backgrounds, headers, and rings
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF0D9488))
)

val HighRiskGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFEF4444), Color(0xFFF43F5E))
)

val MediumRiskGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
)

val LowRiskGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF059669))
)

val NeutralGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF64748B), Color(0xFF475569))
)
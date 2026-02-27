package com.ventatickets.ventaticketstaxis.ui.theme

import androidx.compose.ui.unit.dp

object Dimensions {
    // Espaciado general
    val spacingSmall = 4.dp
    val spacingMedium = 8.dp
    val spacingLarge = 16.dp
    val spacingXLarge = 24.dp
    val spacingXXLarge = 32.dp
    
    // Padding de pantalla
    val screenPaddingHorizontal = 16.dp
    val screenPaddingVertical = 16.dp
    
    // Elevaciones
    val elevationSmall = 2.dp
    val elevationMedium = 4.dp
    val elevationLarge = 8.dp
    
    // Tamaños de botones
    val buttonHeight = 48.dp
    val buttonHeightLarge = 64.dp
    val buttonWidth = 200.dp
    
    // Tamaños de tarjetas
    val cardElevation = 4.dp
    val cardPadding = 16.dp
    
    // Tamaños de grid
    val gridSpacing = 8.dp
    
    // Breakpoints para diferentes tamaños de pantalla
    object Breakpoints {
        val small = 320.dp
        val medium = 480.dp
        val large = 720.dp
        val xlarge = 960.dp
    }
    
    // Número de columnas para grid según tamaño de pantalla
    fun getGridColumns(screenWidth: androidx.compose.ui.unit.Dp): Int {
        return when {
            screenWidth < Breakpoints.small -> 3
            screenWidth < Breakpoints.medium -> 4
            screenWidth < Breakpoints.large -> 5
            else -> 6
        }
    }
    
    // Ancho de navegación según tamaño de pantalla
    fun getNavigationWidth(screenWidth: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
        return when {
            screenWidth < Breakpoints.small -> 240.dp
            screenWidth < Breakpoints.medium -> 280.dp
            screenWidth < Breakpoints.large -> 320.dp
            else -> 360.dp
        }
    }
    
    // Ancho de botón responsive según tamaño de pantalla
    // Usa porcentaje del ancho de pantalla para mantener consistencia
    fun getButtonWidth(screenWidth: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
        return when {
            screenWidth < Breakpoints.small -> screenWidth * 0.85f  // 85% en pantallas muy pequeñas
            screenWidth < Breakpoints.medium -> screenWidth * 0.75f  // 75% en pantallas pequeñas
            screenWidth < Breakpoints.large -> 200.dp  // Fijo en pantallas medianas
            else -> 220.dp  // Fijo en pantallas grandes
        }
    }
    
    // Altura de botón responsive
    fun getButtonHeight(screenWidth: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
        return when {
            screenWidth < Breakpoints.small -> 44.dp  // Ligeramente más pequeño en pantallas pequeñas
            else -> buttonHeight  // 48dp estándar
        }
    }
    
    // Altura de botón grande responsive
    fun getButtonHeightLarge(screenWidth: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
        return when {
            screenWidth < Breakpoints.small -> 56.dp  // Más pequeño en pantallas pequeñas
            screenWidth < Breakpoints.medium -> 60.dp  // Intermedio en pantallas medianas
            else -> buttonHeightLarge  // 64dp estándar
        }
    }
    
    // Padding horizontal responsive para botones
    fun getButtonPaddingHorizontal(screenWidth: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
        return when {
            screenWidth < Breakpoints.small -> 12.dp
            screenWidth < Breakpoints.medium -> 16.dp
            else -> 24.dp
        }
    }
    
    // Ancho mínimo para botones dinámicos (basado en contenido)
    fun getButtonMinWidth(screenWidth: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
        return when {
            screenWidth < Breakpoints.small -> screenWidth * 0.70f  // 70% en pantallas muy pequeñas
            screenWidth < Breakpoints.medium -> 140.dp  // 140dp en pantallas pequeñas
            else -> 160.dp  // 160dp en pantallas medianas/grandes
        }
    }
    
    // Ancho máximo para botones dinámicos (basado en contenido)
    fun getButtonMaxWidth(screenWidth: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
        return when {
            screenWidth < Breakpoints.small -> screenWidth * 0.85f  // 85% en pantallas muy pequeñas
            screenWidth < Breakpoints.medium -> screenWidth * 0.75f  // 75% en pantallas pequeñas
            else -> 200.dp  // 200dp en pantallas medianas/grandes
        }
    }
} 
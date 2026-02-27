package com.ventatickets.ventaticketstaxis.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * LoadingIndicator de Material 3 - Componente personalizado basado en las especificaciones M3 Expressive
 * 
 * Este componente muestra un indicador de carga animado que cambia de forma y color.
 * Diseñado para reemplazar CircularProgressIndicator en la mayoría de casos.
 * 
 * @param modifier Modifier para aplicar al componente
 * @param indicatorColor Color del indicador activo
 * @param containerColor Color del contenedor (opcional, transparente por defecto)
 * @param indicatorSize Tamaño del indicador (38dp por defecto según Material 3)
 * @param containerWidth Ancho del contenedor (48dp por defecto)
 * @param containerHeight Alto del contenedor (48dp por defecto)
 * @param isContained Si es true, muestra el contenedor redondeado detrás del indicador
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = Color.Transparent,
    indicatorSize: Dp = 38.dp,
    containerWidth: Dp = 48.dp,
    containerHeight: Dp = 48.dp,
    isContained: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_indicator")
    
    // Animación de rotación
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Animación de morfología (cambio de forma)
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph"
    )
    
    // Animación de escala
    val scaleProgress by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(if (isContained) containerWidth else indicatorSize, if (isContained) containerHeight else indicatorSize)
            .clip(CircleShape),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        // Contenedor opcional
        if (isContained && containerColor != Color.Transparent) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawCircle(
                    color = containerColor,
                    radius = size.minDimension / 2f
                )
            }
        }
        
        // Indicador animado
        Canvas(
            modifier = Modifier
                .size(indicatorSize)
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = scaleProgress
                    scaleY = scaleProgress
                }
        ) {
            drawMorphingShape(
                progress = morphProgress,
                color = indicatorColor,
                size = size
            )
        }
    }
}

/**
 * Dibuja una forma que cambia entre círculo y otras formas según el progreso
 */
private fun DrawScope.drawMorphingShape(
    progress: Float,
    color: Color,
    size: Size
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f * 0.7f
    
    // Interpolación entre diferentes formas
    when {
        progress < 0.33f -> {
            // Forma 1: Círculo con puntos
            val phase = progress / 0.33f
            drawCircle(
                color = color.copy(alpha = 0.3f + phase * 0.7f),
                radius = radius,
                center = center
            )
            // Puntos alrededor del círculo
            for (i in 0 until 8) {
                val angle = (i * 45f + phase * 360f) * (Math.PI / 180f).toFloat()
                val pointRadius = radius * 0.3f
                val pointX = center.x + cos(angle) * radius * 0.7f
                val pointY = center.y + sin(angle) * radius * 0.7f
                drawCircle(
                    color = color.copy(alpha = 0.5f + phase * 0.5f),
                    radius = pointRadius,
                    center = Offset(pointX, pointY)
                )
            }
        }
        progress < 0.66f -> {
            // Forma 2: Círculo con arcos
            val phase = (progress - 0.33f) / 0.33f
            val arcCount = 4
            val arcAngle = 360f / arcCount
            
            for (i in 0 until arcCount) {
                val startAngle = i * arcAngle + phase * 360f
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = arcAngle * 0.6f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = radius * 0.2f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }
        }
        else -> {
            // Forma 3: Círculo completo con animación de pulso
            val phase = (progress - 0.66f) / 0.34f
            drawCircle(
                color = color.copy(alpha = 0.4f + phase * 0.6f),
                radius = radius * (0.7f + phase * 0.3f),
                center = center
            )
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = radius * 0.15f
                )
            )
        }
    }
}


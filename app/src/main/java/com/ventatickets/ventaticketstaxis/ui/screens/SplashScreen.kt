package com.ventatickets.ventaticketstaxis.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import com.ventatickets.ventaticketstaxis.R

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Animación de fade in
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alpha"
    )
    
    // Animación de escala con bounce
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutBack),
        label = "scale"
    )
    
    // Animación de rotación del círculo exterior
    val rotationAnim = animateFloatAsState(
        targetValue = if (startAnimation) 360f else 0f,
        animationSpec = tween(durationMillis = 3000, easing = EaseInOutCubic),
        label = "rotation"
    )
    
    // Animación de pulso infinito
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Animación de blur para efecto de profundidad
    val blurAnim = rememberInfiniteTransition(label = "blur")
    val blurRadius by blurAnim.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blur"
    )
    
    // Animación de fade in escalonado para textos
    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 600),
        label = "titleAlpha"
    )
    
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 800),
        label = "subtitleAlpha"
    )
    
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(400) // Duración total del splash
        onSplashComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Círculo principal animado con múltiples capas
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .scale(scaleAnim.value * pulseScale)
                    .alpha(alphaAnim.value),
                contentAlignment = Alignment.Center
            ) {
                // Círculo exterior con blur y rotación
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .rotate(rotationAnim.value)
                        .blur(Dp(blurRadius))
                )
                
                // Icono central con escala independiente
                Image(
                    painter = painterResource(id = R.drawable.splash_logo),
                    contentDescription = "Logo de la aplicación",
                    modifier = Modifier
                        .size(150.dp)
                        .scale(scaleAnim.value * 1.2f),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(50.dp))
            
            // Título principal con animación escalonada
            Text(
                text = "Venta de Tickets",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.alpha(titleAlpha)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subtítulo con animación escalonada
            Text(
                text = "Sistema de ventas",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.alpha(subtitleAlpha)
            )
            
            Spacer(modifier = Modifier.height(60.dp))
            
            // Indicador de carga con animación
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(40.dp)
                    .alpha(alphaAnim.value),
                strokeWidth = 4.dp
            )
        }
        
        // Texto de versión en la parte inferior
        Text(
            text = "Versión 1.0",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(alphaAnim.value)
        )
    }
} 
package com.ventatickets.ventaticketstaxis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.platform.LocalConfiguration
import com.ventatickets.ventaticketstaxis.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostInfoSection(
    cost: String,
    folio: String,
    zone: String,
    onFolioChange: (String) -> Unit,
    isLoadingFolio: Boolean = false,
    pullToRefreshState: PullToRefreshState? = null,
    isRefreshing: Boolean = false
) {
    // Detectar tamaño de pantalla para diseño compacto
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // Calcular valores responsive para diseño compacto
    val responsiveCardPadding = when {
        screenWidth < Dimensions.Breakpoints.small -> 8.dp
        screenWidth < Dimensions.Breakpoints.medium -> 12.dp
        else -> Dimensions.cardPadding
    }
    
    val responsiveVerticalPadding = when {
        screenWidth < Dimensions.Breakpoints.small -> 4.dp
        screenWidth < Dimensions.Breakpoints.medium -> 6.dp
        else -> Dimensions.spacingMedium
    }
    
    val responsiveSpacing = when {
        screenWidth < Dimensions.Breakpoints.small -> 8.dp
        screenWidth < Dimensions.Breakpoints.medium -> 10.dp
        else -> 12.dp
    }
    
    val responsiveCostPadding = when {
        screenWidth < Dimensions.Breakpoints.small -> 10.dp
        screenWidth < Dimensions.Breakpoints.medium -> 12.dp
        else -> 16.dp
    }
    
    val responsiveFolioPadding = when {
        screenWidth < Dimensions.Breakpoints.small -> 8.dp
        screenWidth < Dimensions.Breakpoints.medium -> 10.dp
        else -> 12.dp
    }
    
    val responsiveSpacerWidth = when {
        screenWidth < Dimensions.Breakpoints.small -> 8.dp
        screenWidth < Dimensions.Breakpoints.medium -> 12.dp
        else -> Dimensions.spacingLarge
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = responsiveVerticalPadding),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(Dimensions.cardElevation),
        tonalElevation = Dimensions.cardElevation,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .padding(responsiveCardPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(responsiveSpacing)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.large,
                tonalElevation = Dimensions.elevationSmall
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = responsiveCostPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mostrar LoadingIndicator cuando se está refrescando
                    if (isRefreshing && pullToRefreshState != null) {
                        val scaleFraction = if (isRefreshing) {
                            1f
                        } else {
                            LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
                        }
                        
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scaleFraction
                                    scaleY = scaleFraction
                                }
                                .padding(bottom = 8.dp)
                        ) {
                            LoadingIndicator(
                                indicatorColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                indicatorSize = 24.dp,
                                isContained = false
                            )
                        }
                    }
                    
                    Text(
                        text = "Costo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "$$cost.00",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Folio y Zona en fila horizontal centrada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = Dimensions.elevationSmall
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = responsiveFolioPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Folio",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        // Box con altura fija para mantener el espacio consistente en todas las resoluciones
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingFolio) {
                                LoadingIndicator(
                                    indicatorColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    indicatorSize = 20.dp,
                                    isContained = false
                                )
                            } else {
                                Text(
                                    text = folio,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(responsiveSpacerWidth))

                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = Dimensions.elevationSmall
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = responsiveFolioPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Zona",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = zone,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
} 
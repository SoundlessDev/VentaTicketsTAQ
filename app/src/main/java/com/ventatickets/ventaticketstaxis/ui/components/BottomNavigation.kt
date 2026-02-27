package com.ventatickets.ventaticketstaxis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ventatickets.ventaticketstaxis.ui.theme.Dimensions

@Composable
fun BottomNavigation(
    currentSection: Int,
    onSectionSelected: (Int) -> Unit
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Dimensions.elevationSmall),
            tonalElevation = Dimensions.elevationSmall
        ) {
            NavigationBarItem(
                selected = currentSection == 0,
                onClick = { onSectionSelected(0) },
                icon = { 
                    Icon(
                        Icons.Default.Place, 
                    contentDescription = "Por Zonas"
                    ) 
                },
                label = { 
                Text("Por Zonas")
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            NavigationBarItem(
                selected = currentSection == 1,
                onClick = { onSectionSelected(1) },
                icon = { 
                    Icon(
                        Icons.Default.Search, 
                    contentDescription = "Por Destinos"
                    ) 
                },
                label = { 
                Text("Por Destinos")
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
    }
} 
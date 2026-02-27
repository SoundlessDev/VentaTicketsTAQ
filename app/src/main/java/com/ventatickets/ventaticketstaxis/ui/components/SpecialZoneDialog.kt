package com.ventatickets.ventaticketstaxis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun SpecialZoneDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var destination by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Zona Especial S",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destino") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Ingrese el destino") }
                )

                OutlinedTextField(
                    value = cost,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*$"))) {
                            cost = it
                        }
                    },
                    label = { Text("Costo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Ingrese el costo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            ElevatedButton(
                onClick = {
                    if (destination.isNotEmpty() && cost.isNotEmpty()) {
                        onConfirm(destination, cost)
                        onDismiss()
                    }
                },
                enabled = destination.isNotEmpty() && cost.isNotEmpty()
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            ElevatedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
} 
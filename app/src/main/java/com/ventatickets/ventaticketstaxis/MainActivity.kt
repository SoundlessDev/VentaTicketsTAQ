package com.ventatickets.ventaticketstaxis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ventatickets.ventaticketstaxis.alarms.AlarmManagerHelper
import com.ventatickets.ventaticketstaxis.notifications.NotificationHelper
import com.ventatickets.ventaticketstaxis.ui.theme.VentaTicketsTaxisTheme
import com.ventatickets.ventaticketstaxis.ui.screens.TicketSalesScreen
import com.ventatickets.ventaticketstaxis.ui.screens.LoginScreen
import com.ventatickets.ventaticketstaxis.ui.screens.SplashScreen
import com.ventatickets.ventaticketstaxis.ui.viewmodels.LoginViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crear canal de notificación al iniciar la aplicación
        NotificationHelper.createNotificationChannel(this)

        setContent {
            VentaTicketsTaxisTheme {
                val context = LocalContext.current

                // Launcher para solicitar permiso de notificaciones (Android 13+)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        android.util.Log.d("MainActivity", "Permiso de notificaciones concedido")
                    } else {
                        android.util.Log.w("MainActivity", "Permiso de notificaciones denegado")
                    }
                }

                // Solicitar permisos necesarios al iniciar la app
                LaunchedEffect(Unit) {
                    // Solicitar permiso de notificaciones (Android 13+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasNotificationPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasNotificationPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Verificar permiso para alarmas exactas (Android 12+)
                    // Nota: En Android 12+, el permiso se otorga automáticamente pero puede ser revocado
                    // Si es necesario, se debe abrir la configuración del sistema
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (!AlarmManagerHelper.canScheduleExactAlarms(context)) {
                            android.util.Log.w("MainActivity", "⚠️ No se pueden programar alarmas exactas. El usuario debe habilitarlas en configuración.")
                            // Podríamos mostrar un diálogo informativo aquí si es necesario
                            // Por ahora solo lo registramos en los logs
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val loginViewModel: LoginViewModel = viewModel()
                    var showSplash by remember { mutableStateOf(true) }
                    var showMainScreen by remember { mutableStateOf(false) }

                    // Observar el estado de login
                    val loginState by loginViewModel.loginState.collectAsState()
                    LaunchedEffect(loginState) {
                        showMainScreen = loginState is LoginViewModel.LoginState.Success
                    }

                    when {
                        showSplash -> {
                            SplashScreen(
                                onSplashComplete = {
                                    showSplash = false
                                }
                            )
                        }
                        showMainScreen -> {
                            TicketSalesScreen(
                                onLogout = {
                                    showMainScreen = false
                                },
                                loginViewModel = loginViewModel
                            )
                        }
                        else -> {
                            LoginScreen(
                                onLoginSuccess = { showMainScreen = true },
                                loginViewModel = loginViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
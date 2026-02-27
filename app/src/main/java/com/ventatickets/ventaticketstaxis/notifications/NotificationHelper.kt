package com.ventatickets.ventaticketstaxis.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ventatickets.ventaticketstaxis.MainActivity
import com.ventatickets.ventaticketstaxis.R

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ID = "tarifas_channel"
    private const val CHANNEL_NAME = "Cambios de Tarifas"
    private const val CHANNEL_DESCRIPTION = "Notificaciones sobre cambios en las tarifas"
    
    private const val NOTIFICATION_ID_NOCTURNO = 1001
    private const val NOTIFICATION_ID_DIURNO = 1002
    
    /**
     * Crea el canal de notificación (requerido para Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH para que se muestre incluso en segundo plano
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true) // Mostrar badge en el icono
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC // Visible en pantalla bloqueada
            }
            
            val notificationManager = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "Canal de notificación creado: $CHANNEL_ID con importancia HIGH")
        }
    }
    
    /**
     * Muestra notificación de cambio a tarifas nocturnas
     */
    fun showNocturnoNotification(context: Context) {
        val title = "🌙 Tarifas Nocturnas Activas"
        val message = "Las tarifas han cambiado a precios nocturnos"
        
        Log.d(TAG, "=== Intentando mostrar notificación de inicio de tarifas nocturnas ===")
        showNotification(context, title, message, NOTIFICATION_ID_NOCTURNO, true)
    }
    
    /**
     * Muestra notificación de cambio a tarifas diurnas
     */
    fun showDiurnoNotification(context: Context) {
        val title = "☀️ Terminan las Tarifas Nocturnas"
        val message = "Las tarifas han vuelto a precios diurnos"
        
        Log.d(TAG, "=== Intentando mostrar notificación de fin de tarifas nocturnas ===")
        showNotification(context, title, message, NOTIFICATION_ID_DIURNO, false)
    }
    
    /**
     * Muestra una notificación genérica
     */
    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        isNocturno: Boolean
    ) {
        try {
            Log.d(TAG, "showNotification llamado - Título: $title, Mensaje: $message, ID: $notificationId")
            
            // Usar applicationContext para asegurar que funcione en segundo plano
            val appContext = context.applicationContext
            
            // Crear canal de notificación si no existe (por si acaso)
            createNotificationChannel(appContext)
            
            // Crear intent para abrir la app
            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                notificationId, // Usar notificationId único para cada notificación
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Obtener el icono de notificación
            // Prioridad: 1) ic_stat_name (PNG en carpetas de densidad), 2) ic_notification_taxi (vector), 3) icono del sistema
            val iconResourceId = getNotificationIcon(appContext)
            
            Log.d(TAG, "📌 Icono final que se usará para la notificación: ID $iconResourceId")
            
            // Construir la notificación según documentación oficial de Android
            // https://developer.android.com/develop/ui/views/notifications/build-notification
            val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(iconResourceId) // Icono pequeño (monocromático blanco) - requerido
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Para Android 7.1 y anteriores
                .setAutoCancel(true) // Se cancela automáticamente al tocar
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 500, 200, 500)) // Patrón de vibración
                .setLights(0xFF0000FF.toInt(), 1000, 1000) // Luz LED azul
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Sonido, vibración y luz por defecto
            
            // Verificar permisos y estado de notificaciones
            val notificationManager = NotificationManagerCompat.from(appContext)
            val areEnabled = areNotificationsEnabled(appContext)
            
            Log.d(TAG, "Estado de notificaciones - Habilitadas: $areEnabled, SDK: ${Build.VERSION.SDK_INT}")
            
            // Mostrar la notificación
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Para Android 13+ necesitamos verificar permisos
                if (areEnabled) {
                    try {
                        notificationManager.notify(notificationId, builder.build())
                        Log.d(TAG, "✅ Notificación mostrada correctamente: $title (ID: $notificationId)")
                    } catch (e: SecurityException) {
                        Log.e(TAG, "❌ Error de seguridad al mostrar notificación: ${e.message}", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error inesperado al mostrar notificación: ${e.message}", e)
                    }
                } else {
                    Log.w(TAG, "⚠️ Notificaciones deshabilitadas por el usuario o permisos no concedidos")
                }
            } else {
                // Para versiones anteriores a Android 13
                if (areEnabled) {
                    try {
                        notificationManager.notify(notificationId, builder.build())
                        Log.d(TAG, "✅ Notificación mostrada correctamente: $title (ID: $notificationId)")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error al mostrar notificación: ${e.message}", e)
                    }
                } else {
                    Log.w(TAG, "⚠️ Notificaciones deshabilitadas por el usuario")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error crítico mostrando notificación: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Cancela todas las notificaciones de tarifas
     */
    fun cancelAllNotifications(context: Context) {
        try {
            with(NotificationManagerCompat.from(context)) {
                cancel(NOTIFICATION_ID_NOCTURNO)
                cancel(NOTIFICATION_ID_DIURNO)
            }
            Log.d(TAG, "Todas las notificaciones de tarifas canceladas")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelando notificaciones", e)
        }
    }
    
    /**
     * Verifica si las notificaciones están habilitadas
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando permisos de notificación", e)
            false
        }
    }
    
    /**
     * Obtiene el icono de notificación con sistema de fallback
     * Prioridad: 1) ic_stat_name (PNG), 2) ic_notification_taxi (vector), 3) icono del sistema
     */
    private fun getNotificationIcon(context: Context): Int {
        // Intentar primero con ic_stat_name (iconos PNG en carpetas de densidad)
        val iconCandidates = listOf(
            Pair(R.drawable.ic_stat_name, "ic_stat_name (PNG)"),
            Pair(R.drawable.ic_notification_taxi, "ic_notification_taxi (Vector)")
        )
        
        for ((resourceId, description) in iconCandidates) {
            try {
                val drawable = context.getDrawable(resourceId)
                if (drawable != null) {
                    val drawableWidth = drawable.intrinsicWidth
                    val drawableHeight = drawable.intrinsicHeight
                    Log.d(TAG, "✅ Icono de notificación encontrado: $description")
                    Log.d(TAG, "   - ID del recurso: $resourceId")
                    Log.d(TAG, "   - Dimensiones: ${drawableWidth}x${drawableHeight}")
                    Log.d(TAG, "   - Tipo: ${drawable::class.java.simpleName}")
                    return resourceId
                } else {
                    Log.w(TAG, "⚠️ $description retornó null, intentando siguiente opción")
                }
            } catch (e: Resources.NotFoundException) {
                Log.w(TAG, "⚠️ $description no encontrado: ${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error obteniendo $description: ${e.message}")
            }
        }
        
        // Si ninguno funcionó, usar icono del sistema como último recurso
        Log.e(TAG, "❌ Ningún icono personalizado disponible, usando icono del sistema")
        return android.R.drawable.ic_dialog_info
    }
} 
package com.ventatickets.ventaticketstaxis.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ventatickets.ventaticketstaxis.data.EmpresaGlobals
import com.ventatickets.ventaticketstaxis.notifications.NotificationHelper

/**
 * BroadcastReceiver que se ejecuta cuando se dispara una alarma programada
 * para el cambio de tarifas nocturnas/diurnas.
 * 
 * Este receiver funciona incluso cuando la app está completamente cerrada.
 */
class TarifasAlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "TarifasAlarmReceiver"
        const val ACTION_INICIO_NOCTURNO = "com.ventatickets.ventaticketstaxis.INICIO_NOCTURNO"
        const val ACTION_FIN_NOCTURNO = "com.ventatickets.ventaticketstaxis.FIN_NOCTURNO"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "=== Alarma recibida: ${intent.action} ===")
        
        // Usar goAsync() para procesar en un hilo separado y no bloquear el sistema
        val pendingResult = goAsync()
        
        try {
            // Inicializar EmpresaGlobals para asegurar que los datos estén disponibles
            EmpresaGlobals.initialize(context)
            
            // Crear canal de notificación si no existe
            NotificationHelper.createNotificationChannel(context)
            
            when (intent.action) {
                ACTION_INICIO_NOCTURNO -> {
                    Log.d(TAG, "Procesando inicio de horario nocturno")
                    handleInicioNocturno(context)
                }
                ACTION_FIN_NOCTURNO -> {
                    Log.d(TAG, "Procesando fin de horario nocturno")
                    handleFinNocturno(context)
                }
                else -> {
                    Log.w(TAG, "Acción desconocida: ${intent.action}")
                }
            }

            // Reprogramar las siguientes alarmas (operación rápida)
            AlarmManagerHelper.scheduleAlarms(context)
            
            // Marcar como completado
            pendingResult.finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando alarma: ${e.message}", e)
            pendingResult.finish()
        }
    }
    
    private fun handleInicioNocturno(context: Context) {
        try {
            Log.d(TAG, "=== Iniciando proceso de cambio a tarifas nocturnas ===")
            
            // Mostrar notificación
            NotificationHelper.showNocturnoNotification(context)
            
            // Actualizar estado nocturno en memoria para aplicar tarifas locales
            EmpresaGlobals.setNocturnoActive(true)
            
            Log.d(TAG, "✅ Proceso de inicio nocturno completado")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en handleInicioNocturno: ${e.message}", e)
        }
    }
    
    private fun handleFinNocturno(context: Context) {
        try {
            Log.d(TAG, "=== Iniciando proceso de cambio a tarifas diurnas ===")
            
            // Mostrar notificación
            NotificationHelper.showDiurnoNotification(context)
            
            // Actualizar estado nocturno para volver a tarifas diurnas
            EmpresaGlobals.setNocturnoActive(false)
            
            Log.d(TAG, "✅ Proceso de fin nocturno completado")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en handleFinNocturno: ${e.message}", e)
        }
    }
}


package com.ventatickets.ventaticketstaxis.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ventatickets.ventaticketstaxis.data.EmpresaGlobals
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper para gestionar alarmas exactas usando AlarmManager.
 * 
 * AlarmManager con setExactAndAllowWhileIdle() es la forma más confiable
 * de ejecutar código a una hora exacta incluso cuando la app está cerrada.
 */
object AlarmManagerHelper {
    
    private const val TAG = "AlarmManagerHelper"
    private const val REQUEST_CODE_INICIO = 1001
    private const val REQUEST_CODE_FIN = 1002
    
    /**
     * Programa las alarmas para inicio y fin de horario nocturno
     */
    fun scheduleAlarms(context: Context) {
        try {
            Log.d(TAG, "=== Programando alarmas para cambios de tarifas ===")
            
            // Verificar si los datos de empresa están disponibles
            if (!EmpresaGlobals.isDataLoaded()) {
                Log.d(TAG, "Datos de empresa no disponibles, no se pueden programar alarmas")
                return
            }
            
            val iniNocturno = EmpresaGlobals.ini_nocturno
            val finNocturno = EmpresaGlobals.fin_nocturno
            
            if (iniNocturno.isNullOrEmpty() || finNocturno.isNullOrEmpty()) {
                Log.d(TAG, "Horarios nocturnos no configurados")
                return
            }
            
            // Cancelar alarmas existentes primero
            cancelAlarms(context)
            
            // Programar alarma para inicio nocturno
            scheduleAlarmForTime(
                context = context,
                timeString = iniNocturno,
                action = TarifasAlarmReceiver.ACTION_INICIO_NOCTURNO,
                requestCode = REQUEST_CODE_INICIO,
                tag = "inicio_nocturno"
            )
            
            // Programar alarma para fin nocturno
            scheduleAlarmForTime(
                context = context,
                timeString = finNocturno,
                action = TarifasAlarmReceiver.ACTION_FIN_NOCTURNO,
                requestCode = REQUEST_CODE_FIN,
                tag = "fin_nocturno"
            )
            
            Log.d(TAG, "✅ Alarmas programadas correctamente: $iniNocturno - $finNocturno")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error programando alarmas: ${e.message}", e)
        }
    }
    
    /**
     * Programa una alarma para una hora específica
     */
    private fun scheduleAlarmForTime(
        context: Context,
        timeString: String,
        action: String,
        requestCode: Int,
        tag: String
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Parsear la hora objetivo
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val targetTime = timeFormat.parse(timeString) ?: run {
                Log.e(TAG, "Error parseando hora: $timeString")
                return
            }
            
            // Calcular el tiempo objetivo en el calendario
            val calendar = Calendar.getInstance()
            val now = Calendar.getInstance()
            
            val targetCalendar = Calendar.getInstance()
            targetCalendar.time = targetTime
            val targetHour = targetCalendar.get(Calendar.HOUR_OF_DAY)
            val targetMinute = targetCalendar.get(Calendar.MINUTE)
            
            // Configurar la hora objetivo para hoy
            calendar.set(Calendar.HOUR_OF_DAY, targetHour)
            calendar.set(Calendar.MINUTE, targetMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            // Si la hora ya pasó hoy, programar para mañana
            if (calendar.before(now) || calendar.timeInMillis == now.timeInMillis) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            val triggerAtMillis = calendar.timeInMillis
            val delayMinutes = (triggerAtMillis - now.timeInMillis) / (1000 * 60)
            
            Log.d(TAG, "Programando alarma para $tag a las $timeString (en ${delayMinutes} minutos)")
            Log.d(TAG, "Fecha/hora objetivo: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(triggerAtMillis))}")
            
            // Crear Intent para el BroadcastReceiver
            val intent = Intent(context, TarifasAlarmReceiver::class.java).apply {
                setAction(action)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Programar la alarma usando el método más confiable disponible
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+ (API 23+): setExactAndAllowWhileIdle es el más confiable
                // Funciona incluso cuando el dispositivo está en modo Doze
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Alarma programada con setExactAndAllowWhileIdle para $tag")
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Android 4.4+ (API 19+): setExact
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Alarma programada con setExact para $tag")
            } else {
                // Android anterior: set (menos preciso, pero funciona)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Alarma programada con set para $tag")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error programando alarma para $tag: ${e.message}", e)
        }
    }
    
    /**
     * Cancela todas las alarmas programadas
     */
    fun cancelAlarms(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Cancelar alarma de inicio nocturno
            val intentInicio = Intent(context, TarifasAlarmReceiver::class.java).apply {
                setAction(TarifasAlarmReceiver.ACTION_INICIO_NOCTURNO)
            }
            val pendingIntentInicio = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_INICIO,
                intentInicio,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntentInicio)
            
            // Cancelar alarma de fin nocturno
            val intentFin = Intent(context, TarifasAlarmReceiver::class.java).apply {
                setAction(TarifasAlarmReceiver.ACTION_FIN_NOCTURNO)
            }
            val pendingIntentFin = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_FIN,
                intentFin,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntentFin)
            
            Log.d(TAG, "Alarmas canceladas")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelando alarmas: ${e.message}", e)
        }
    }
    
    /**
     * Verifica si se puede programar alarmas exactas (Android 12+ requiere permiso)
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // En versiones anteriores no se requiere permiso
        }
    }
}


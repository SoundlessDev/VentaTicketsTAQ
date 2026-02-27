package com.ventatickets.ventaticketstaxis.data

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object NocturnoHelper {
    private const val TAG = "NocturnoHelper"
    
    /**
     * Verifica si la hora actual está dentro del período nocturno
     */
    fun isHorarioNocturno(iniNocturno: String?, finNocturno: String?): Boolean {
        if (iniNocturno.isNullOrEmpty() || finNocturno.isNullOrEmpty()) {
            Log.d(TAG, "Horarios nocturnos no configurados")
            return false
        }
        
        try {
            val currentTime = Calendar.getInstance()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentTimeString = timeFormat.format(currentTime.time)
            
            return isInNocturnoPeriod(currentTimeString, iniNocturno, finNocturno)
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando horario nocturno", e)
            return false
        }
    }
    
    /**
     * Verifica si una hora específica está dentro del período nocturno
     */
    fun isInNocturnoPeriod(currentTime: String, iniNocturno: String, finNocturno: String): Boolean {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val current = timeFormat.parse(currentTime) ?: return false
            val inicio = timeFormat.parse(iniNocturno) ?: return false
            val fin = timeFormat.parse(finNocturno) ?: return false
            
            // Caso especial: horario nocturno cruza la medianoche
            if (inicio.after(fin)) {
                // Ejemplo: 22:00 - 06:00 (noche a mañana)
                return current.after(inicio) || current.before(fin)
            } else {
                // Ejemplo: 06:00 - 22:00 (mañana a noche)
                return current.after(inicio) && current.before(fin)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando horarios", e)
            return false
        }
    }
    
    /**
     * Obtiene la hora actual en formato HH:mm
     */
    fun getCurrentTime(): String {
        val currentTime = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return timeFormat.format(currentTime.time)
    }
    
    /**
     * Obtiene información del estado nocturno actual
     */
    fun getNocturnoInfo(): String {
        val iniNocturno = EmpresaGlobals.ini_nocturno
        val finNocturno = EmpresaGlobals.fin_nocturno
        val currentTime = getCurrentTime()
        val isNocturno = isHorarioNocturno(iniNocturno, finNocturno)
        
        return "Hora actual: $currentTime, Horario nocturno: $iniNocturno-$finNocturno, ¿Es nocturno? $isNocturno"
    }
} 
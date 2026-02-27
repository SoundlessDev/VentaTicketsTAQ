package com.ventatickets.ventaticketstaxis.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.ventatickets.ventaticketstaxis.alarms.AlarmManagerHelper
import com.ventatickets.ventaticketstaxis.data.EmpresaGlobals
import com.ventatickets.ventaticketstaxis.data.NocturnoHelper
import com.ventatickets.ventaticketstaxis.notifications.NotificationHelper
import com.ventatickets.ventaticketstaxis.ui.viewmodels.ZoneViewModel
import com.ventatickets.ventaticketstaxis.ui.viewmodels.DestinationViewModel
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DataRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "DataRefreshWorker"
        const val WORK_NAME = "data_refresh_worker"
        
        // Función para programar alarmas basadas en horarios nocturnos
        // Ahora usa AlarmManager en lugar de WorkManager para mayor confiabilidad
        fun scheduleDataRefresh(context: Context) {
            Log.d(TAG, "Programando alarmas para cambios de tarifas usando AlarmManager")
            
            // Usar AlarmManager que es más confiable para notificaciones exactas
            AlarmManagerHelper.scheduleAlarms(context)
        }
        
        // Función para reprogramar alarmas (llamar cuando cambien los horarios)
        fun rescheduleDataRefresh(context: Context) {
            Log.d(TAG, "Reprogramando alarmas de refresco")
            scheduleDataRefresh(context)
        }
        
        // Función para cancelar las alarmas
        fun cancelDataRefresh(context: Context) {
            AlarmManagerHelper.cancelAlarms(context)
            Log.d(TAG, "Alarmas de refresco canceladas")
        }
        
        /**
         * Refresca los datos de forma asíncrona usando un worker
         * Se llama desde el BroadcastReceiver cuando se dispara una alarma
         */
        fun refreshDataAsync(context: Context) {
            try {
                Log.d(TAG, "Iniciando refresco asíncrono de datos")
                
                // Crear un worker de una sola vez para refrescar datos
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // Requerir conexión para refrescar datos
                    .build()
                
                val refreshWorkRequest = OneTimeWorkRequestBuilder<DataRefreshWorker>()
                    .setConstraints(constraints)
                    .addTag("${WORK_NAME}_refresh")
                    .build()
                
                WorkManager.getInstance(context).enqueue(refreshWorkRequest)
                
                Log.d(TAG, "Worker de refresco de datos encolado")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar refresco asíncrono: ${e.message}", e)
            }
        }
        
        // Función para verificar estado de alarmas
        fun checkActiveWorkers(context: Context) {
            val canSchedule = AlarmManagerHelper.canScheduleExactAlarms(context)
            Log.d(TAG, "¿Puede programar alarmas exactas? $canSchedule")
            
            if (!canSchedule) {
                Log.w(TAG, "⚠️ No se pueden programar alarmas exactas. El usuario debe otorgar permiso en configuración.")
            }
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "=== DataRefreshWorker ejecutado - Solo refrescando datos ===")
        
        try {
            // Este worker ahora solo se usa para refrescar datos de forma asíncrona
            // Las notificaciones se manejan mediante AlarmManager y BroadcastReceiver
            
            // Verificar si los datos de empresa están cargados
            if (!EmpresaGlobals.isDataLoaded()) {
                Log.d(TAG, "Datos de empresa no disponibles, saltando refresco")
                return Result.success()
            }
            
            // Refrescar datos
            Log.d(TAG, "Refrescando datos de zonas y destinos")
            refreshData()
            
            return Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en DataRefreshWorker: ${e.message}", e)
            // No retry automático para evitar loops infinitos
            return Result.success()
        }
    }
    
    private suspend fun refreshData() {
        try {
            // Obtener instancias de ViewModels
            val zoneViewModel = ZoneViewModel.getInstance(applicationContext)
            val destinationViewModel = DestinationViewModel.getInstance(applicationContext)
            
            // Verificar si pueden cargar desde servidor
            if (zoneViewModel.canLoadFromServer() && destinationViewModel.canLoadFromServer()) {
                Log.d(TAG, "Refrescando zonas y destinos desde servidor")
                
                // Cargar datos en paralelo usando coroutines
                coroutineScope {
                    val zonesJob = async {
                        zoneViewModel.loadZonesFromWebService()
                    }
                    
                    val destinationsJob = async {
                        destinationViewModel.loadDestinationsFromWebService()
                    }
                    
                    // Esperar a que ambos terminen
                    zonesJob.await()
                    destinationsJob.await()
                }
                
                Log.d(TAG, "Datos refrescados exitosamente")
            } else {
                Log.w(TAG, "No se puede cargar desde servidor - configuración incompleta")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error refrescando datos", e)
        }
    }
} 
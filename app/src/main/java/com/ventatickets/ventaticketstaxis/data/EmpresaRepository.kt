package com.ventatickets.ventaticketstaxis.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmpresaRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "EmpresaRepository"
    }
    
    suspend fun getEmpresaData(): List<EmpresaData> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Obteniendo datos de la empresa...")
                val apiService = ServiceLocator.getApiService(context)
                val response = apiService.getEmpresaData()
                Log.d(TAG, "Datos de empresa obtenidos: ${response.size} registros")
                response
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener datos de empresa", e)
                emptyList()
            }
        }
    }
} 
package com.ventatickets.ventaticketstaxis.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class FolioRepository(private val context: Context) {
    
    private fun getApiService() = ServiceLocator.getApiService(context)
    
    private fun getConfigValues(): Pair<String, String> {
        val configTaquilla = AppConfig.getTaquilla(context)
        val configZona = AppConfig.getZona(context)
        return Pair(configTaquilla, configZona)
    }
    
    fun getFolio(zonaSelect: String): Flow<Result<String>> = flow {
        try {
            val (configTaquilla, configZona) = getConfigValues()
            android.util.Log.d("FolioRepository", "Solicitando folio para zona: $zonaSelect, configTaquilla: $configTaquilla, configZona: $configZona")
            
            val request = FolioRequest(
                zonaSelect = zonaSelect,
                configZona = configZona,
                configTaquilla = configTaquilla
            )
            
            android.util.Log.d("FolioRepository", "Request: $request")
            
            val response = getApiService().getFolio(request)
            
            android.util.Log.d("FolioRepository", "Respuesta recibida: ${response.folio}")
            
            if (response.folio == "Error-NoExiste") {
                emit(Result.failure(Exception("Configura la aplicación, esta taquilla no existe en el sistema")))
            } else {
                emit(Result.success(response.folio))
            }
            
        } catch (e: SocketTimeoutException) {
            // Error de timeout - falla rápido cuando no hay conexión (3-5 segundos)
            android.util.Log.e("FolioRepository", "Error de timeout: ${e.message}")
            emit(Result.failure(Exception("Error de conexión: No se puede conectar al servidor. Verifica tu conexión a internet.")))
        } catch (e: UnknownHostException) {
            // Error de host desconocido - falla inmediatamente cuando no hay DNS o conexión
            android.util.Log.e("FolioRepository", "Error de host desconocido: ${e.message}")
            emit(Result.failure(Exception("Error de conexión: No se puede conectar al servidor. Verifica tu conexión a internet.")))
        } catch (e: IOException) {
            // Error de IO - falla rápido cuando no hay conexión de red
            android.util.Log.e("FolioRepository", "Error de IO: ${e.message}")
            val errorMessage = when {
                e.message?.contains("timeout", ignoreCase = true) == true -> 
                    "Error de conexión: Tiempo de espera agotado. Verifica tu conexión a internet."
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> 
                    "Error de conexión: No se puede resolver el servidor. Verifica la configuración."
                else -> 
                    "Error de conexión: No se pudo establecer la conexión al servidor. Verifica tu conexión a internet."
            }
            emit(Result.failure(Exception(errorMessage)))
        } catch (e: HttpException) {
            // Error HTTP - el servidor respondió pero con error
            android.util.Log.e("FolioRepository", "Error HTTP: ${e.code()} - ${e.message()}")
            val errorMessage = when (e.code()) {
                404 -> "Error del servidor: Endpoint no encontrado. Verifica la configuración."
                500, 502, 503 -> "Error del servidor: El servidor no está disponible temporalmente."
                else -> "Error del servidor: ${e.code()}. No se puede comunicar con el servidor."
            }
            emit(Result.failure(Exception(errorMessage)))
        } catch (e: Exception) {
            // Error inesperado
            android.util.Log.e("FolioRepository", "Error inesperado: ${e.message}", e)
            emit(Result.failure(Exception("Error inesperado: ${e.message ?: "Revisa la configuración de la aplicación."}")))
        }
    }
} 
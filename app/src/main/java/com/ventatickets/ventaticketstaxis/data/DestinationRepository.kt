package com.ventatickets.ventaticketstaxis.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DestinationRepository(private val context: Context) {
    
    private fun getApiService() = ServiceLocator.getApiService(context)
    
    fun getDropdownZonaS(): Flow<Result<List<Destination>>> = flow {
        try {
            android.util.Log.d("DestinationRepository", "Solicitando dropdown zona S")
            val destinations: List<Destination> = getApiService().getDropdownZonaS()
            android.util.Log.d("DestinationRepository", "Destinos zona S recibidos: ${destinations.size}")
            emit(Result.success(destinations))
        } catch (e: HttpException) {
            android.util.Log.e("DestinationRepository", "Error HTTP: ${e.code()} - ${e.message()}")
            emit(Result.failure(Exception("Error del servidor: ${e.code()}")))
        } catch (e: SocketTimeoutException) {
            android.util.Log.e("DestinationRepository", "Error de timeout: ${e.message}")
            emit(Result.failure(Exception("Error de conexión: Tiempo de espera agotado")))
        } catch (e: UnknownHostException) {
            android.util.Log.e("DestinationRepository", "Error de host desconocido: ${e.message}")
            emit(Result.failure(Exception("Error de conexión: No se puede conectar al servidor")))
        } catch (e: IOException) {
            android.util.Log.e("DestinationRepository", "Error de IO: ${e.message}")
            emit(Result.failure(Exception("Error de conexión: ${e.message}")))
        } catch (e: Exception) {
            android.util.Log.e("DestinationRepository", "Error inesperado: ${e.message}")
            emit(Result.failure(Exception("Error inesperado: ${e.message}")))
        }
    }
    
    fun getDropdownZonaDifS(): Flow<Result<List<Destination>>> = flow {
        try {
            android.util.Log.d("DestinationRepository", "Solicitando dropdown zona dif S")
            val destinations: List<Destination> = getApiService().getDropdownZonaDifS()
            android.util.Log.d("DestinationRepository", "Destinos zona dif S recibidos: ${destinations.size}")
            emit(Result.success(destinations))
        } catch (e: HttpException) {
            android.util.Log.e("DestinationRepository", "Error HTTP: ${e.code()} - ${e.message()}")
            emit(Result.failure(Exception("Error del servidor: ${e.code()}")))
        } catch (e: SocketTimeoutException) {
            android.util.Log.e("DestinationRepository", "Error de timeout: ${e.message}")
            emit(Result.failure(Exception("Error de conexión: Tiempo de espera agotado")))
        } catch (e: UnknownHostException) {
            android.util.Log.e("DestinationRepository", "Error de host desconocido: ${e.message}")
            emit(Result.failure(Exception("Error de conexión: No se puede conectar al servidor")))
        } catch (e: IOException) {
            android.util.Log.e("DestinationRepository", "Error de IO: ${e.message}")
            emit(Result.failure(Exception("Error de conexión: ${e.message}")))
        } catch (e: Exception) {
            android.util.Log.e("DestinationRepository", "Error inesperado: ${e.message}")
            emit(Result.failure(Exception("Error inesperado: ${e.message}")))
        }
    }
} 
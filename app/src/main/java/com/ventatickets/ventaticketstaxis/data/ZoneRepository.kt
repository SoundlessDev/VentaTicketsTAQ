package com.ventatickets.ventaticketstaxis.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ZoneRepository(private val context: Context) {
    private fun getApiService() = ServiceLocator.getApiService(context)

    init {
        Log.d("ZoneRepository", "Inicializando ZoneRepository")
    }

    suspend fun getZones(): List<Zone> {
        return try {
            Log.d("ZoneRepository", "Solicitando zonas del servidor")
            val zones: List<Zone> = getApiService().getZones()
            Log.d("ZoneRepository", "Zonas recibidas: ${zones.size}")
            zones
        } catch (e: HttpException) {
            Log.e("ZoneRepository", "Error HTTP: ${e.code()} - ${e.message()}")
            throw Exception("Error del servidor: ${e.code()}")
        } catch (e: SocketTimeoutException) {
            Log.e("ZoneRepository", "Error de timeout: ${e.message}")
            throw Exception("Error de conexión: Tiempo de espera agotado")
        } catch (e: UnknownHostException) {
            Log.e("ZoneRepository", "Error de host desconocido: ${e.message}")
            throw Exception("Error de conexión: No se puede conectar al servidor")
        } catch (e: IOException) {
            Log.e("ZoneRepository", "Error de IO: ${e.message}")
            throw Exception("Error de conexión: ${e.message}")
        } catch (e: Exception) {
            Log.e("ZoneRepository", "Error inesperado: ${e.message}")
            throw Exception("Error inesperado: ${e.message}")
        }
    }

    suspend fun getDropdownZonaS(): List<Destination> {
        return try {
            getApiService().getDropdownZonaS()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDropdownZonaDifS(): List<Destination> {
        return try {
            getApiService().getDropdownZonaDifS()
        } catch (e: Exception) {
            emptyList()
        }
    }
} 
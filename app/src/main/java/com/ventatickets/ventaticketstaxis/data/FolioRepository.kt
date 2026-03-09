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

        val maxRetries = 3
        var attempt = 0

        while (attempt < maxRetries) {
            try {

                val (configTaquilla, configZona) = getConfigValues()

                android.util.Log.d(
                    "FolioRepository",
                    "Intento ${attempt + 1}: Solicitando folio para zona: $zonaSelect"
                )

                val request = FolioRequest(
                    zonaSelect = zonaSelect,
                    configZona = configZona,
                    configTaquilla = configTaquilla
                )

                val response = getApiService().getFolio(request)

                android.util.Log.d("FolioRepository", "Respuesta recibida: ${response.folio}")

                if (response.folio == "Error-NoExiste") {
                    emit(Result.failure(Exception("Configura la aplicación, esta taquilla no existe en el sistema")))
                } else {
                    emit(Result.success(response.folio))
                }

                return@flow

            } catch (e: SocketTimeoutException) {

                android.util.Log.w("FolioRepository", "Timeout intento ${attempt + 1}")
                attempt++

                if (attempt >= maxRetries) {
                    emit(Result.failure(Exception("Error de conexión: Tiempo de espera agotado. Verifica tu conexión a internet.")))
                    return@flow
                }

            } catch (e: UnknownHostException) {

                android.util.Log.w("FolioRepository", "Host desconocido intento ${attempt + 1}")
                attempt++

                if (attempt >= maxRetries) {
                    emit(Result.failure(Exception("Error de conexión: No se puede conectar al servidor. Verifica tu conexión a internet.")))
                    return@flow
                }

            } catch (e: IOException) {

                android.util.Log.w("FolioRepository", "Error IO intento ${attempt + 1}: ${e.message}")
                attempt++

                if (attempt >= maxRetries) {

                    val errorMessage = when {
                        e.message?.contains("timeout", ignoreCase = true) == true ->
                            "Error de conexión: Tiempo de espera agotado. Verifica tu conexión a internet."

                        e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                            "Error de conexión: No se puede resolver el servidor. Verifica la configuración."

                        else ->
                            "Error de conexión: No se pudo establecer la conexión al servidor."
                    }

                    emit(Result.failure(Exception(errorMessage)))
                    return@flow
                }

            } catch (e: HttpException) {

                android.util.Log.e("FolioRepository", "Error HTTP: ${e.code()} - ${e.message()}")

                val errorMessage = when (e.code()) {
                    404 -> "Error del servidor: Endpoint no encontrado. Verifica la configuración."
                    500, 502, 503 -> "Error del servidor: El servidor no está disponible temporalmente."
                    else -> "Error del servidor: ${e.code()}. No se puede comunicar con el servidor."
                }

                emit(Result.failure(Exception(errorMessage)))
                return@flow

            } catch (e: Exception) {

                android.util.Log.e("FolioRepository", "Error inesperado: ${e.message}", e)
                emit(Result.failure(Exception("Error inesperado: ${e.message ?: "Revisa la configuración de la aplicación."}")))
                return@flow
            }

            // Esperar antes del siguiente intento
            kotlinx.coroutines.delay(400)
        }
    }
} 
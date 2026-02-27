package com.ventatickets.ventaticketstaxis.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class TicketRepository(private val context: Context) {
    
    private fun getApiService() = ServiceLocator.getApiService(context)
    
    fun saveTicket(
        folio: String,
        costo: String,
        user: String,
        destino: String
    ): Flow<Result<String>> = flow {
        try {
            val configZona = AppConfig.getZona(context)
            android.util.Log.d("TicketRepository", "Guardando ticket - folio: $folio, costo: $costo, user: $user, destino: $destino, configZona: $configZona")
            
            val request = SaveTicketRequest(
                folio = folio,
                costo = costo,
                user = user,
                destino = destino,
                configZona = configZona
            )
            
            android.util.Log.d("TicketRepository", "Request: $request")
            
            val response = getApiService().saveTicket(request)
            
      
        } catch (e: HttpException) {
            android.util.Log.e("TicketRepository", "Error HTTP")
            emit(Result.failure(Exception("Error del servidor: No se puede comunicar con el Servidor.")))
        } catch (e: SocketTimeoutException) {
            android.util.Log.e("TicketRepository", "Error de timeout")
            emit(Result.failure(Exception("Error de conexión: Tiempo de espera agotado")))
        } catch (e: UnknownHostException) {
            android.util.Log.e("TicketRepository", "Error de host desconocido")
            emit(Result.failure(Exception("Error de conexión: No se puede conectar al servidor")))
        } catch (e: IOException) {
            android.util.Log.e("TicketRepository", "Error de IO")
            emit(Result.failure(Exception("Error de conexión: No se pudo establecer la conexión al Servidor.")))
        } catch (e: Exception) {
            android.util.Log.e("TicketRepository", "Error inesperado")
            emit(Result.failure(Exception("Error inesperado: Revisa la configuración de la aplicación.")))
        }
    }
} 
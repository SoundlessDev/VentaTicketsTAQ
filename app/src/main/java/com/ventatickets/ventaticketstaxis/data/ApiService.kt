package com.ventatickets.ventaticketstaxis.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("cargarZonas")
    suspend fun getZones(): List<Zone>

    @GET("cargarDropdownZona-S")
    suspend fun getDropdownZonaS(): List<Destination>

    @GET("cargarDropdownZonaDif-S")
    suspend fun getDropdownZonaDifS(): List<Destination>
    
    @POST("cargarFolio")
    suspend fun getFolio(@Body request: FolioRequest): FolioResponse

    @POST("guardarTicket")
    suspend fun saveTicket(@Body request: SaveTicketRequest): SaveTicketResponse

    // --- NUEVOS ENDPOINTS ---
    @POST("datosLogin")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<LoginResponse>

    @GET("datosEmpresa")
    suspend fun getEmpresaData(): List<EmpresaData>

    @POST("guardarDestinoManual")
    suspend fun guardarDestinoManual(
        @Body request: SaveDestinoManualRequest
    ): SaveDestinoManualResponse

    @GET("listarImpresoras")
    suspend fun listarImpresoras(): List<ImpresoraResponse>

    @GET("listarApiKeys")
    suspend fun listarApiKeys(): List<ApiKeyResponse>
} 
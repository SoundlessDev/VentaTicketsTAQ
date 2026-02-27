package com.ventatickets.ventaticketstaxis.data

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ServiceLocator {
    
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null
    private var currentContext: Context? = null
    
    // Interceptor para agregar API Key al header
    private fun createApiKeyInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            val apiKey = AppConfig.getApiKey(context)
            val originalRequest = chain.request()
            
            // Agregar header x-api-key solo si el API Key está configurado
            val newRequest = if (apiKey.isNotEmpty()) {
                originalRequest.newBuilder()
                    .header("x-api-key", apiKey)
                    .build()
            } else {
                originalRequest
            }
            
            chain.proceed(newRequest)
        }
    }
    
    private fun createOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(createApiKeyInterceptor(context))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            // Timeouts optimizados para fallar rápido cuando no hay conexión
            // connectTimeout: 3 segundos - falla rápido si no puede conectar al servidor
            .connectTimeout(3, TimeUnit.SECONDS)
            // readTimeout: 5 segundos - suficiente para respuestas normales del servidor
            .readTimeout(5, TimeUnit.SECONDS)
            // writeTimeout: 5 segundos - suficiente para enviar datos al servidor
            .writeTimeout(5, TimeUnit.SECONDS)
            // Retry automático deshabilitado para fallar rápido en lugar de reintentar
            .retryOnConnectionFailure(false)
            .build()
    }
    
    fun getApiService(context: Context): ApiService {
        // Recrear si el contexto cambió o si no hay instancia
        if (apiService == null || currentContext != context) {
            currentContext = context
            val baseUrl = AppConfig.getBaseUrl(context)
            val okHttpClient = createOkHttpClient(context)
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            apiService = retrofit!!.create(ApiService::class.java)
        }
        return apiService!!
    }
    
    fun updateBaseUrl(context: Context) {
        // Reset para forzar recreación con nueva URL y/o API Key
        retrofit = null
        apiService = null
        currentContext = null
        getApiService(context)
    }
    
    // Repositorios
    fun provideZoneRepository(context: Context): ZoneRepository {
        return ZoneRepository(context)
    }
    
    fun provideEmpresaRepository(context: Context): EmpresaRepository {
        return EmpresaRepository(context)
    }
} 
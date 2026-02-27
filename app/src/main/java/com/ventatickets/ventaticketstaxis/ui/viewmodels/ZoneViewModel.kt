package com.ventatickets.ventaticketstaxis.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ventatickets.ventaticketstaxis.data.Zone
import com.ventatickets.ventaticketstaxis.data.ZoneRepository
import com.ventatickets.ventaticketstaxis.data.FolioRepository
import com.ventatickets.ventaticketstaxis.data.AppConfig
import com.ventatickets.ventaticketstaxis.data.EmpresaGlobals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

sealed class ZoneLoadState {
    object Idle : ZoneLoadState()
    object Loading : ZoneLoadState()
    data class Success(val zones: List<Zone>) : ZoneLoadState()
    data class Error(val message: String) : ZoneLoadState()
    object NoConfiguration : ZoneLoadState()
}

class ZoneViewModel(private val context: Context) : ViewModel() {
    private val zoneRepository = ZoneRepository(context)
    private val folioRepository = FolioRepository(context)
    
    private val _zones = MutableStateFlow<List<Zone>>(emptyList())
    val zones: StateFlow<List<Zone>> = _zones.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Nuevo estado de carga
    private val _loadState = MutableStateFlow<ZoneLoadState>(ZoneLoadState.Idle)
    val loadState: StateFlow<ZoneLoadState> = _loadState.asStateFlow()
    
    // Estados para folio
    private val _folio = MutableStateFlow<String>("")
    val folio: StateFlow<String> = _folio.asStateFlow()
    
    private val _isLoadingFolio = MutableStateFlow(false)
    val isLoadingFolio: StateFlow<Boolean> = _isLoadingFolio.asStateFlow()
    
    private val _folioError = MutableStateFlow<String?>(null)
    val folioError: StateFlow<String?> = _folioError.asStateFlow()

    private val _isNocturno = MutableStateFlow(EmpresaGlobals.isNocturnoActive())
    val isNocturno: StateFlow<Boolean> = _isNocturno.asStateFlow()

    // Flag para controlar si las zonas ya fueron cargadas
    private var zonesLoaded = false

    init {
        Log.d("ZoneViewModel", "Inicializando ViewModel")
        EmpresaGlobals.initialize(context)
        cargarZonasLocales()
        observeNocturnoState()
    }

    // Verificar si se puede cargar desde el servidor
    fun canLoadFromServer(): Boolean {
        val serverIp = AppConfig.getServerIp(context)
        val hasValidConfig = serverIp.isNotEmpty() && serverIp.contains(":")
        Log.d("ZoneViewModel", "Puede cargar desde servidor: $hasValidConfig (IP: $serverIp)")
        return hasValidConfig
    }

    // Solo se debe llamar tras login exitoso
    fun guardarZonasEnPrefs(zonas: List<Zone>) {
        val prefs = context.getSharedPreferences("zonas_prefs", Context.MODE_PRIVATE)
        val zonasJson = Gson().toJson(zonas)
        prefs.edit().putString("zonas", zonasJson).apply()
        _zones.value = zonas
        zonesLoaded = true
        _loadState.value = ZoneLoadState.Success(zonas)
        Log.d("ZoneViewModel", "Zonas guardadas en SharedPreferences: ${zonas.size}")
    }

    // Cargar zonas locales al iniciar la app
    fun cargarZonasLocales() {
        val prefs = context.getSharedPreferences("zonas_prefs", Context.MODE_PRIVATE)
        val zonasJson = prefs.getString("zonas", null)
        if (zonasJson != null) {
            try {
                val type = object : TypeToken<List<Zone>>() {}.type
                val zonas: List<Zone> = Gson().fromJson(zonasJson, type)
                _zones.value = zonas
                zonesLoaded = true
                _loadState.value = ZoneLoadState.Success(zonas)
                Log.d("ZoneViewModel", "Zonas cargadas de SharedPreferences: ${zonas.size}")
            } catch (e: Exception) {
                Log.e("ZoneViewModel", "Error parseando zonas locales", e)
                _zones.value = emptyList()
                zonesLoaded = false
                _loadState.value = ZoneLoadState.Error("Error cargando datos locales")
            }
        } else {
            _zones.value = emptyList()
            zonesLoaded = false
            _loadState.value = ZoneLoadState.Idle
            Log.d("ZoneViewModel", "No hay zonas guardadas en SharedPreferences")
        }
    }

    // Cargar desde webservice con mejor manejo de errores
    fun loadZonesFromWebService() {
        if (!canLoadFromServer()) {
            Log.w("ZoneViewModel", "No se puede cargar desde servidor - configuración incompleta")
            // Preservar estado actual si hay datos cargados
            if (_zones.value.isNotEmpty()) {
                _loadState.value = ZoneLoadState.Success(_zones.value)
            } else {
                _loadState.value = ZoneLoadState.NoConfiguration
            }
            _error.value = "Configure el servidor antes de cargar datos"
            return
        }
        
        viewModelScope.launch {
            Log.d("ZoneViewModel", "Iniciando carga de zonas desde webservice")
            // Preservar datos actuales durante la carga
            val zonasActuales = _zones.value.toList()
            val tieneDatosActuales = zonasActuales.isNotEmpty()
            
            _isLoading.value = true
            _error.value = null
            // Solo cambiar a Loading si no hay datos actuales, de lo contrario mantener Success
            if (!tieneDatosActuales) {
                _loadState.value = ZoneLoadState.Loading
            }
            
            try {
                val zonesList = zoneRepository.getZones()
                if (zonesList.isEmpty()) {
                    val errorMsg = "No se pudieron cargar las zonas. La lista está vacía."
                    Log.w("ZoneViewModel", errorMsg)
                    _error.value = errorMsg
                    // Preservar datos actuales si existen, de lo contrario mostrar error
                    if (tieneDatosActuales) {
                        Log.d("ZoneViewModel", "Manteniendo ${zonasActuales.size} zonas actuales debido a error")
                        _loadState.value = ZoneLoadState.Success(zonasActuales)
                    } else {
                        _loadState.value = ZoneLoadState.Error(errorMsg)
                    }
                } else {
                    Log.d("ZoneViewModel", "Zonas cargadas exitosamente: ${zonesList.size}")
                    guardarZonasEnPrefs(zonesList)
                }
            } catch (e: Exception) {
                val errorMsg = "Error al cargar las zonas: ${e.message}"
                Log.e("ZoneViewModel", errorMsg, e)
                _error.value = errorMsg
                // Preservar datos actuales si existen, de lo contrario mostrar error
                if (tieneDatosActuales) {
                    Log.d("ZoneViewModel", "Manteniendo ${zonasActuales.size} zonas actuales debido a error: ${e.message}")
                    _loadState.value = ZoneLoadState.Success(zonasActuales)
                } else {
                    _loadState.value = ZoneLoadState.Error(errorMsg)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Método para verificar si las zonas están cargadas
    fun areZonesLoaded(): Boolean {
        return zonesLoaded && _zones.value.isNotEmpty()
    }

    // Método para limpiar zonas (por ejemplo, al cerrar sesión)
    fun limpiarZonasPrefs() {
        val prefs = context.getSharedPreferences("zonas_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("zonas").apply()
        _zones.value = emptyList()
        zonesLoaded = false
        _loadState.value = ZoneLoadState.Idle
        Log.d("ZoneViewModel", "Zonas eliminadas de SharedPreferences")
    }

    fun getEffectiveCost(zone: Zone, nocturno: Boolean = _isNocturno.value): Int {
        return if (nocturno && zone.costo_nocturno != null) {
            zone.costo_nocturno
        } else {
            zone.costo
        }
    }

    fun getFolio(zonaSelect: String) {
        viewModelScope.launch {
            Log.d("ZoneViewModel", "Solicitando folio para zona: $zonaSelect")
            _isLoadingFolio.value = true
            _folioError.value = null
            // Limpiar folio anterior antes de solicitar uno nuevo
            _folio.value = ""
            
            folioRepository.getFolio(zonaSelect).collect { result ->
                _isLoadingFolio.value = false
                result.fold(
                    onSuccess = { folio ->
                        Log.d("ZoneViewModel", "Folio recibido: $folio")
                        _folio.value = folio
                        // Limpiar cualquier error previo si se obtuvo éxito
                        _folioError.value = null
                    },
                    onFailure = { exception ->
                        Log.e("ZoneViewModel", "Error al obtener folio", exception)
                        // Asegurar que el folio esté vacío cuando hay error
                        _folio.value = ""
                        _folioError.value = exception.message ?: "Error desconocido"
                    }
                )
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
    
    fun clearFolioError() {
        _folioError.value = null
    }
    
    fun clearFolio() {
        _folio.value = ""
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ZoneViewModel? = null
        
        fun getInstance(context: Context): ZoneViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ZoneViewModel(context).also { INSTANCE = it }
            }
        }
        
        fun clearInstance() {
            INSTANCE = null
        }
    }

    private fun observeNocturnoState() {
        viewModelScope.launch {
            EmpresaGlobals.nocturnoState.collect { active ->
                Log.d("ZoneViewModel", "Estado nocturno actualizado: $active")
                _isNocturno.value = active
            }
        }
    }
} 
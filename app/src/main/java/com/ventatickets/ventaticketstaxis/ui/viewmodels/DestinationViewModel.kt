package com.ventatickets.ventaticketstaxis.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ventatickets.ventaticketstaxis.data.Destination
import com.ventatickets.ventaticketstaxis.data.DestinationRepository
import com.ventatickets.ventaticketstaxis.data.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

sealed class DestinationLoadState {
    object Idle : DestinationLoadState()
    object Loading : DestinationLoadState()
    data class Success(val zonaS: List<Destination>, val zonaDifS: List<Destination>) : DestinationLoadState()
    data class Error(val message: String) : DestinationLoadState()
    object NoConfiguration : DestinationLoadState()
}

class DestinationViewModel(private val context: Context) : ViewModel() {
    private val repository = DestinationRepository(context)

    private val _dropdownZonaS = MutableStateFlow<List<Destination>>(emptyList())
    val dropdownZonaS: StateFlow<List<Destination>> = _dropdownZonaS.asStateFlow()

    private val _dropdownZonaDifS = MutableStateFlow<List<Destination>>(emptyList())
    val dropdownZonaDifS: StateFlow<List<Destination>> = _dropdownZonaDifS.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Nuevo estado de carga
    private val _loadState = MutableStateFlow<DestinationLoadState>(DestinationLoadState.Idle)
    val loadState: StateFlow<DestinationLoadState> = _loadState.asStateFlow()

    // Flags para controlar si los destinos ya fueron cargados
    private var zonaSLoaded = false
    private var zonaDifSLoaded = false

    init {
        cargarDestinosLocales()
    }

    // Verificar si se puede cargar desde el servidor
    fun canLoadFromServer(): Boolean {
        val serverIp = AppConfig.getServerIp(context)
        val hasValidConfig = serverIp.isNotEmpty() && serverIp.contains(":")
        android.util.Log.d("DestinationViewModel", "Puede cargar desde servidor: $hasValidConfig (IP: $serverIp)")
        return hasValidConfig
    }

    // Solo se debe llamar tras login exitoso
    fun guardarDestinosEnPrefs(zonaS: List<Destination>, zonaDifS: List<Destination>) {
        val prefs = context.getSharedPreferences("destinos_prefs", Context.MODE_PRIVATE)
        val zonaSJson = Gson().toJson(zonaS)
        val zonaDifSJson = Gson().toJson(zonaDifS)
        prefs.edit()
            .putString("zonaS", zonaSJson)
            .putString("zonaDifS", zonaDifSJson)
            .apply()
        _dropdownZonaS.value = zonaS
        _dropdownZonaDifS.value = zonaDifS
        zonaSLoaded = true
        zonaDifSLoaded = true
        _loadState.value = DestinationLoadState.Success(zonaS, zonaDifS)
        android.util.Log.d("DestinationViewModel", "Destinos guardados en SharedPreferences: zonaS=${zonaS.size}, zonaDifS=${zonaDifS.size}")
    }

    // Cargar destinos locales al iniciar la app
    fun cargarDestinosLocales() {
        val prefs = context.getSharedPreferences("destinos_prefs", Context.MODE_PRIVATE)
        val zonaSJson = prefs.getString("zonaS", null)
        val zonaDifSJson = prefs.getString("zonaDifS", null)
        
        if (zonaSJson != null && zonaDifSJson != null) {
            try {
                val type = object : TypeToken<List<Destination>>() {}.type
                val zonaS: List<Destination> = Gson().fromJson(zonaSJson, type)
                val zonaDifS: List<Destination> = Gson().fromJson(zonaDifSJson, type)
                _dropdownZonaS.value = zonaS
                _dropdownZonaDifS.value = zonaDifS
                zonaSLoaded = true
                zonaDifSLoaded = true
                _loadState.value = DestinationLoadState.Success(zonaS, zonaDifS)
                android.util.Log.d("DestinationViewModel", "Destinos cargados de SharedPreferences: zonaS=${zonaS.size}, zonaDifS=${zonaDifS.size}")
            } catch (e: Exception) {
                android.util.Log.e("DestinationViewModel", "Error parseando destinos locales", e)
                _dropdownZonaS.value = emptyList()
                _dropdownZonaDifS.value = emptyList()
                zonaSLoaded = false
                zonaDifSLoaded = false
                _loadState.value = DestinationLoadState.Error("Error cargando datos locales")
            }
        } else {
            _dropdownZonaS.value = emptyList()
            _dropdownZonaDifS.value = emptyList()
            zonaSLoaded = false
            zonaDifSLoaded = false
            _loadState.value = DestinationLoadState.Idle
            android.util.Log.d("DestinationViewModel", "No hay destinos guardados en SharedPreferences")
        }
    }

    // Cargar desde webservice con mejor manejo de errores
    fun loadDestinationsFromWebService() {
        if (!canLoadFromServer()) {
            android.util.Log.w("DestinationViewModel", "No se puede cargar desde servidor - configuración incompleta")
            // Preservar estado actual si hay datos cargados
            if (_dropdownZonaS.value.isNotEmpty() || _dropdownZonaDifS.value.isNotEmpty()) {
                _loadState.value = DestinationLoadState.Success(_dropdownZonaS.value, _dropdownZonaDifS.value)
            } else {
                _loadState.value = DestinationLoadState.NoConfiguration
            }
            _error.value = "Configure el servidor antes de cargar datos"
            return
        }
        
        viewModelScope.launch {
            android.util.Log.d("DestinationViewModel", "Iniciando carga de destinos desde webservice")
            // Preservar datos actuales durante la carga
            val destinosActualesS = _dropdownZonaS.value.toList()
            val destinosActualesDifS = _dropdownZonaDifS.value.toList()
            val tieneDatosActuales = destinosActualesS.isNotEmpty() || destinosActualesDifS.isNotEmpty()
            
            _isLoading.value = true
            _error.value = null
            // Solo cambiar a Loading si no hay datos actuales, de lo contrario mantener Success
            if (!tieneDatosActuales) {
                _loadState.value = DestinationLoadState.Loading
            }
            
            try {
                var zonaSLoaded = false
                var zonaDifSLoaded = false
                var zonaSList = emptyList<Destination>()
                var zonaDifSList = emptyList<Destination>()
                
                // Cargar zona S
                try {
                    var zonaSError: Throwable? = null
                    repository.getDropdownZonaS().collect { result ->
                        result.fold(
                            onSuccess = { destinations ->
                                zonaSList = destinations
                                zonaSLoaded = true
                                android.util.Log.d("DestinationViewModel", "Dropdown zona S cargado: ${destinations.size} destinos")
                            },
                            onFailure = { exception ->
                                android.util.Log.e("DestinationViewModel", "Error cargando zona S", exception)
                                zonaSError = exception
                                // Si hay datos actuales, mantenerlos; de lo contrario propagar el error
                                if (tieneDatosActuales) {
                                    android.util.Log.d("DestinationViewModel", "Manteniendo destinos actuales debido a error en zona S")
                                    zonaSList = destinosActualesS
                                    zonaSLoaded = true // Usar datos actuales como fallback
                                }
                            }
                        )
                    }
                    // Si hubo error y no hay datos actuales, propagar el error
                    if (zonaSError != null && !tieneDatosActuales) {
                        throw zonaSError!!
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("DestinationViewModel", "Error en zona S", e)
                    // Si hay datos actuales, mantenerlos; de lo contrario propagar el error
                    if (!tieneDatosActuales) {
                        throw e
                    } else {
                        android.util.Log.d("DestinationViewModel", "Manteniendo destinos actuales debido a error en zona S")
                        zonaSList = destinosActualesS
                        zonaSLoaded = true // Usar datos actuales como fallback
                    }
                }

                // Cargar zona dif S
                try {
                    var zonaDifSError: Throwable? = null
                    repository.getDropdownZonaDifS().collect { result ->
                        result.fold(
                            onSuccess = { destinations ->
                                zonaDifSList = destinations
                                zonaDifSLoaded = true
                                android.util.Log.d("DestinationViewModel", "Dropdown zona dif S cargado: ${destinations.size} destinos")
                            },
                            onFailure = { exception ->
                                android.util.Log.e("DestinationViewModel", "Error cargando zona dif S", exception)
                                zonaDifSError = exception
                                // Si hay datos actuales, mantenerlos; de lo contrario propagar el error
                                if (tieneDatosActuales) {
                                    android.util.Log.d("DestinationViewModel", "Manteniendo destinos actuales debido a error en zona dif S")
                                    zonaDifSList = destinosActualesDifS
                                    zonaDifSLoaded = true // Usar datos actuales como fallback
                                }
                            }
                        )
                    }
                    // Si hubo error y no hay datos actuales, propagar el error
                    if (zonaDifSError != null && !tieneDatosActuales) {
                        throw zonaDifSError!!
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("DestinationViewModel", "Error en zona dif S", e)
                    // Si hay datos actuales, mantenerlos; de lo contrario propagar el error
                    if (!tieneDatosActuales) {
                        throw e
                    } else {
                        android.util.Log.d("DestinationViewModel", "Manteniendo destinos actuales debido a error en zona dif S")
                        zonaDifSList = destinosActualesDifS
                        zonaDifSLoaded = true // Usar datos actuales como fallback
                    }
                }

                // Guardar en SharedPreferences después de cargar ambos
                if (zonaSLoaded && zonaDifSLoaded) {
                    // Solo actualizar si se obtuvieron nuevos datos del servidor
                    if (zonaSList.isNotEmpty() || zonaDifSList.isNotEmpty()) {
                        guardarDestinosEnPrefs(zonaSList, zonaDifSList)
                    } else if (tieneDatosActuales) {
                        // Mantener datos actuales si no se pudieron obtener nuevos datos
                        android.util.Log.d("DestinationViewModel", "Manteniendo destinos actuales: S=${destinosActualesS.size}, DifS=${destinosActualesDifS.size}")
                        _loadState.value = DestinationLoadState.Success(destinosActualesS, destinosActualesDifS)
                    } else {
                        val errorMsg = "No se pudieron cargar todos los destinos"
                        android.util.Log.w("DestinationViewModel", errorMsg)
                        _error.value = errorMsg
                        _loadState.value = DestinationLoadState.Error(errorMsg)
                    }
                } else {
                    val errorMsg = "No se pudieron cargar todos los destinos"
                    android.util.Log.w("DestinationViewModel", errorMsg)
                    _error.value = errorMsg
                    // Preservar datos actuales si existen
                    if (tieneDatosActuales) {
                        android.util.Log.d("DestinationViewModel", "Manteniendo destinos actuales debido a error parcial")
                        _loadState.value = DestinationLoadState.Success(destinosActualesS, destinosActualesDifS)
                    } else {
                        _loadState.value = DestinationLoadState.Error(errorMsg)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Error al cargar destinos: ${e.message}"
                android.util.Log.e("DestinationViewModel", errorMsg, e)
                _error.value = errorMsg
                // Preservar datos actuales si existen, de lo contrario mostrar error
                if (tieneDatosActuales) {
                    android.util.Log.d("DestinationViewModel", "Manteniendo destinos actuales debido a error: S=${destinosActualesS.size}, DifS=${destinosActualesDifS.size}")
                    _loadState.value = DestinationLoadState.Success(destinosActualesS, destinosActualesDifS)
                } else {
                    _loadState.value = DestinationLoadState.Error(errorMsg)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Método para verificar si los destinos están cargados
    fun areDestinationsLoaded(): Boolean {
        return zonaSLoaded && zonaDifSLoaded && 
               _dropdownZonaS.value.isNotEmpty() && _dropdownZonaDifS.value.isNotEmpty()
    }
    
    // Método para limpiar destinos (por ejemplo, al cerrar sesión)
    fun limpiarDestinosPrefs() {
        val prefs = context.getSharedPreferences("destinos_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _dropdownZonaS.value = emptyList()
        _dropdownZonaDifS.value = emptyList()
        zonaSLoaded = false
        zonaDifSLoaded = false
        _loadState.value = DestinationLoadState.Idle
        android.util.Log.d("DestinationViewModel", "Destinos eliminados de SharedPreferences")
    }
    
    fun clearError() {
        _error.value = null
    }
    
    companion object {
        @Volatile
        private var INSTANCE: DestinationViewModel? = null
        
        fun getInstance(context: Context): DestinationViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DestinationViewModel(context).also { INSTANCE = it }
            }
        }
        
        fun clearInstance() {
            INSTANCE = null
        }
    }
} 
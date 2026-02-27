package com.ventatickets.ventaticketstaxis.ui.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ventatickets.ventaticketstaxis.alarms.TarifasAlarmReceiver
import com.ventatickets.ventaticketstaxis.data.EmpresaData
import com.ventatickets.ventaticketstaxis.data.EmpresaGlobals
import com.ventatickets.ventaticketstaxis.data.LoginRepository
import com.ventatickets.ventaticketstaxis.data.LoginResponse
import com.ventatickets.ventaticketstaxis.data.AppConfig
import com.ventatickets.ventaticketstaxis.ui.viewmodels.ZoneViewModel
import com.ventatickets.ventaticketstaxis.ui.viewmodels.DestinationViewModel
import com.ventatickets.ventaticketstaxis.workers.DataRefreshWorker
import com.ventatickets.ventaticketstaxis.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LoginRepository(application.applicationContext)

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _empresaData = MutableStateFlow<EmpresaData?>(null)
    val empresaData: StateFlow<EmpresaData?> = _empresaData.asStateFlow()

    init {
        if (repository.isLoggedIn()) {
            _loginState.value = LoginState.Success(repository.getSavedUser())
            // No cargar automáticamente los datos de empresa al iniciar
            // Se cargarán cuando sea necesario (al abrir el diálogo o al imprimir)
        }
    }

    fun login(usuario: String, contrasena: String) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            val result = repository.login(usuario, contrasena)
            result.fold(
                onSuccess = {
                    _loginState.value = LoginState.Success(it)
                    loadEmpresaData()
                    onLoginSuccess(getApplication())
                },
                onFailure = {
                    _loginState.value = LoginState.Error(it.message ?: "Error de inicio de sesión. Verifique sus datos o la conexión.")
                }
            )
        }
    }


    fun loadEmpresaData() {
        loadEmpresaData(forceRefresh = false)
    }
    
    fun loadEmpresaData(forceRefresh: Boolean) {
        android.util.Log.d("LoginViewModel", "Iniciando carga de datos de empresa... (forceRefresh=$forceRefresh)")
        viewModelScope.launch {
            val result = repository.getEmpresaData(forceRefresh)
            result.fold(
                onSuccess = { 
                    android.util.Log.d("LoginViewModel", "Datos de empresa cargados exitosamente: ${it.descrip}")
                    android.util.Log.d("LoginViewModel", "Horarios: ${it.ini_nocturno} - ${it.fin_nocturno}")
                    _empresaData.value = it
                    // 🔥 GUARDAR EN EmpresaGlobals
                    EmpresaGlobals.setFromData(it)
                    // 🔔 Programar alarmas ahora que ya tenemos horarios
                    DataRefreshWorker.scheduleDataRefresh(getApplication())
                },
                onFailure = { 
                    android.util.Log.e("LoginViewModel", "Error cargando datos de empresa: ${it.message}")
                    _empresaData.value = null
                }
            )
        }
    }
    
    // Versión suspend para poder esperar su completación
    suspend fun loadEmpresaDataSuspend(forceRefresh: Boolean): Result<com.ventatickets.ventaticketstaxis.data.EmpresaData> {
        android.util.Log.d("LoginViewModel", "Iniciando carga de datos de empresa (suspend)... (forceRefresh=$forceRefresh)")
        return repository.getEmpresaData(forceRefresh)
    }

    fun logout() {
        repository.logout()
        ZoneViewModel.clearInstance()
        DestinationViewModel.clearInstance()
        
        // Cancelar el worker de refresco automático
        DataRefreshWorker.cancelDataRefresh(getApplication())
        
        // Cancelar todas las notificaciones de tarifas
        NotificationHelper.cancelAllNotifications(getApplication())
        
        _loginState.value = LoginState.Idle
        _empresaData.value = null
    }

    // Verificar si la configuración está completa
    fun verifyConfiguration(): Boolean {
        val isComplete = AppConfig.isConfigurationComplete(getApplication())
        android.util.Log.d("LoginViewModel", "Configuración completa: $isComplete")
        return isComplete
    }

    private fun onLoginSuccess(context: Context) {
        android.util.Log.d("LoginViewModel", "Iniciando carga de datos después del login exitoso")
        
        val zoneViewModel = ZoneViewModel.getInstance(context)
        val destinationViewModel = DestinationViewModel.getInstance(context)
        
        zoneViewModel.loadZonesFromWebService()
        destinationViewModel.loadDestinationsFromWebService()
        
        // Iniciar el worker de refresco automático después de cargar datos de empresa
        if (EmpresaGlobals.isDataLoaded()) {
            DataRefreshWorker.scheduleDataRefresh(context)
            android.util.Log.d("LoginViewModel", "Worker de refresco automático iniciado")
        } else {
            android.util.Log.d("LoginViewModel", "Esperando datos de empresa para programar worker")
        }
    }

    // Método para reprogramar worker cuando se carguen los datos de empresa
    fun rescheduleWorkerIfNeeded() {
        EmpresaGlobals.descrip?.let { empresa ->
            android.util.Log.d("LoginViewModel", "Datos de empresa cargados: $empresa")
            // Reprogramar worker con los nuevos horarios
            DataRefreshWorker.rescheduleDataRefresh(getApplication())
        }
    }

    // Método para verificar estado de workers
    fun checkWorkerStatus() {
        DataRefreshWorker.checkActiveWorkers(getApplication())
    }


    fun limpiarDatosLocales(context: Context) {
        val zoneViewModel = ZoneViewModel.getInstance(context)
        val destinationViewModel = DestinationViewModel.getInstance(context)
        zoneViewModel.limpiarZonasPrefs()
        destinationViewModel.limpiarDestinosPrefs()
        EmpresaGlobals.clear()
        android.util.Log.d("LoginViewModel", "Datos locales limpiados al cerrar sesión")
    }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val user: LoginResponse?) : LoginState()
        data class Error(val message: String) : LoginState()
    }
} 
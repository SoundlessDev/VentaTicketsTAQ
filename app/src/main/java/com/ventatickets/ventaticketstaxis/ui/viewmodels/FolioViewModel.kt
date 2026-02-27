package com.ventatickets.ventaticketstaxis.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ventatickets.ventaticketstaxis.data.FolioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FolioViewModel(private val folioRepository: FolioRepository) : ViewModel() {
    
    private val _folio = MutableStateFlow<String>("")
    val folio: StateFlow<String> = _folio.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun getFolio(zonaSelect: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            folioRepository.getFolio(zonaSelect).collect { result ->
                _isLoading.value = false
                
                result.fold(
                    onSuccess = { folio ->
                        _folio.value = folio
                        android.util.Log.d("FolioViewModel", "Folio obtenido: $folio")
                    },
                    onFailure = { exception ->
                        _error.value = exception.message
                        android.util.Log.e("FolioViewModel", "Error al obtener folio: ${exception.message}")
                    }
                )
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearFolio() {
        _folio.value = ""
    }
} 
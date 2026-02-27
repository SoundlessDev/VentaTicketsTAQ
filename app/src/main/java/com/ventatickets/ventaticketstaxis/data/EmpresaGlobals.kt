package com.ventatickets.ventaticketstaxis.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EmpresaGlobals {
    private const val PREFS_NAME = "empresa_prefs"
    private const val KEY_CLA_EMPRE = "cla_empre"
    private const val KEY_DESCRIP = "descrip"
    private const val KEY_RFC = "rfc"
    private const val KEY_DIRECCION = "direccion"
    private const val KEY_INI_NOCTURNO = "ini_nocturno"
    private const val KEY_FIN_NOCTURNO = "fin_nocturno"
    
    private var prefs: SharedPreferences? = null
    private val nocturnoStateInternal = MutableStateFlow(false)
    
    val nocturnoState: StateFlow<Boolean> = nocturnoStateInternal.asStateFlow()
    
    var cla_empre: String? = null
        get() = field ?: prefs?.getString(KEY_CLA_EMPRE, null)
        set(value) {
            field = value
            prefs?.edit()?.putString(KEY_CLA_EMPRE, value)?.apply()
        }
    
    var descrip: String? = null
        get() = field ?: prefs?.getString(KEY_DESCRIP, null)
        set(value) {
            field = value
            prefs?.edit()?.putString(KEY_DESCRIP, value)?.apply()
        }
    
    var rfc: String? = null
        get() = field ?: prefs?.getString(KEY_RFC, null)
        set(value) {
            field = value
            prefs?.edit()?.putString(KEY_RFC, value)?.apply()
        }
    
    var direccion: String? = null
        get() = field ?: prefs?.getString(KEY_DIRECCION, null)
        set(value) {
            field = value
            prefs?.edit()?.putString(KEY_DIRECCION, value)?.apply()
        }
    
    var ini_nocturno: String? = null
        get() = field ?: prefs?.getString(KEY_INI_NOCTURNO, null)
        set(value) {
            field = value
            prefs?.edit()?.putString(KEY_INI_NOCTURNO, value)?.apply()
            recalculateNocturnoState()
        }
    
    var fin_nocturno: String? = null
        get() = field ?: prefs?.getString(KEY_FIN_NOCTURNO, null)
        set(value) {
            field = value
            prefs?.edit()?.putString(KEY_FIN_NOCTURNO, value)?.apply()
            recalculateNocturnoState()
        }

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            android.util.Log.d("EmpresaGlobals", "SharedPreferences inicializado")
            recalculateNocturnoState()
        }
    }

    fun setFromData(data: EmpresaData) {
        android.util.Log.d("EmpresaGlobals", "Guardando datos de empresa: ${data.descrip}")
        cla_empre = data.cla_empre
        descrip = data.descrip
        rfc = data.rfc
        direccion = data.direccion
        ini_nocturno = data.ini_nocturno
        fin_nocturno = data.fin_nocturno
        android.util.Log.d("EmpresaGlobals", "Datos guardados - descrip: $descrip, rfc: $rfc")
        recalculateNocturnoState()
    }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
        cla_empre = null
        descrip = null
        rfc = null
        direccion = null
        ini_nocturno = null
        fin_nocturno = null
        android.util.Log.d("EmpresaGlobals", "Datos de empresa limpiados")
        setNocturnoActive(false)
    }
    
    fun isDataLoaded(): Boolean {
        return descrip != null && rfc != null && direccion != null
    }
    
    fun isNocturnoActive(): Boolean = nocturnoStateInternal.value
    
    fun setNocturnoActive(active: Boolean) {
        if (nocturnoStateInternal.value != active) {
            android.util.Log.d("EmpresaGlobals", "Estado nocturno actualizado: $active")
            nocturnoStateInternal.value = active
        }
    }
    
    fun recalculateNocturnoState() {
        val active = calculateNocturnoActive(ini_nocturno, fin_nocturno)
        setNocturnoActive(active)
    }
    
    private fun calculateNocturnoActive(iniNocturno: String?, finNocturno: String?): Boolean {
        if (iniNocturno.isNullOrBlank() || finNocturno.isNullOrBlank()) {
            return false
        }
        return try {
            NocturnoHelper.isHorarioNocturno(iniNocturno, finNocturno)
        } catch (e: Exception) {
            android.util.Log.e("EmpresaGlobals", "Error calculando estado nocturno", e)
            false
        }
    }
} 
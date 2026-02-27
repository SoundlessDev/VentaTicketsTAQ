package com.ventatickets.ventaticketstaxis.data

import android.content.Context
import android.content.SharedPreferences

object AppConfig {
    private const val PREFS_NAME = "config_prefs"
    private const val KEY_SERVER_IP = "serverIp"
    private const val KEY_TAQUILLA = "taquilla"
    private const val KEY_ZONA = "zona"
    private const val KEY_TIPO_IMPRESORA = "tipoImpresora"
    private const val KEY_PRINTER_MAC = "printerMac"
    private const val KEY_API_KEY = "apiKey"
    private const val KEY_TIPO_CODIGO = "tipoCodigo"
    
    // Valores por defecto
    private const val DEFAULT_SERVER_IP = ""
    private const val DEFAULT_TAQUILLA = ""
    private const val DEFAULT_ZONA = ""
    private const val DEFAULT_TIPO_IMPRESORA = "ZEBRA (ZPL)"
    private const val DEFAULT_PRINTER_MAC = ""
    private const val DEFAULT_API_KEY = ""
    private const val DEFAULT_TIPO_CODIGO = "QR"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Getters
    fun getServerIp(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_IP, DEFAULT_SERVER_IP) ?: DEFAULT_SERVER_IP
    }
    
    fun getTaquilla(context: Context): String {
        return getPrefs(context).getString(KEY_TAQUILLA, DEFAULT_TAQUILLA) ?: DEFAULT_TAQUILLA
    }
    
    fun getZona(context: Context): String {
        return getPrefs(context).getString(KEY_ZONA, DEFAULT_ZONA) ?: DEFAULT_ZONA
    }
    
    fun getTipoImpresora(context: Context): String {
        return getPrefs(context).getString(KEY_TIPO_IMPRESORA, DEFAULT_TIPO_IMPRESORA) ?: DEFAULT_TIPO_IMPRESORA
    }
    
    fun getPrinterMac(context: Context): String {
        return getPrefs(context).getString(KEY_PRINTER_MAC, DEFAULT_PRINTER_MAC) ?: DEFAULT_PRINTER_MAC
    }
    
    fun getApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }
    
    fun getTipoCodigo(context: Context): String {
        return getPrefs(context).getString(KEY_TIPO_CODIGO, DEFAULT_TIPO_CODIGO) ?: DEFAULT_TIPO_CODIGO
    }
    
    // Setters
    fun setServerIp(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_SERVER_IP, value.trim()).apply()
    }
    
    fun setTaquilla(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_TAQUILLA, value.trim()).apply()
    }
    
    fun setZona(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_ZONA, value.trim()).apply()
    }
    
    fun setTipoImpresora(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_TIPO_IMPRESORA, value.trim()).apply()
    }
    
    fun setPrinterMac(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_PRINTER_MAC, value.trim()).apply()
    }
    
    fun setApiKey(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_API_KEY, value.trim()).apply()
    }
    
    fun setTipoCodigo(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_TIPO_CODIGO, value.trim()).apply()
    }
    
    // Métodos de utilidad
    fun getBaseUrl(context: Context): String {
        val serverIp = getServerIp(context)
        return "http://$serverIp/"
    }
    
    fun isZebraPrinter(context: Context): Boolean {
        val tipo = getTipoImpresora(context)
        return tipo.contains("ZEBRA", ignoreCase = true)
    }
    
    fun isEscPosPrinter(context: Context): Boolean {
        val tipo = getTipoImpresora(context)
        return tipo.contains("ESC/POS", ignoreCase = true)
    }
    
    fun isStarPrinter(context: Context): Boolean {
        val tipo = getTipoImpresora(context)
        return tipo.contains("STAR", ignoreCase = true)
    }
    
    fun isOtherPrinter(context: Context): Boolean {
        val tipo = getTipoImpresora(context)
        return tipo.contains("OTROS", ignoreCase = true)
    }
    
    fun isQrCode(context: Context): Boolean {
        val tipoCodigo = getTipoCodigo(context)
        return tipoCodigo.contains("QR", ignoreCase = true)
    }
    
    fun isBarcode(context: Context): Boolean {
        val tipoCodigo = getTipoCodigo(context)
        return tipoCodigo.contains("Código de Barras", ignoreCase = true) || 
               tipoCodigo.contains("Codigo de Barras", ignoreCase = true) ||
               tipoCodigo.contains("Barcode", ignoreCase = true)
    }
    
    fun getConfigJson(context: Context): String {
        return """{
            "taquilla": "${getTaquilla(context)}",
            "zonaSelect": "${getZona(context)}",
            "zonataquilla": "${getZona(context)}",
            "tipoImpresora": "${getTipoImpresora(context)}",
            "printerMac": "${getPrinterMac(context)}"
        }"""
    }
    
    // Verificar si la configuración está completa
    fun isConfigurationComplete(context: Context): Boolean {
        val serverIp = getServerIp(context).trim()
        val taquilla = getTaquilla(context).trim()
        val zona = getZona(context).trim()
        val printerMac = getPrinterMac(context).trim()
        return serverIp.isNotEmpty() && taquilla.isNotEmpty() && zona.isNotEmpty() && printerMac.isNotEmpty()
    }
    
    // Limpiar toda la configuración
    fun clearConfig(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
} 
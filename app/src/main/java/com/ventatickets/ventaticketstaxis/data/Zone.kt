package com.ventatickets.ventaticketstaxis.data

data class Zone(
    val zona: String,
    val descrip: String,
    val costo: Int,
    val retencion: Double,
    val costo_nocturno: Int? = null,
    val retencion_nocturno: Double? = null
)

data class Destination(
    val titulo: String,
    val clave: Int,
    val descrip: String,
    val zona: String,
    val costo: Int,
    val costo_nocturno: Int? = null,
    val referencia: String?,
    val id: String?,
    val kms: Double?
)
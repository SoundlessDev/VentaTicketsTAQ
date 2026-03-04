package com.ventatickets.ventaticketstaxis.data


data class SaveDestinoManualRequest(
    val descrip: String,
    val zona: String,
    val costo: String,
    val costo_nocturno: String
)

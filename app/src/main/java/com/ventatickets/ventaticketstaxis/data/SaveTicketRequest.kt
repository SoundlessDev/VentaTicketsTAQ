package com.ventatickets.ventaticketstaxis.data

data class SaveTicketRequest(
    val folio: String,
    val costo: String,
    val user: String,
    val destino: String,
    val configZona: String
) 
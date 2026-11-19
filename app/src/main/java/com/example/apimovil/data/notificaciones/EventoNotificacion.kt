package com.example.apimovil.data.notificaciones

data class EventoNotificacion(
    val id: String,
    val tipo: String,
    val titulo: String,
    val cuerpo: String,
    val fechaMillis: Long,
    val destino: String,
    val ordenId: Int? = null
)

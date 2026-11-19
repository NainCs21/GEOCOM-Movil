package com.example.apimovil.data.notificaciones

data class NotificacionApp(
    val id: String,
    val tipo: String,
    val titulo: String,
    val cuerpo: String,
    val fechaMillis: Long,
    val destino: String,
    val ordenId: Int? = null,
    var leida: Boolean = false
)

package com.example.apimovil.data.notificaciones

interface FuenteNotificaciones {
    suspend fun obtenerEventos(): List<EventoNotificacion>
}

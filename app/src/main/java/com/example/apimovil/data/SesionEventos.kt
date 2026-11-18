package com.example.apimovil.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Administra eventos globales de sesion (como expiracion por 401 o inactividad)
object SesionEventos {
    private val _sesionExpirada = MutableSharedFlow<String>(replay = 0)
    val sesionExpirada: SharedFlow<String> = _sesionExpirada.asSharedFlow()

    private var emitido = false

    suspend fun notificarSesionExpirada(mensaje: String) {
        if (!emitido) {
            emitido = true
            _sesionExpirada.emit(mensaje)
        }
    }

    fun reiniciarGuardia() {
        emitido = false
    }
}

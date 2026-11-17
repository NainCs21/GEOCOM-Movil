package com.example.apimovil.ui.common

import com.google.gson.Gson
import com.example.apimovil.data.models.MensajeResponse
import java.io.IOException
import retrofit2.HttpException

data class UiState<T>(
    val loading: Boolean = false,
    val data: T? = null,
    val error: String? = null
)

/**
 * Traduce excepciones de Retrofit/OkHttp a un mensaje que se le puede
 * mostrar directo al usuario. Los 404 del escaneo de QR y los 401/403 de
 * login/roles ya traen su propio mensaje en el body ("mensaje" o "message"),
 * así que se intenta leer eso primero antes de caer a un texto genérico.
 */
fun mensajeDeError(error: Exception): String {
    if (error is HttpException) {
        val cuerpo = error.response()?.errorBody()?.string()
        if (!cuerpo.isNullOrBlank()) {
            try {
                val parseado = Gson().fromJson(cuerpo, MensajeResponse::class.java)
                if (!parseado.texto.isBlank() && parseado.texto != "Ocurrió un error inesperado.") {
                    return parseado.texto
                }
            } catch (_: Exception) { /* cae al mensaje genérico de abajo */ }
        }
        return when (error.code()) {
            401 -> "Correo o contraseña incorrectos."
            403 -> "No tienes permiso para hacer esto."
            404 -> "No se encontró lo que buscabas."
            422 -> "Revisa los datos, algo no es válido."
            else -> "El servidor respondió con el código ${error.code()}."
        }
    }
    return when (error) {
        is IOException -> "No se pudo conectar al servidor. Revisa que el backend Laravel esté activo."
        else -> error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Ocurrió un error inesperado. Intenta de nuevo."
    }
}

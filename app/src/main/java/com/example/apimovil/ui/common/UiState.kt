package com.example.apimovil.ui.common

import com.example.apimovil.data.models.MensajeResponse
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

data class UiState<T>(
    val loading: Boolean = false,
    val data: T? = null,
    val error: String? = null,
    val camposError: Map<String, String> = emptyMap()
)

data class ErrorInterpretado(
    val codigo: Int?,
    val mensaje: String,
    val camposError: Map<String, String> = emptyMap()
)

fun mensajeDeError(error: Exception): String {
    return interpretarError(error).mensaje
}

fun interpretarError(error: Exception): ErrorInterpretado {
    if (error is HttpException) {
        val code = error.code()
        var cuerpoStr: String? = null
        var parseado: MensajeResponse? = null

        try {
            cuerpoStr = error.response()?.errorBody()?.string()
            if (!cuerpoStr.isNullOrBlank()) {
                parseado = Gson().fromJson(cuerpoStr, MensajeResponse::class.java)
            }
        } catch (_: Exception) {}

        val serverMsg = parseado?.texto?.takeIf {
            it.isNotBlank() && it != "Ocurrió un error inesperado." && it != "Ocurrio un error inesperado."
        }

        when (code) {
            400 -> return ErrorInterpretado(code, serverMsg ?: "No se pudo procesar la solicitud. Revisa los datos e intenta de nuevo.")
            401 -> return ErrorInterpretado(code, "Correo o contraseña incorrectos.")
            403 -> {
                val msg = if (serverMsg != null && contieneEspanol(serverMsg)) serverMsg else "No tienes permiso para hacer esto."
                return ErrorInterpretado(code, msg)
            }
            404 -> {
                val msg = if (serverMsg != null && contieneEspanol(serverMsg)) serverMsg else "No se encontró lo que buscabas."
                return ErrorInterpretado(code, msg)
            }
            405, 409 -> return ErrorInterpretado(code, "Esa acción ya no está disponible. Actualiza la pantalla e intenta de nuevo.")
            413 -> return ErrorInterpretado(code, "El archivo es demasiado grande. Toma la foto de nuevo.")
            415 -> return ErrorInterpretado(code, "Ese tipo de archivo no está permitido. Usa una foto JPG o PNG.")
            422 -> {
                val campos = mutableMapOf<String, String>()
                val rawErrors = parseado?.errors
                if (!rawErrors.isNullOrEmpty()) {
                    for ((campo, msgs) in rawErrors) {
                        val rawMsg = msgs.firstOrNull().orEmpty()
                        val msgTraducido = mapearErrorCampo(campo, rawMsg)
                        campos[campo] = msgTraducido
                    }
                }
                val primerError = campos.values.firstOrNull() ?: serverMsg ?: "Revisa los datos, algo no es válido."
                return ErrorInterpretado(code, primerError, campos)
            }
            429 -> {
                val retryHeader = error.response()?.headers()?.get("Retry-After")
                val retrySecs = retryHeader?.toIntOrNull()
                val msg = if (retrySecs != null) {
                    "Demasiados intentos. Espera $retrySecs segundos e intenta de nuevo."
                } else {
                    "Demasiados intentos. Espera un minuto e intenta de nuevo."
                }
                return ErrorInterpretado(code, msg)
            }
            in 500..599 -> return ErrorInterpretado(code, "El servidor tuvo un problema. Intenta de nuevo en unos minutos.")
            else -> return ErrorInterpretado(code, serverMsg ?: "Ocurrió un error inesperado. Intenta de nuevo.")
        }
    }

    return when (error) {
        is UnknownHostException, is ConnectException -> ErrorInterpretado(null, "No hay conexión a internet. Revísala e intenta de nuevo.")
        is SocketTimeoutException -> ErrorInterpretado(null, "La conexión tardó demasiado. Revisa tu señal e intenta de nuevo.")
        is SSLException -> ErrorInterpretado(null, "No se pudo establecer una conexión segura. Revisa la fecha y hora del teléfono.")
        is JsonParseException, is MalformedJsonException -> ErrorInterpretado(null, "El servidor respondió algo inesperado. Intenta de nuevo.")
        is IOException -> ErrorInterpretado(null, "No se pudo conectar. Revisa tu conexión a internet e intenta de nuevo.")
        else -> ErrorInterpretado(null, "Ocurrió un error inesperado. Intenta de nuevo.")
    }
}

private fun contieneEspanol(texto: String): Boolean {
    val palabrasIngles = listOf(
        "the ", "field ", "must ", "required", "invalid", "taken", "already", "selected",
        "unauthorized", "unauthenticated", "this action", "given data"
    )
    val min = texto.lowercase()
    return palabrasIngles.none { min.contains(it) }
}

private fun mapearErrorCampo(campo: String, rawMsg: String): String {
    val isTaken = rawMsg.contains("taken", ignoreCase = true) ||
                  rawMsg.contains("already", ignoreCase = true) ||
                  rawMsg.contains("ya esta en uso", ignoreCase = true) ||
                  rawMsg.contains("ya existe", ignoreCase = true)

    if (isTaken) {
        return when (campo) {
            "correo", "email" -> "Ya existe una cuenta con ese correo."
            "placa" -> "Ya existe un vehículo con esa placa."
            "codigo_qr" -> "Ya existe una gasolinera con ese código QR."
            else -> "Ese dato ya está en uso."
        }
    }

    val etiqueta = when (campo) {
        "correo", "email" -> "correo"
        "contrasena", "password" -> "contraseña"
        "telefono" -> "teléfono"
        "nombre" -> "nombre"
        "cargo" -> "cargo"
        "placa" -> "placa"
        "marca" -> "marca"
        "modelo" -> "modelo"
        "anio" -> "año"
        "rendimiento_km_litro" -> "rendimiento"
        "direccion" -> "dirección"
        "codigo_qr" -> "código QR"
        "descripcion_actividad" -> "actividad"
        "descripcion_mgp" -> "equipo MGP"
        "monto" -> "monto"
        "justificacion" -> "justificación"
        else -> campo.replace("_", " ")
    }

    if (rawMsg.isNotBlank() && contieneEspanol(rawMsg)) {
        return rawMsg
    }

    return "El campo $etiqueta no es válido."
}

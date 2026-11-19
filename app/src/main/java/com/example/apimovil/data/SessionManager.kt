package com.example.apimovil.data

import android.content.Context
import android.content.SharedPreferences

// Administra la sesion local del usuario en SharedPreferences
object SessionManager {

    // El valor definitivo esta pendiente de confirmar con el cliente (RNF-20)
    const val INACTIVIDAD_MINUTOS = 30

    private const val PREFS = "api_movil_sesion"
    private const val KEY_TOKEN = "token"
    private const val KEY_USUARIO_ID = "usuario_id"
    private const val KEY_NOMBRE = "nombre"
    private const val KEY_CORREO = "correo"
    private const val KEY_ROL = "rol"
    private const val KEY_ULTIMA_ACTIVIDAD = "ultima_actividad"
    private const val KEY_MENSAJE_SALIDA = "mensaje_salida"

    private lateinit var prefs: SharedPreferences

    // Inicializa las preferencias compartidas
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    // Guarda los datos del usuario tras un login exitoso
    fun guardarSesion(token: String, usuarioId: Int, nombre: String, correo: String, rol: String?) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USUARIO_ID, usuarioId)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_CORREO, correo)
            .putString(KEY_ROL, rol)
            .apply()
        actualizarUltimaActividad()
    }

    // Actualiza la marca de tiempo de la ultima actividad del usuario
    fun actualizarUltimaActividad() {
        if (haySesion) {
            prefs.edit().putLong(KEY_ULTIMA_ACTIVIDAD, System.currentTimeMillis()).apply()
        }
    }

    // Comprueba si ha superado el tiempo limite de inactividad
    fun haExpiradoInactividad(): Boolean {
        if (!haySesion) return false
        val ultima = prefs.getLong(KEY_ULTIMA_ACTIVIDAD, 0L)
        if (ultima == 0L) {
            actualizarUltimaActividad()
            return false
        }
        val transcurridoMs = System.currentTimeMillis() - ultima
        val limiteMs = INACTIVIDAD_MINUTOS * 60 * 1000L
        return transcurridoMs > limiteMs
    }

    // Guarda un mensaje de salida (sesion expirada o inactividad) para mostrarlo en el login
    fun guardarMensajeSalida(mensaje: String) {
        prefs.edit().putString(KEY_MENSAJE_SALIDA, mensaje).apply()
    }

    // Obtiene y limpia el mensaje de salida pendiente
    fun obtenerYLimpiarMensajeSalida(): String? {
        val msg = prefs.getString(KEY_MENSAJE_SALIDA, null)
        if (msg != null) {
            prefs.edit().remove(KEY_MENSAJE_SALIDA).apply()
        }
        return msg
    }

    // Llama al endpoint POST /logout del servidor y luego borra la sesion local
    suspend fun cerrarSesionRemotaYLocal() {
        try {
            ApiClient.api.logout()
        } catch (_: Exception) {
            // Si falla por red o token expirado, igual se limpia la sesion local
        }
        cerrarSesion()
    }

    // Limpia los datos almacenados al cerrar sesion
    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }

    val token: String? get() = prefs.getString(KEY_TOKEN, null)
    val usuarioId: Int get() = prefs.getInt(KEY_USUARIO_ID, -1)
    val nombre: String? get() = prefs.getString(KEY_NOMBRE, null)
    val correo: String? get() = prefs.getString(KEY_CORREO, null)
    val rol: String? get() = prefs.getString(KEY_ROL, null)

    val haySesion: Boolean get() = !token.isNullOrBlank()
    val esAdmin: Boolean get() = rol == "admin"
    val esContabilidad: Boolean get() = rol == "contabilidad"
    val esConductor: Boolean get() = rol == "conductor"
    val puedeConsultar: Boolean get() = esAdmin || esContabilidad
}

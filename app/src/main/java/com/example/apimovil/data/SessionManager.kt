package com.example.apimovil.data

import android.content.Context
import android.content.SharedPreferences

// Administra la sesion local del usuario en SharedPreferences
object SessionManager {

    private const val PREFS = "api_movil_sesion"
    private const val KEY_TOKEN = "token"
    private const val KEY_USUARIO_ID = "usuario_id"
    private const val KEY_NOMBRE = "nombre"
    private const val KEY_CORREO = "correo"
    private const val KEY_ROL = "rol"

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
    }

    // Limpia los datos almacenados al cerrar sesion
    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }

    val token: String? get() = prefs.getString(KEY_TOKEN, null)
    val nombre: String? get() = prefs.getString(KEY_NOMBRE, null)
    val correo: String? get() = prefs.getString(KEY_CORREO, null)
    val rol: String? get() = prefs.getString(KEY_ROL, null)

    val haySesion: Boolean get() = !token.isNullOrBlank()
    val esAdmin: Boolean get() = rol == "admin"
    val esContabilidad: Boolean get() = rol == "contabilidad"
    val esConductor: Boolean get() = rol == "conductor"
    val puedeConsultar: Boolean get() = esAdmin || esContabilidad
}

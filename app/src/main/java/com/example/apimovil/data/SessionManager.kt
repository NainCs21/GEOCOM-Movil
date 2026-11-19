package com.example.apimovil.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Administra la sesion local del usuario de forma segura con EncryptedSharedPreferences
object SessionManager {

    private const val PREFS = "api_movil_sesion"
    private const val KEY_TOKEN = "token"
    private const val KEY_USUARIO_ID = "usuario_id"
    private const val KEY_NOMBRE = "nombre"
    private const val KEY_CORREO = "correo"
    private const val KEY_ROL = "rol"
    private const val KEY_MENSAJE_SALIDA = "mensaje_salida"

    private lateinit var prefs: SharedPreferences

    // Inicializa las preferencias compartidas cifradas con migración y fallback
    fun init(context: Context) {
        val appContext = context.applicationContext
        try {
            val regularPrefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val oldToken = regularPrefs.getString(KEY_TOKEN, null)

            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedPrefs = EncryptedSharedPreferences.create(
                appContext,
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            if (!oldToken.isNullOrBlank() && encryptedPrefs.getString(KEY_TOKEN, null).isNullOrBlank()) {
                val usuarioId = regularPrefs.getInt(KEY_USUARIO_ID, -1)
                val nombre = regularPrefs.getString(KEY_NOMBRE, null)
                val correo = regularPrefs.getString(KEY_CORREO, null)
                val rol = regularPrefs.getString(KEY_ROL, null)
                val mensajeSalida = regularPrefs.getString(KEY_MENSAJE_SALIDA, null)

                val editor = encryptedPrefs.edit()
                editor.putString(KEY_TOKEN, oldToken)
                if (usuarioId != -1) editor.putInt(KEY_USUARIO_ID, usuarioId)
                if (nombre != null) editor.putString(KEY_NOMBRE, nombre)
                if (correo != null) editor.putString(KEY_CORREO, correo)
                if (rol != null) editor.putString(KEY_ROL, rol)
                if (mensajeSalida != null) editor.putString(KEY_MENSAJE_SALIDA, mensajeSalida)
                editor.apply()

                regularPrefs.edit().clear().apply()
            }
            prefs = encryptedPrefs
        } catch (_: Exception) {
            prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
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

    // Actualiza los datos del usuario sin modificar el token
    fun actualizarUsuario(nombre: String, correo: String, rol: String?) {
        prefs.edit()
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_CORREO, correo)
            .putString(KEY_ROL, rol)
            .apply()
    }

    // Guarda un mensaje de salida (sesion expirada) para mostrarlo en el login
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

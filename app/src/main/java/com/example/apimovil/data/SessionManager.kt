package com.example.apimovil.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore

// Administra la sesion local del usuario de forma segura con EncryptedSharedPreferences
object SessionManager {

    const val PREFS_LEGACY = "api_movil_sesion"
    const val PREFS_SEGURO = "api_movil_sesion_seg"
    private const val KEY_TOKEN = "token"
    private const val KEY_USUARIO_ID = "usuario_id"
    private const val KEY_NOMBRE = "nombre"
    private const val KEY_CORREO = "correo"
    private const val KEY_ROL = "rol"
    private const val KEY_MENSAJE_SALIDA = "mensaje_salida"

    @Volatile
    private var iniciado = false
    private lateinit var prefs: SharedPreferences
    private var esSeguro = false

    // Inicializa las preferencias compartidas cifradas con migración y fallback
    fun init(context: Context) {
        if (iniciado) return
        synchronized(this) {
            if (iniciado) return
            val appContext = context.applicationContext

            var createdPrefs: SharedPreferences? = null
            try {
                createdPrefs = crearEncryptedPrefs(appContext)
                esSeguro = true
            } catch (e: Exception) {
                if (e is GeneralSecurityException || e is IOException || e is RuntimeException) {
                    Log.e("GeoComSession", "falló el almacén cifrado", e)
                    borrarAlmacenCifradoYClave(appContext)
                    try {
                        createdPrefs = crearEncryptedPrefs(appContext)
                        esSeguro = true
                    } catch (e2: Exception) {
                        Log.e("GeoComSession", "falló el reintento del almacén cifrado, usando fallback legacy", e2)
                        createdPrefs = appContext.getSharedPreferences(PREFS_LEGACY, Context.MODE_PRIVATE)
                        esSeguro = false
                    }
                } else {
                    createdPrefs = appContext.getSharedPreferences(PREFS_LEGACY, Context.MODE_PRIVATE)
                    esSeguro = false
                }
            }

            prefs = createdPrefs

            if (esSeguro) {
                migrarDeLegacyASeguro(appContext, prefs)
            }

            iniciado = true
            Log.i("GeoComSession", "init ok seguro=$esSeguro haySesion=$haySesion")
        }
    }

    private fun crearEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_SEGURO,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun borrarAlmacenCifradoYClave(context: Context) {
        try {
            context.deleteSharedPreferences(PREFS_SEGURO)
            context.deleteSharedPreferences("__androidx_security_crypto_encrypted_prefs__")
        } catch (e: Exception) {
            Log.e("GeoComSession", "falló al borrar archivos de SharedPreferences cifradas", e)
        }
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry("_androidx_security_master_key_")
        } catch (e: Exception) {
            Log.e("GeoComSession", "falló al borrar entrada de AndroidKeyStore", e)
        }
    }

    private fun migrarDeLegacyASeguro(context: Context, securePrefs: SharedPreferences) {
        val legacyPrefs = context.getSharedPreferences(PREFS_LEGACY, Context.MODE_PRIVATE)
        val legacyToken = legacyPrefs.getString(KEY_TOKEN, null)
        val secureToken = securePrefs.getString(KEY_TOKEN, null)

        if (!legacyToken.isNullOrBlank() && secureToken.isNullOrBlank()) {
            val usuarioId = legacyPrefs.getInt(KEY_USUARIO_ID, -1)
            val nombre = legacyPrefs.getString(KEY_NOMBRE, null)
            val correo = legacyPrefs.getString(KEY_CORREO, null)
            val rol = legacyPrefs.getString(KEY_ROL, null)
            val mensajeSalida = legacyPrefs.getString(KEY_MENSAJE_SALIDA, null)

            val editor = securePrefs.edit()
            editor.putString(KEY_TOKEN, legacyToken)
            if (usuarioId != -1) editor.putInt(KEY_USUARIO_ID, usuarioId)
            if (nombre != null) editor.putString(KEY_NOMBRE, nombre)
            if (correo != null) editor.putString(KEY_CORREO, correo)
            if (rol != null) editor.putString(KEY_ROL, rol)
            if (mensajeSalida != null) editor.putString(KEY_MENSAJE_SALIDA, mensajeSalida)
            editor.apply()

            val tokenLeido = securePrefs.getString(KEY_TOKEN, null)
            if (!tokenLeido.isNullOrBlank()) {
                legacyPrefs.edit().clear().apply()
            }
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

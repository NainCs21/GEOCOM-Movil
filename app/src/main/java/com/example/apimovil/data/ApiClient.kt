package com.example.apimovil.data

import android.util.Log
import com.example.apimovil.BuildConfig
import com.example.apimovil.data.models.Gasolinera
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// URL base del servicio API en la nube
const val BASE_URL = "https://geocom-production.up.railway.app/api/"

// Interceptor para agregar el token de autorizacion en las peticiones
private class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder().addHeader("Accept", "application/json")
        val path = original.url.encodedPath
        if (!path.endsWith("/login")) {
            val token = SessionManager.token
            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
        }
        return chain.proceed(requestBuilder.build())
    }
}

// Interceptor para detectar respuestas 401 fuera de login/logout y cerrar sesion localmente
private class SessionExpiredInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val response = chain.proceed(original)
        if (response.code == 401 && SessionManager.haySesion) {
            val path = original.url.encodedPath
            if (!path.endsWith("/login") && !path.endsWith("/logout") && !path.endsWith("/verificar-contrasena")) {
                Log.w("GeoComSync", "401 en ${original.url.encodedPath}: se cierra la sesión local")
                SessionManager.cerrarSesion()
                runBlocking {
                    SesionEventos.notificarSesionExpirada("Tu sesión expiró. Inicia sesión de nuevo.")
                }
            }
        }
        return response
    }
}

// Adapter especial para deserializar gasolineras en formato texto u objeto JSON
private val deserializadorGasolinera = JsonDeserializer { json, _, _ ->
    if (json.isJsonPrimitive) {
        Gasolinera(id = 0, nombre = json.asString, direccion = null, codigo_qr = "", activo = true)
    } else {
        val obj = json.asJsonObject
        Gasolinera(
            id = obj.get("id")?.asInt ?: 0,
            nombre = obj.get("nombre")?.asString ?: "",
            direccion = obj.get("direccion")?.takeIf { !it.isJsonNull }?.asString,
            codigo_qr = obj.get("codigo_qr")?.asString ?: "",
            activo = obj.get("activo")?.asBoolean ?: true
        )
    }
}

// Configura y provee la instancia unica de Retrofit
object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor())
        .addInterceptor(SessionExpiredInterceptor())
        .addInterceptor(logging)
        .build()

    private val gson = GsonBuilder()
        .registerTypeAdapter(Gasolinera::class.java, deserializadorGasolinera)
        .create()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApiService::class.java)
}

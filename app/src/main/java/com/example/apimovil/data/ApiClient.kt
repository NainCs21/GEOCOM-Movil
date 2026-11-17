package com.example.apimovil.data

import com.example.apimovil.BuildConfig
import com.example.apimovil.data.models.Gasolinera
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// URL base del servicio API en la nube
private const val BASE_URL = "https://geocom-production.up.railway.app/api/"

// Interceptor para agregar el token de autorizacion en las peticiones
private class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.url.encodedPath.endsWith("/login")) {
            return chain.proceed(original)
        }
        val token = SessionManager.token
        val requestBuilder = original.newBuilder().addHeader("Accept", "application/json")
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
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

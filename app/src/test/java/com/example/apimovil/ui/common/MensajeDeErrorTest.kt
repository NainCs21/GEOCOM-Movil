package com.example.apimovil.ui.common

import com.google.gson.JsonParseException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException

class MensajeDeErrorTest {

    private fun crearHttpException(codigo: Int, jsonCuerpo: String): HttpException {
        val responseBody = jsonCuerpo.toResponseBody("application/json".toMediaType())
        val okHttpResponse = okhttp3.Response.Builder()
            .code(codigo)
            .message("HTTP $codigo")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://geocom-production.up.railway.app/api/test").build())
            .build()
        val retrofitResponse = Response.error<Any>(responseBody, okHttpResponse)
        return HttpException(retrofitResponse)
    }

    @Test
    fun error_404_con_mensaje_en_espanol() {
        val e = crearHttpException(404, "{\"mensaje\":\"Orden no encontrada.\"}")
        val interp = interpretarError(e)
        assertEquals(404, interp.codigo)
        assertEquals("Orden no encontrada.", interp.mensaje)
    }

    @Test
    fun error_403_con_mensaje_en_ingles_usa_fallback() {
        val e = crearHttpException(403, "{\"message\":\"This action is unauthorized.\"}")
        val interp = interpretarError(e)
        assertEquals(403, interp.codigo)
        assertEquals("No tienes permiso para hacer esto.", interp.mensaje)
    }

    @Test
    fun error_422_con_errores_por_campo() {
        val json = """
            {
                "message": "The given data was invalid.",
                "errors": {
                    "correo": ["The correo has already been taken."],
                    "placa": ["The placa field is required."]
                }
            }
        """.trimIndent()
        val e = crearHttpException(422, json)
        val interp = interpretarError(e)
        assertEquals(422, interp.codigo)
        assertEquals("Ya existe una cuenta con ese correo.", interp.camposError["correo"])
        assertEquals("El campo placa no es válido.", interp.camposError["placa"])
    }

    @Test
    fun error_500_con_html_no_muestra_detalle() {
        val html = "<html><body><h1>Fatal Error in Laravel</h1></body></html>"
        val responseBody = html.toResponseBody("text/html".toMediaType())
        val okHttpResponse = okhttp3.Response.Builder()
            .code(500)
            .message("Internal Server Error")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://geocom-production.up.railway.app/api/test").build())
            .build()
        val retrofitResponse = Response.error<Any>(responseBody, okHttpResponse)
        val e = HttpException(retrofitResponse)

        val interp = interpretarError(e)
        assertEquals(500, interp.codigo)
        assertEquals("El servidor tuvo un problema. Intenta de nuevo en unos minutos.", interp.mensaje)
    }

    @Test
    fun error_conexion_sin_internet() {
        val e = ConnectException("Failed to connect")
        val interp = interpretarError(e)
        assertEquals("No hay conexión a internet. Revísala e intenta de nuevo.", interp.mensaje)
    }

    @Test
    fun error_json_invalido() {
        val e = JsonParseException("Syntax error")
        val interp = interpretarError(e)
        assertEquals("El servidor respondió algo inesperado. Intenta de nuevo.", interp.mensaje)
    }

    @Test
    fun error_desconocido_no_filtra_clase() {
        val e = RuntimeException("NullPointer in internal class com.example.Foo")
        val interp = interpretarError(e)
        assertEquals("Ocurrió un error inesperado. Intenta de nuevo.", interp.mensaje)
    }
}

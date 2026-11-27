package com.example.apimovil.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadoresTest {

    @Test
    fun correo_valido() {
        val res = Validadores.correo(" usuario@empresa.com ")
        assertTrue(res.esOk)
        assertEquals("usuario@empresa.com", res.valorOrNull())
    }

    @Test
    fun correo_vacio() {
        val res = Validadores.correo("")
        assertFalse(res.esOk)
        assertEquals("Escribe tu correo.", res.mensajeError())
    }

    @Test
    fun correo_sin_arroba() {
        val res = Validadores.correo("usuarioempresa.com")
        assertFalse(res.esOk)
        assertEquals("Escribe un correo válido, por ejemplo nombre@empresa.com.", res.mensajeError())
    }

    @Test
    fun correo_con_espacios() {
        val res = Validadores.correo("usuario @empresa.com")
        assertFalse(res.esOk)
        assertEquals("El correo no puede tener espacios.", res.mensajeError())
    }

    @Test
    fun correo_con_doble_punto() {
        val res = Validadores.correo("usuario..nombre@empresa.com")
        assertFalse(res.esOk)
        assertEquals("Escribe un correo válido, por ejemplo nombre@empresa.com.", res.mensajeError())
    }

    @Test
    fun correo_excede_254_caracteres() {
        val largo = "a".repeat(250) + "@empresa.com"
        val res = Validadores.correo(largo)
        assertFalse(res.esOk)
        assertEquals("El correo no puede pasar de 254 caracteres.", res.mensajeError())
    }

    @Test
    fun contrasenaLogin_valida() {
        val res = Validadores.contrasenaLogin("  Secret123!  ")
        assertTrue(res.esOk)
        assertEquals("  Secret123!  ", res.valorOrNull())
    }

    @Test
    fun contrasenaLogin_vacia() {
        val res = Validadores.contrasenaLogin("")
        assertFalse(res.esOk)
        assertEquals("Escribe tu contraseña.", res.mensajeError())
    }

    @Test
    fun contrasenaNueva_edicion_vacia_es_ok() {
        val res = Validadores.contrasenaNueva("", esEdicion = true)
        assertTrue(res.esOk)
        assertNull(res.valorOrNull())
    }

    @Test
    fun contrasenaNueva_corta() {
        val res = Validadores.contrasenaNueva("12345")
        assertFalse(res.esOk)
        assertEquals("La contraseña debe tener entre 8 y 128 caracteres.", res.mensajeError())
    }

    @Test
    fun contrasenaNueva_espacio_al_inicio() {
        val res = Validadores.contrasenaNueva(" 12345678")
        assertFalse(res.esOk)
        assertEquals("La contraseña no puede empezar ni terminar con espacios.", res.mensajeError())
    }

    @Test
    fun nombrePersona_valido() {
        val res = Validadores.nombrePersona("María José O'Brien-Pérez")
        assertTrue(res.esOk)
        assertEquals("María José O'Brien-Pérez", res.valorOrNull())
    }

    @Test
    fun nombrePersona_con_digitos_es_invalido() {
        val res1 = Validadores.nombrePersona("Juan3")
        assertFalse(res1.esOk)
        val res2 = Validadores.nombrePersona("R2D2")
        assertFalse(res2.esOk)
    }

    @Test
    fun telefono_honduras_valido() {
        val res = Validadores.telefono("+504 9999-9999")
        assertTrue(res.esOk)
        assertEquals("99999999", res.valorOrNull())
    }

    @Test
    fun telefono_honduras_invalido() {
        val res = Validadores.telefono("12345")
        assertFalse(res.esOk)
        assertEquals("Escribe un teléfono de 8 dígitos, por ejemplo 9999-9999.", res.mensajeError())
    }

    @Test
    fun placa_valida() {
        val res = Validadores.placa("h aa-1234")
        assertTrue(res.esOk)
        assertEquals("H AA-1234", res.valorOrNull())
    }

    @Test
    fun placa_invalida() {
        val res = Validadores.placa("AB@123")
        assertFalse(res.esOk)
        assertEquals("La placa solo puede tener letras, números, espacio y guion.", res.mensajeError())
    }

    @Test
    fun anio_valido_e_invalido() {
        assertTrue(Validadores.anio("2024").esOk)
        assertFalse(Validadores.anio("1979").esOk)
        assertFalse(Validadores.anio("2050").esOk)
    }

    @Test
    fun rendimiento_valido_e_invalido() {
        assertTrue(Validadores.rendimiento("8,5").esOk)
        assertEquals(8.5, Validadores.rendimiento("8.5").valorOrNull()!!, 0.001)
        assertFalse(Validadores.rendimiento("8.555").esOk)
        assertFalse(Validadores.rendimiento("120").esOk)
    }

    @Test
    fun monto_rechaza_basura_y_cientifica() {
        assertFalse(Validadores.monto("1e5").esOk)
        assertFalse(Validadores.monto("NaN").esOk)
        assertFalse(Validadores.monto("Infinity").esOk)
        assertFalse(Validadores.monto("5d").esOk)
        assertFalse(Validadores.monto("-5").esOk)
        assertFalse(Validadores.monto("0").esOk)
        assertFalse(Validadores.monto("100000000").esOk)
        assertTrue(Validadores.monto("500,50").esOk)
        assertEquals(500.5, Validadores.monto("500.50").valorOrNull()!!, 0.001)
    }

    @Test
    fun textoLibre_rechaza_ataques_sql_xss_y_path_traversal() {
        val ataques = listOf(
            "' OR '1'='1",
            "; DROP TABLE users;--",
            "admin'--",
            "1; SELECT * FROM users",
            "UNION SELECT null,null--",
            "\" OR \"\"=\"",
            "<script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "javascript:alert(1)",
            "../../etc/passwd",
            "texto con \u0000 nulo",
            "texto con \u202E unicode invertido \u200B",
            "   ",
            "!!!"
        )

        for (ataque in ataques) {
            val res = Validadores.textoLibre(ataque, "actividad", 5, 1000)
            assertFalse("Debe rechazar: $ataque", res.esOk)
        }
    }

    @Test
    fun textoLibre_acepta_texto_legitimo_con_apostrofe_y_coma() {
        val textoLegitimo = "Reparación del motor, llantas y frenos de la O'Brien"
        val res = Validadores.textoLibre(textoLegitimo, "actividad", 5, 1000)
        assertTrue(res.esOk)
        assertEquals(textoLegitimo, res.valorOrNull())
    }

    @Test
    fun esUrlDeApi_validaciones_estrictas() {
        val base = "https://geocom-production.up.railway.app/api/"
        assertTrue(esUrlDeApi("https://geocom-production.up.railway.app/api/foto.jpg", base))
        assertFalse("http no permitido", esUrlDeApi("http://geocom-production.up.railway.app/api/foto.jpg", base))
        assertFalse("subdominio falso", esUrlDeApi("https://geocom-production.up.railway.app.evil.com/foto.jpg", base))
        assertFalse("query string engañoso", esUrlDeApi("https://evil.com/?geocom-production.up.railway.app", base))
        assertFalse("userinfo engañoso", esUrlDeApi("https://geocom-production.up.railway.app@evil.com/foto.jpg", base))
        assertFalse("puerto distinto", esUrlDeApi("https://geocom-production.up.railway.app:8443/foto.jpg", base))
        assertFalse("vacio", esUrlDeApi("", base))
    }

    @Test
    fun nombreArchivoSeguro_sanitizacion() {
        assertEquals("etc_passwd", nombreArchivoSeguro("../../etc/passwd"))
        assertEquals("ORD-2026_001", nombreArchivoSeguro("ORD-2026/001"))
        assertEquals("a_b_.pdf", nombreArchivoSeguro("a b?.pdf"))
        assertEquals("archivo", nombreArchivoSeguro("....."))
        assertEquals("archivo", nombreArchivoSeguro(""))
        assertEquals("a".repeat(60), nombreArchivoSeguro("a".repeat(500)))
    }

    @Test
    fun idValido_y_tipos() {
        assertTrue(Validadores.idValido(1))
        assertFalse(Validadores.idValido(0))
        assertFalse(Validadores.idValido(-1))
        assertFalse(Validadores.idValido(null))

        assertTrue(Validadores.tipoEvidenciaValido("bomba"))
        assertFalse(Validadores.tipoEvidenciaValido("desconocido"))

        assertTrue(Validadores.tipoDestinoValido("vehiculo"))
        assertFalse(Validadores.tipoDestinoValido("invalido"))
    }
}

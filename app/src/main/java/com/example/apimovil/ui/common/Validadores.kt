package com.example.apimovil.ui.common

import android.text.InputFilter
import java.math.BigDecimal
import java.util.Calendar
import java.util.regex.Pattern

sealed class Resultado<out T> {
    data class Ok<out T>(val valor: T) : Resultado<T>()
    data class Error(val mensaje: String) : Resultado<Nothing>()

    val esOk: Boolean get() = this is Ok
    fun mensajeError(): String? = (this as? Error)?.mensaje
    fun valorOrNull(): T? = (this as? Ok)?.valor
}

object Validadores {

    fun contieneControlChars(input: String?, esMultilinea: Boolean = false): Boolean {
        if (input == null) return false
        for (ch in input) {
            val code = ch.code
            if (esMultilinea && (code == 0x0A || code == 0x09)) continue
            if (code in 0x0000..0x001F || code == 0x007F) return true
            if (code in 0x200B..0x200F || code in 0x202A..0x202E || code in 0x2066..0x2069 || code == 0xFEFF) return true
        }
        return false
    }

    // Normalizacion estandar previa a validacion
    fun normalizarTexto(input: String?, esMultilinea: Boolean = false): String {
        if (input == null) return ""
        val sinControles = StringBuilder()
        for (ch in input) {
            val code = ch.code
            if (esMultilinea && (code == 0x0A || code == 0x09)) {
                sinControles.append(ch)
                continue
            }
            if (code in 0x0000..0x001F || code == 0x007F) continue
            if (code in 0x200B..0x200F || code in 0x202A..0x202E || code in 0x2066..0x2069 || code == 0xFEFF) continue
            sinControles.append(ch)
        }
        val limpio = sinControles.toString().trim()
        return if (esMultilinea) {
            limpio.replace(Regex("[ \\t]+"), " ")
        } else {
            limpio.replace(Regex("\\s+"), " ")
        }
    }

    // Patrones de ataque en texto libre
    private val PATRONES_ATAQUE = listOf(
        Pattern.compile("\\b(union\\s+select|drop\\s+table|insert\\s+into|delete\\s+from|update\\s+\\w+\\s+set|select\\s+.+\\s+from|information_schema|xp_cmdshell|sleep\\s*\\(|benchmark\\s*\\()", Pattern.CASE_INSENSITIVE),
        Pattern.compile("['\"]\\s*(or|and)\\s+['\"]?\\w*['\"]?\\s*=\\s*['\"]?\\w*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(";\\s*(drop|select|insert|update|delete)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("/\\*|\\*/"),
        Pattern.compile("--\\s*$"),
        Pattern.compile("[<>]"),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bon\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data:text/html", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\./"),
        Pattern.compile("%2e%2e", Pattern.CASE_INSENSITIVE)
    )

    fun contieneAtaque(texto: String): Boolean {
        for (patron in PATRONES_ATAQUE) {
            if (patron.matcher(texto).find()) return true
        }
        return false
    }

    fun correo(input: String?): Resultado<String> {
        val raw = input?.trim().orEmpty()
        if (raw.isEmpty()) return Resultado.Error("Escribe tu correo.")
        if (raw.contains(" ")) return Resultado.Error("El correo no puede tener espacios.")
        if (raw.length > 254) return Resultado.Error("El correo no puede pasar de 254 caracteres.")
        if (raw.contains("..")) return Resultado.Error("Escribe un correo válido, por ejemplo nombre@empresa.com.")

        val regex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        if (!regex.matches(raw)) {
            return Resultado.Error("Escribe un correo válido, por ejemplo nombre@empresa.com.")
        }
        return Resultado.Ok(raw.lowercase())
    }

    fun contrasenaLogin(input: String?): Resultado<String> {
        val raw = input.orEmpty()
        if (raw.isEmpty()) return Resultado.Error("Escribe tu contraseña.")
        if (raw.length > 128) return Resultado.Error("La contraseña no puede pasar de 128 caracteres.")
        return Resultado.Ok(raw)
    }

    fun contrasenaNueva(input: String?, esEdicion: Boolean = false): Resultado<String?> {
        val raw = input.orEmpty()
        if (esEdicion && raw.isEmpty()) return Resultado.Ok(null)
        if (raw.startsWith(" ") || raw.endsWith(" ")) {
            return Resultado.Error("La contraseña no puede empezar ni terminar con espacios.")
        }
        if (raw.length < 8 || raw.length > 128) {
            return Resultado.Error("La contraseña debe tener entre 8 y 128 caracteres.")
        }
        return Resultado.Ok(raw)
    }

    fun nombrePersona(input: String?): Resultado<String> {
        val norm = normalizarTexto(input)
        if (norm.isEmpty()) return Resultado.Error("Ingresa el nombre.")
        if (norm.length < 2) return Resultado.Error("El nombre es muy corto, mínimo 2 letras.")
        if (norm.length > 100) return Resultado.Error("El nombre no puede pasar de 100 caracteres.")

        val regex = Regex("^[\\p{L}][\\p{L} .'-]*$")
        if (!regex.matches(norm)) {
            return Resultado.Error("El nombre solo puede tener letras, espacios, punto, apóstrofo y guion.")
        }
        return Resultado.Ok(norm)
    }

    fun telefono(input: String?): Resultado<String> {
        val norm = normalizarTexto(input)
        if (norm.isEmpty()) return Resultado.Error("Ingresa el teléfono.")

        val permitidosRegex = Regex("^[0-9 \\-+()]+$")
        if (!permitidosRegex.matches(norm)) {
            return Resultado.Error("Escribe un teléfono de 8 dígitos, por ejemplo 9999-9999.")
        }

        var soloDigitos = norm.replace(Regex("[^0-9]"), "")
        if (soloDigitos.startsWith("504") && soloDigitos.length == 11) {
            soloDigitos = soloDigitos.substring(3)
        }

        if (soloDigitos.length != 8) {
            return Resultado.Error("Escribe un teléfono de 8 dígitos, por ejemplo 9999-9999.")
        }
        return Resultado.Ok(soloDigitos)
    }

    fun cargo(input: String?): Resultado<String> {
        val norm = normalizarTexto(input)
        if (norm.isEmpty()) return Resultado.Error("Ingresa el cargo.")
        if (norm.length > 100) return Resultado.Error("El cargo no puede pasar de 100 caracteres.")

        val regex = Regex("^[\\p{L} .-]+$")
        if (!regex.matches(norm)) {
            return Resultado.Error("El cargo solo puede tener letras, espacios, punto y guion.")
        }
        return Resultado.Ok(norm)
    }

    fun placa(input: String?): Resultado<String> {
        val norm = normalizarTexto(input).uppercase()
        if (norm.isEmpty()) return Resultado.Error("Ingresa la placa.")
        if (norm.length < 4 || norm.length > 15) {
            return Resultado.Error("La placa debe tener entre 4 y 15 caracteres.")
        }

        val regex = Regex("^[A-Z0-9 -]+$")
        if (!regex.matches(norm)) {
            return Resultado.Error("La placa solo puede tener letras, números, espacio y guion.")
        }
        return Resultado.Ok(norm)
    }

    fun marcaModelo(input: String?, campoNombre: String = "la marca", maxLargo: Int = 50): Resultado<String> {
        val norm = normalizarTexto(input)
        if (norm.isEmpty()) return Resultado.Error("Ingresa $campoNombre.")
        if (norm.length > maxLargo) return Resultado.Error("Ingresa $campoNombre válida (máximo $maxLargo caracteres).")

        val regex = Regex("^[a-zA-Z0-9\\p{L}][a-zA-Z0-9\\p{L} .\\-/+]*$")
        if (!regex.matches(norm)) {
            return Resultado.Error("Campo $campoNombre solo puede tener letras, números y . - / +")
        }
        return Resultado.Ok(norm)
    }

    fun anio(input: String?): Resultado<Int?> {
        val norm = input?.trim().orEmpty()
        if (norm.isEmpty()) return Resultado.Ok(null)

        val anioVal = norm.toIntOrNull()
        val anioMax = Calendar.getInstance().get(Calendar.YEAR) + 1
        if (anioVal == null || norm.length != 4 || anioVal !in 1980..anioMax) {
            return Resultado.Error("El año debe tener 4 dígitos y estar entre 1980 y $anioMax.")
        }
        return Resultado.Ok(anioVal)
    }

    fun rendimiento(input: String?): Resultado<Double?> {
        val norm = input?.trim()?.replace(',', '.').orEmpty()
        if (norm.isEmpty()) return Resultado.Ok(null)

        val regex = Regex("^\\d{1,2}(\\.\\d{1,2})?$")
        if (!regex.matches(norm)) {
            if (norm.contains(".") && norm.substringAfter(".").length > 2) {
                return Resultado.Error("Usa máximo 2 decimales.")
            }
            return Resultado.Error("El rendimiento debe estar entre 0.1 y 99.9 km por litro.")
        }

        val valDouble = norm.toDoubleOrNull()
        if (valDouble == null || valDouble < 0.1 || valDouble > 99.9) {
            return Resultado.Error("El rendimiento debe estar entre 0.1 y 99.9 km por litro.")
        }
        return Resultado.Ok(valDouble)
    }

    fun nombreGasolinera(input: String?): Resultado<String> {
        val norm = normalizarTexto(input)
        if (norm.isEmpty()) return Resultado.Error("Ingresa el nombre de la gasolinera.")
        if (norm.length < 2) return Resultado.Error("El nombre es muy corto, mínimo 2 caracteres.")
        if (norm.length > 100) return Resultado.Error("El nombre no puede pasar de 100 caracteres.")

        val regex = Regex("^[\\p{L}0-9 .,\\-&'#()]+$")
        if (!regex.matches(norm)) {
            return Resultado.Error("El nombre solo puede tener letras, números, espacios y . , - & ' # ( )")
        }
        return Resultado.Ok(norm)
    }

    fun direccion(input: String?): Resultado<String?> {
        val norm = normalizarTexto(input)
        if (norm.isEmpty()) return Resultado.Ok(null)
        if (norm.length > 255) return Resultado.Error("La dirección no puede pasar de 255 caracteres.")

        val regex = Regex("^[\\p{L}0-9 .,\\-#/()]+$")
        if (!regex.matches(norm)) {
            return Resultado.Error("La dirección solo puede tener letras, números, espacios y . , - # / ( )")
        }
        return Resultado.Ok(norm)
    }

    fun codigoQrNuevo(input: String?): Resultado<String> {
        val norm = normalizarTexto(input)
        if (norm.isEmpty()) return Resultado.Error("Ingresa el código QR.")
        if (norm.length < 3 || norm.length > 100) {
            return Resultado.Error("El código QR debe tener entre 3 y 100 caracteres.")
        }

        val regex = Regex("^[a-zA-Z0-9_\\-:.]+$")
        if (!regex.matches(norm)) {
            return Resultado.Error("El código QR solo puede tener letras, números y _ - : .")
        }
        return Resultado.Ok(norm)
    }

    fun codigoQrEscaneado(input: String?): Resultado<String> {
        val norm = normalizarTexto(input)
        if (norm.isEmpty() || norm.length > 100) {
            return Resultado.Error("Este código QR no es de una gasolinera de GEOCOM. Intenta con el QR de la estación.")
        }
        return Resultado.Ok(norm)
    }

    fun textoLibre(
        input: String?,
        campoNombre: String,
        minLargo: Int,
        maxLargo: Int,
        esObligatorio: Boolean = true
    ): Resultado<String?> {
        val raw = input.orEmpty()
        if (contieneControlChars(raw, esMultilinea = true) || contieneAtaque(raw)) {
            return Resultado.Error("El texto contiene símbolos o frases no permitidas. Quítalas e intenta de nuevo.")
        }

        val norm = normalizarTexto(input, esMultilinea = true)
        if (norm.isEmpty()) {
            return if (esObligatorio) Resultado.Error("Ingresa $campoNombre.") else Resultado.Ok(null)
        }
        if (norm.length < minLargo) {
            return Resultado.Error("Escribe al menos $minLargo caracteres.")
        }
        if (norm.length > maxLargo) {
            return Resultado.Error("La descripción no puede pasar de $maxLargo caracteres.")
        }
        if (!Regex("[\\p{L}0-9]").containsMatchIn(norm)) {
            return Resultado.Error("El texto contiene símbolos o frases no permitidas. Quítalas e intenta de nuevo.")
        }
        return Resultado.Ok(norm)
    }

    fun monto(input: String?, campoNombre: String = "monto"): Resultado<Double> {
        val norm = input?.trim()?.replace(',', '.').orEmpty()
        if (norm.isEmpty()) return Resultado.Error("Ingresa el $campoNombre.")

        val regex = Regex("^\\d{1,8}(\\.\\d{1,2})?$")
        if (!regex.matches(norm)) {
            if (norm.contains(".") && norm.substringAfter(".").length > 2) {
                return Resultado.Error("Usa máximo 2 decimales.")
            }
            return Resultado.Error("Ingresa un $campoNombre válido.")
        }

        val valBd = try {
            BigDecimal(norm)
        } catch (_: Exception) {
            return Resultado.Error("Ingresa un $campoNombre válido.")
        }

        if (valBd <= BigDecimal.ZERO) {
            return Resultado.Error("El $campoNombre debe ser mayor a L 0.00.")
        }
        if (valBd > BigDecimal("99999999.99")) {
            return Resultado.Error("El $campoNombre no puede pasar de L 99,999,999.99.")
        }

        return Resultado.Ok(valBd.toDouble())
    }

    fun horasVigencia(input: String?): Resultado<Int> {
        val norm = input?.trim().orEmpty()
        val valInt = norm.toIntOrNull()
        if (valInt == null || valInt !in 1..3) {
            return Resultado.Error("Las horas de vigencia deben ser 1, 2 o 3.")
        }
        return Resultado.Ok(valInt)
    }

    fun idValido(id: Int?): Boolean = id != null && id > 0

    fun tipoEvidenciaValido(tipo: String?): Boolean =
        tipo == "bomba" || tipo == "tablero" || tipo == "tanque" || tipo == "factura"

    fun tipoDestinoValido(tipo: String?): Boolean =
        tipo == "vehiculo" || tipo == "mgp" || tipo == "ambos"
}

object FiltrosEntrada {
    fun limiteYLargo(maxLargo: Int, regexPermitida: String? = null): Array<InputFilter> {
        val filtros = mutableListOf<InputFilter>()
        filtros.add(InputFilter.LengthFilter(maxLargo))
        if (regexPermitida != null) {
            val patron = Pattern.compile(regexPermitida)
            filtros.add(InputFilter { source, start, end, _, _, dstart ->
                if (dstart == 0 && source.isNotEmpty() && Character.isWhitespace(source[0])) {
                    return@InputFilter ""
                }
                for (i in start until end) {
                    val ch = source[i]
                    if (!patron.matcher(ch.toString()).matches()) {
                        return@InputFilter ""
                    }
                }
                null
            })
        }
        return filtros.toTypedArray()
    }
}

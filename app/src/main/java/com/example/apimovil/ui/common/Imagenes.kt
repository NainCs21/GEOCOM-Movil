package com.example.apimovil.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.example.apimovil.data.BASE_URL
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import kotlin.math.roundToInt

/**
 * Normaliza las URLs devueltas por la API para asegurar que utilicen HTTPS
 * si corresponden exactamente al host del servidor de la aplicacion.
 */
fun normalizarUrlApi(url: String, baseUrlStr: String = BASE_URL): String {
    if (url.isBlank()) return url
    val hostBase = try {
        URI(baseUrlStr).host?.lowercase()
    } catch (_: Exception) {
        "geocom-production.up.railway.app"
    }

    if (url.startsWith("http://", ignoreCase = true)) {
        val hostUrl = try {
            URI(url).host?.lowercase()
        } catch (_: Exception) {
            null
        }
        if (hostUrl != null && hostUrl == hostBase) {
            return "https://" + url.substring(7)
        }
    }
    return url
}

/**
 * Valida de forma estricta si una URL pertenece exactamente al servidor seguro de la API.
 */
fun esUrlDeApi(url: String, baseUrlStr: String = BASE_URL): Boolean {
    if (url.isBlank()) return false
    return try {
        val uri = URI(url)
        val baseUri = URI(baseUrlStr)

        val esHttps = uri.scheme?.equals("https", ignoreCase = true) == true
        val hostIgual = uri.host != null && uri.host.equals(baseUri.host, ignoreCase = true)
        val sinUserInfo = uri.userInfo.isNullOrBlank()
        val puertoIgual = uri.port == baseUri.port

        esHttps && hostIgual && sinUserInfo && puertoIgual
    } catch (_: Exception) {
        false
    }
}

/**
 * Sanitiza un texto para usarlo como nombre de archivo de forma segura.
 */
fun nombreArchivoSeguro(texto: String): String {
    if (texto.isBlank()) return "archivo"
    var limpio = texto.replace(Regex("[^A-Za-z0-9._-]"), "_")
    limpio = limpio.replace(Regex("^[._-]+"), "")
    if (limpio.length > 60) {
        limpio = limpio.substring(0, 60)
    }
    return if (limpio.isBlank()) "archivo" else limpio
}

/**
 * Comprime y redimensiona una imagen para reducir su tamano de archivo.
 */
fun comprimirFoto(origen: File, cacheDir: File?): File {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(origen.absolutePath, options)
    val origW = options.outWidth
    val origH = options.outHeight

    if (origW <= 0 || origH <= 0) {
        throw IllegalStateException("No se pudieron leer las dimensiones de la imagen.")
    }

    val maxDimension = 1600
    val maxSide = maxOf(origW, origH)
    var inSampleSize = 1
    while (maxSide / (inSampleSize * 2) >= maxDimension) {
        inSampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
    }

    var bitmap = BitmapFactory.decodeFile(origen.absolutePath, decodeOptions)
        ?: throw IllegalStateException("No se pudo decodificar la imagen.")

    try {
        val exif = ExifInterface(origen.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        if (degrees != 0f) {
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
                bitmap = rotated
            }
        }
    } catch (_: Exception) {}

    val currentMax = maxOf(bitmap.width, bitmap.height)
    if (currentMax > maxDimension) {
        val scale = maxDimension.toFloat() / currentMax
        val newW = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val newH = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        if (scaled != bitmap) {
            bitmap.recycle()
            bitmap = scaled
        }
    }

    val targetDir = cacheDir ?: origen.parentFile ?: File(origen.path).parentFile
    if (!targetDir.exists()) targetDir.mkdirs()

    val archivoSalida = File.createTempFile("evidencia_comp_", ".jpg", targetDir)
    var calidad = 80

    try {
        FileOutputStream(archivoSalida).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, out)
        }

        while (archivoSalida.length() > 5 * 1024 * 1024 && calidad >= 40) {
            calidad -= 10
            FileOutputStream(archivoSalida).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, out)
            }
        }
    } finally {
        bitmap.recycle()
    }

    if (archivoSalida.length() > 5 * 1024 * 1024) {
        archivoSalida.delete()
        throw IllegalStateException("La foto es demasiado pesada. Acércate al objetivo e intenta de nuevo.")
    }

    if (!esJpegValido(archivoSalida)) {
        archivoSalida.delete()
        throw IllegalStateException("El archivo de foto no es un JPEG válido.")
    }

    return archivoSalida
}

private fun esJpegValido(file: File): Boolean {
    if (!file.exists() || file.length() < 3) return false
    return try {
        FileInputStream(file).use { input ->
            val b1 = input.read()
            val b2 = input.read()
            val b3 = input.read()
            b1 == 0xFF && b2 == 0xD8 && b3 == 0xFF
        }
    } catch (_: Exception) {
        false
    }
}

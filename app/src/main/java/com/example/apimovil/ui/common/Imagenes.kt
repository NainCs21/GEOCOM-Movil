package com.example.apimovil.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.example.apimovil.data.BASE_URL
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import kotlin.math.roundToInt

/**
 * Normaliza las URLs devueltas por la API para asegurar que utilicen HTTPS
 * si corresponden al servidor de la aplicacion. Previene que OkHttp elimine
 * el encabezado de autorizacion al seguir redirecciones HTTP -> HTTPS.
 */
fun normalizarUrlApi(url: String): String {
    if (url.isBlank()) return url
    val hostBase = try {
        URI(BASE_URL).host
    } catch (_: Exception) {
        "geocom-production.up.railway.app"
    }
    return if (url.startsWith("http://", ignoreCase = true) && (hostBase != null && url.contains(hostBase, ignoreCase = true))) {
        "https://" + url.substring(7)
    } else {
        url
    }
}

/**
 * Comprime y redimensiona una imagen para reducir su tamano de archivo
 * antes de subirla al servidor, cumpliendo el requisito RNF-19.
 */
fun comprimirFoto(origen: File, cacheDir: File?): File {
    // 1. Leer dimensiones originales con inJustDecodeBounds = true
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(origen.absolutePath, options)
    val origW = options.outWidth
    val origH = options.outHeight

    if (origW <= 0 || origH <= 0) {
        throw IllegalStateException("No se pudieron leer las dimensiones de la imagen.")
    }

    // 2. Calcular inSampleSize para escalar la decodificacion y no saturar memoria
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

    // 3. Leer orientacion EXIF y rotar el bitmap si es necesario
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
    } catch (_: Exception) {
        // Se continua con el bitmap cargado si no se puede leer EXIF
    }

    // 4. Reducir el lado mas largo a un maximo de 1600 px manteniendo la proporcion
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

    // 5. Guardar como JPEG con calidad 80 e ir reduciendo de 10 en 10 hasta 50 si pesa mas de 1 MB
    val targetDir = cacheDir ?: origen.parentFile ?: File(origen.path).parentFile
    val archivoSalida = File.createTempFile("evidencia_comp_", ".jpg", targetDir)
    var calidad = 80

    try {
        FileOutputStream(archivoSalida).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, out)
        }

        while (archivoSalida.length() > 1024 * 1024 && calidad > 50) {
            calidad -= 10
            FileOutputStream(archivoSalida).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, out)
            }
        }
    } finally {
        bitmap.recycle()
    }

    return archivoSalida
}

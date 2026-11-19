package com.example.apimovil.ui.admin

import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.apimovil.R
import com.example.apimovil.data.ApiClient
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.models.DetalleOrden
import com.example.apimovil.data.models.Orden
import com.example.apimovil.databinding.DialogRehabilitarOrdenBinding
import com.example.apimovil.databinding.FragmentOrdenDetalleBinding
import com.example.apimovil.databinding.ItemLineaEvidenciaBinding
import com.example.apimovil.ui.common.colorDeEstado
import com.example.apimovil.ui.common.etiquetaDeEstado
import com.example.apimovil.ui.common.fondoBadgeDeEstado
import com.example.apimovil.ui.common.mensajeDeError
import com.example.apimovil.ui.common.normalizarUrlApi
import com.example.apimovil.ui.common.esCompletada
import com.example.apimovil.ui.common.esTarde
import com.example.apimovil.ui.common.generarFacturaPdf
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class OrdenDetalleAdminFragment : Fragment(R.layout.fragment_orden_detalle) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentOrdenDetalleBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentOrdenDetalleBinding.bind(view)
        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }

        val ordenId = arguments?.getInt("ordenId") ?: -1
        if (ordenId != -1) {
            cargarOrden(ordenId)
        }
    }

    private fun cargarOrden(ordenId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progress.isVisible = true
            try {
                val orden = withContext(Dispatchers.IO) { ApiClient.api.verOrden(ordenId) }
                binding.progress.isVisible = false
                pintarOrden(orden)
            } catch (e: Exception) {
                binding.progress.isVisible = false
                binding.textError.isVisible = true
                binding.textError.text = mensajeDeError(e)
            }
        }
    }

    private fun pintarOrden(orden: Orden) {
        binding.textNumeroOrden.text = getString(R.string.orden_numero, orden.numero_orden)

        binding.textEstado.text = etiquetaDeEstado(requireContext(), orden.estado)
        binding.textEstado.setBackgroundResource(fondoBadgeDeEstado(orden.estado))
        binding.textEstado.setTextColor(colorDeEstado(requireContext(), orden.estado))

        binding.contenedorContador.isVisible = false
        binding.contenedorProtegido.isVisible = false
        binding.contenedorDetalleRevelado.isVisible = true

        binding.textGasolinera.text = orden.gasolinera?.nombre ?: "—"
        binding.textDireccionGasolinera.text = orden.gasolinera?.direccion.orEmpty()

        val lineas = orden.detalles.orEmpty()
        val montoTotal = lineas.sumOf { it.monto }
        binding.textMontoTotal.text = getString(R.string.monto_total_lempiras, montoTotal)
        binding.textCantidadLineas.text = getString(R.string.distribuido_en_n_lineas, lineas.size)

        binding.contenedorLineas.removeAllViews()
        lineas.forEach { linea -> agregarVistaDeLinea(linea) }

        binding.btnRehabilitar.isVisible = orden.estado == "vencida"
        binding.btnRehabilitar.setOnClickListener {
            mostrarDialogoRehabilitar(orden.id)
        }

        if (esCompletada(orden.estado)) {
            binding.textMensajeFinal.isVisible = true
            binding.textMensajeFinal.text = if (orden.estado == "completada_tarde") {
                getString(R.string.orden_completada_tardio_mensaje)
            } else {
                getString(R.string.orden_completada_mensaje)
            }
        } else if (orden.estado == "vencida") {
            binding.textMensajeFinal.isVisible = true
            binding.textMensajeFinal.text = getString(R.string.orden_vencida_mensaje)
        } else {
            binding.textMensajeFinal.isVisible = false
        }

        if (!orden.justificacion.isNullOrBlank() && esTarde(orden.estado)) {
            binding.textJustificacionRehabilitacion.isVisible = true
            binding.textJustificacionRehabilitacion.text = getString(R.string.justificacion_conductor_label, orden.justificacion)
        } else if (orden.estado == "abierta_tarde" && orden.justificacion.isNullOrBlank()) {
            binding.textJustificacionRehabilitacion.isVisible = true
            binding.textJustificacionRehabilitacion.text = getString(R.string.conductor_aun_no_justificacion)
        } else {
            binding.textJustificacionRehabilitacion.isVisible = false
        }

        val abrirFacturaPdf = {
            viewLifecycleOwner.lifecycleScope.launch {
                binding.progress.isVisible = true
                Toast.makeText(requireContext(), "Generando factura PDF...", Toast.LENGTH_SHORT).show()
                val pdfFile = generarFacturaPdf(requireContext(), orden)
                binding.progress.isVisible = false

                if (pdfFile != null && pdfFile.exists()) {
                    try {
                        val uri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            pdfFile
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(intent, "Compartir factura de orden"))
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), "No se pudo generar la factura PDF", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "No se pudo generar la factura PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnExportarPdf.isVisible = true
        binding.btnExportarPdf.setOnClickListener { abrirFacturaPdf() }

        binding.btnVerFacturaPdf.isVisible = true
        binding.btnVerFacturaPdf.setOnClickListener { abrirFacturaPdf() }
    }

    private fun mostrarDialogoRehabilitar(ordenId: Int) {
        val dialogBinding = DialogRehabilitarOrdenBinding.inflate(LayoutInflater.from(requireContext()))
        var jobRehabilitacion: Job? = null
        var isEnviando = false

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.confirmar, null)
            .setNegativeButton(R.string.cancelar, null)
            .create()

        dialog.setOnDismissListener {
            jobRehabilitacion?.cancel()
            model.limpiarRehabilitacion()
        }

        dialog.setOnShowListener {
            val btnConfirmar = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            btnConfirmar.setOnClickListener {
                if (isEnviando) return@setOnClickListener

                val horasStr = dialogBinding.inputHoras.text?.toString()?.trim().orEmpty()
                val horas = horasStr.toIntOrNull()

                if (horas == null || horas !in 1..3) {
                    dialogBinding.inputHoras.error = getString(R.string.error_horas_vigencia)
                    return@setOnClickListener
                } else {
                    dialogBinding.inputHoras.error = null
                }

                isEnviando = true
                dialogBinding.progressDialog.isVisible = true
                dialogBinding.textErrorDialog.isVisible = false
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = false
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.isEnabled = false
                dialogBinding.inputHoras.isEnabled = false

                model.rehabilitarOrden(ordenId, horas) {
                    // Success callback
                }
            }
        }

        jobRehabilitacion = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.rehabilitacion.collect { estado ->
                    if (estado.loading) {
                        dialogBinding.progressDialog.isVisible = true
                    } else if (estado.error != null) {
                        isEnviando = false
                        dialogBinding.progressDialog.isVisible = false
                        dialogBinding.textErrorDialog.isVisible = true
                        dialogBinding.textErrorDialog.text = estado.error
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = true
                        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.isEnabled = true
                        dialogBinding.inputHoras.isEnabled = true
                    } else if (estado.data == true) {
                        isEnviando = false
                        dialogBinding.progressDialog.isVisible = false
                        Toast.makeText(requireContext(), R.string.orden_rehabilitada, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        cargarOrden(ordenId)
                    }
                }
            }
        }

        dialog.show()
    }

    private fun agregarVistaDeLinea(linea: DetalleOrden) {
        val itemBinding = ItemLineaEvidenciaBinding.inflate(LayoutInflater.from(requireContext()), binding.contenedorLineas, false)

        val esMgp = linea.tipo_destino == "mgp" || linea.vehiculo_id == null
        val etiquetaDestino = if (!esMgp) {
            linea.vehiculo?.placa ?: getString(R.string.vehiculo)
        } else {
            linea.descripcion_mgp ?: getString(R.string.planta_generadora)
        }

        itemBinding.textDestino.text = "$etiquetaDestino (${linea.tipo_combustible})"
        itemBinding.textMonto.text = getString(R.string.monto_lempiras, linea.monto)

        val evidenciasMap = linea.evidencias.associateBy { it.tipo }
        val evBomba = evidenciasMap["bomba"]
        val evTablero = evidenciasMap["tablero"]
        val evTanque = evidenciasMap["tanque"]
        val evFactura = evidenciasMap["factura"]

        // Admin: modo consulta (ver evidencias subidas)
        // 1. Bomba
        itemBinding.btnBomba.isVisible = false
        itemBinding.textBombaListo.isVisible = true
        itemBinding.textBombaListo.text = if (evBomba != null) getString(R.string.ver_evidencia) else "Sin evidencia"
        itemBinding.textBombaListo.setOnClickListener {
            evBomba?.url?.let { url -> mostrarDialogoVerEvidencia(url) }
        }

        // 2. Tablero
        if (esMgp) {
            itemBinding.textTableroLabel.text = getString(R.string.foto_tablero_opcional)
            itemBinding.btnTablero.isVisible = false
            itemBinding.textTableroListo.isVisible = true
            itemBinding.textTableroListo.text = getString(R.string.no_aplica)
        } else {
            itemBinding.textTableroLabel.text = getString(R.string.foto_tablero)
            itemBinding.btnTablero.isVisible = false
            itemBinding.textTableroListo.isVisible = true
            itemBinding.textTableroListo.text = if (evTablero != null) getString(R.string.ver_evidencia) else "Sin evidencia"
            itemBinding.textTableroListo.setOnClickListener {
                evTablero?.url?.let { url -> mostrarDialogoVerEvidencia(url) }
            }
        }

        // 3. Tanque
        itemBinding.btnTanque.isVisible = false
        itemBinding.textTanqueListo.isVisible = true
        itemBinding.textTanqueListo.text = if (evTanque != null) getString(R.string.ver_evidencia) else "Sin evidencia"
        itemBinding.textTanqueListo.setOnClickListener {
            evTanque?.url?.let { url -> mostrarDialogoVerEvidencia(url) }
        }

        // 4. Factura
        itemBinding.btnFactura.isVisible = false
        itemBinding.textFacturaListo.isVisible = true
        itemBinding.textFacturaListo.text = if (evFactura != null) getString(R.string.ver_evidencia) else "Sin evidencia"
        itemBinding.textFacturaListo.setOnClickListener {
            evFactura?.url?.let { url -> mostrarDialogoVerEvidencia(url) }
        }

        binding.contenedorLineas.addView(itemBinding.root)
    }

    private fun mostrarDialogoVerEvidencia(urlEvidencia: String) {
        val urlNormalizada = normalizarUrlApi(urlEvidencia)
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                750
            ).apply {
                setMargins(0, 0, 0, 32)
            }
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val token = SessionManager.token
        val glideUrl = GlideUrl(
            urlNormalizada,
            LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        )

        Glide.with(context)
            .load(glideUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_dialog_alert)
            .into(imageView)

        layout.addView(imageView)

        val btnDescargar = MaterialButton(context).apply {
            text = getString(R.string.descargar_foto)
            setIconResource(android.R.drawable.ic_menu_save)
            setOnClickListener {
                descargarFoto(urlNormalizada)
            }
        }
        layout.addView(btnDescargar)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.evidencia)
            .setView(layout)
            .setPositiveButton(R.string.aceptar, null)
            .show()
    }

    private fun descargarFoto(urlEvidencia: String) {
        val urlNormalizada = normalizarUrlApi(urlEvidencia)
        val context = requireContext()
        Toast.makeText(context, "Descargando foto...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = SessionManager.token
                val request = Request.Builder()
                    .url(urlNormalizada)
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                val response = ApiClient.client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        val bytes = body.bytes()
                        val filename = "evidencia_${System.currentTimeMillis()}.jpg"
                        val saved = saveImageToGalleryOrDownloads(context, bytes, filename)
                        withContext(Dispatchers.Main) {
                            if (saved) {
                                Toast.makeText(context, "¡Foto descargada con éxito en Imágenes/GeoComEvidencias!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Error al guardar la foto", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "La respuesta no contiene datos.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error al descargar: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveImageToGalleryOrDownloads(context: Context, bytes: ByteArray, filename: String): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GeoComEvidencias")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytes)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

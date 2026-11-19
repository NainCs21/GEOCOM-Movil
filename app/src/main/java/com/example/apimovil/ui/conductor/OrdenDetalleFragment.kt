package com.example.apimovil.ui.conductor

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import com.example.apimovil.databinding.FragmentOrdenDetalleBinding
import com.example.apimovil.databinding.ItemLineaEvidenciaBinding
import com.example.apimovil.ui.common.colorDeEstado
import com.example.apimovil.ui.common.etiquetaDeEstado
import com.example.apimovil.ui.common.fondoBadgeDeEstado
import com.example.apimovil.ui.common.formatoLegible
import com.example.apimovil.ui.common.normalizarUrlApi
import com.example.apimovil.ui.common.parseFechaIso
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

class OrdenDetalleFragment : Fragment(R.layout.fragment_orden_detalle) {

    private val model: ConductorViewModel by viewModels()
    private var _binding: FragmentOrdenDetalleBinding? = null
    private val binding get() = _binding!!

    private var contador: CountDownTimer? = null
    private var ordenActual: Orden? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentOrdenDetalleBinding.bind(view)
        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }

        val ordenId = arguments?.getInt("ordenId") ?: -1
        if (ordenId != -1) {
            model.cargarOrdenPorId(ordenId)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ordenEscaneada.collect { estado ->
                    binding.progress.isVisible = estado.loading
                    binding.textError.isVisible = estado.error != null
                    binding.textError.text = estado.error

                    estado.data?.let { orden ->
                        ordenActual = orden
                        pintarOrden(orden)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.evidencia.collect { estado ->
                    if (estado.data != null) {
                        ordenActual = estado.data
                        pintarOrden(estado.data)
                        model.limpiarEvidencia()
                    }
                    if (estado.error != null) {
                        binding.textError.isVisible = true
                        binding.textError.text = estado.error
                    }
                }
            }
        }
    }

    private fun pintarOrden(orden: Orden) {
        binding.textNumeroOrden.text = getString(R.string.orden_numero, orden.numero_orden)

        binding.textEstado.text = etiquetaDeEstado(requireContext(), orden.estado)
        binding.textEstado.setBackgroundResource(fondoBadgeDeEstado(orden.estado))
        binding.textEstado.setTextColor(colorDeEstado(requireContext(), orden.estado))

        contador?.cancel()

        if (orden.estado == "pendiente") {
            binding.contenedorProtegido.isVisible = true
            binding.contenedorDetalleRevelado.isVisible = false
            binding.contenedorContador.isVisible = false
            binding.textMensajeFinal.isVisible = false
            binding.textPlazoRetiro.text = getString(
                R.string.plazo_de_retiro, formatoLegible(orden.vence_asignacion)
            )
            binding.btnEscanearDesdeDetalle.setOnClickListener {
                findNavController().navigate(R.id.escanearQrFragment)
            }
            return
        }

        binding.contenedorProtegido.isVisible = false
        binding.contenedorDetalleRevelado.isVisible = true
        binding.textGasolinera.text = orden.gasolinera?.nombre ?: "—"
        binding.textDireccionGasolinera.text = orden.gasolinera?.direccion.orEmpty()

        val lineas = orden.detalles.orEmpty()
        val montoTotal = lineas.sumOf { it.monto }
        binding.textMontoTotal.text = getString(R.string.monto_lempiras, montoTotal)
        binding.textCantidadLineas.text = getString(R.string.distribuido_en_n_lineas, lineas.size)

        binding.contenedorLineas.removeAllViews()
        lineas.forEach { linea -> agregarVistaDeLinea(linea) }

        val todasLineasCompletas = lineas.isNotEmpty() && lineas.all { l ->
            val mgp = l.tipo_destino == "mgp" || l.vehiculo_id == null
            val tiposSubidos = l.evidencias.map { it.tipo }.toSet()
            tiposSubidos.contains("bomba") && (tiposSubidos.contains("tablero") || mgp) && tiposSubidos.contains("tanque") && tiposSubidos.contains("factura")
        }

        when (orden.estado) {
            "abierta", "rehabilitada", "completada" -> {
                if (todasLineasCompletas || orden.estado == "completada") {
                    binding.contenedorContador.isVisible = false
                    binding.textMensajeFinal.isVisible = true
                    binding.textMensajeFinal.text = getString(R.string.orden_completada_mensaje)
                } else {
                    binding.contenedorContador.isVisible = true
                    binding.textMensajeFinal.isVisible = false
                    iniciarContador(orden.vence_apertura)
                }
            }
            "tardio", "completado_tardio" -> {
                binding.contenedorContador.isVisible = false
                binding.textMensajeFinal.isVisible = true
                binding.textMensajeFinal.text = getString(R.string.orden_completada_mensaje)
            }
            "vencida" -> {
                binding.contenedorContador.isVisible = false
                binding.textMensajeFinal.isVisible = true
                binding.textMensajeFinal.text = getString(R.string.orden_vencida_mensaje)
            }
            else -> {
                binding.contenedorContador.isVisible = false
                binding.textMensajeFinal.isVisible = false
            }
        }
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

        val lineasList = ordenActual?.detalles.orEmpty()
        val todasCompletas = lineasList.isNotEmpty() && lineasList.all { l ->
            val mgp = l.tipo_destino == "mgp" || l.vehiculo_id == null
            val tiposSubidos = l.evidencias.map { it.tipo }.toSet()
            tiposSubidos.contains("bomba") && (tiposSubidos.contains("tablero") || mgp) && tiposSubidos.contains("tanque") && tiposSubidos.contains("factura")
        }
        val isOpen = (ordenActual?.estado == "abierta" || ordenActual?.estado == "rehabilitada") && !todasCompletas

        val evidenciasMap = linea.evidencias.associateBy { it.tipo }

        fun isSubido(tipo: String): Boolean {
            return evidenciasMap.containsKey(tipo)
        }

        fun getUrlOrUri(tipo: String): Any? {
            val ev = evidenciasMap[tipo]
            return ev?.url
        }

        val bombaSubida = isSubido("bomba")
        val tableroSubida = isSubido("tablero") || esMgp
        val tanqueSubida = isSubido("tanque")
        val facturaSubida = isSubido("factura")

        // 1. Bomba
        itemBinding.btnBomba.isVisible = isOpen && !bombaSubida
        itemBinding.textBombaListo.isVisible = bombaSubida
        itemBinding.textBombaListo.text = if (bombaSubida) getString(R.string.ver_evidencia) else ""
        itemBinding.btnBomba.setOnClickListener { pedirPermisoYAbrirCamara(linea.id, "bomba") }
        itemBinding.textBombaListo.setOnClickListener {
            getUrlOrUri("bomba")?.let { target -> mostrarDialogoVerEvidencia(target) }
        }

        // 2. Tablero
        if (esMgp) {
            itemBinding.textTableroLabel.text = getString(R.string.foto_tablero_opcional)
            itemBinding.btnTablero.isVisible = false
            itemBinding.textTableroListo.isVisible = true
            itemBinding.textTableroListo.text = "No aplica (MGP)"
        } else {
            itemBinding.textTableroLabel.text = getString(R.string.foto_tablero)
            itemBinding.btnTablero.isVisible = isOpen && !tableroSubida
            itemBinding.textTableroListo.isVisible = tableroSubida
            itemBinding.textTableroListo.text = if (tableroSubida) getString(R.string.ver_evidencia) else ""
            itemBinding.btnTablero.setOnClickListener { pedirPermisoYAbrirCamara(linea.id, "tablero") }
            itemBinding.textTableroListo.setOnClickListener {
                getUrlOrUri("tablero")?.let { target -> mostrarDialogoVerEvidencia(target) }
            }
        }

        // 3. Tanque
        itemBinding.btnTanque.isVisible = isOpen && !tanqueSubida
        itemBinding.textTanqueListo.isVisible = tanqueSubida
        itemBinding.textTanqueListo.text = if (tanqueSubida) getString(R.string.ver_evidencia) else ""
        itemBinding.btnTanque.setOnClickListener { pedirPermisoYAbrirCamara(linea.id, "tanque") }
        itemBinding.textTanqueListo.setOnClickListener {
            getUrlOrUri("tanque")?.let { target -> mostrarDialogoVerEvidencia(target) }
        }

        // 4. Factura
        itemBinding.btnFactura.isVisible = isOpen && !facturaSubida
        itemBinding.textFacturaListo.isVisible = facturaSubida
        itemBinding.textFacturaListo.text = if (facturaSubida) getString(R.string.ver_evidencia) else ""
        itemBinding.btnFactura.setOnClickListener { pedirPermisoYAbrirCamara(linea.id, "factura") }
        itemBinding.textFacturaListo.setOnClickListener {
            getUrlOrUri("factura")?.let { target -> mostrarDialogoVerEvidencia(target) }
        }

        binding.contenedorLineas.addView(itemBinding.root)
    }

    private fun mostrarDialogoVerEvidencia(target: Any) {
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

        if (target is String) {
            val urlNormalizada = normalizarUrlApi(target)
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
        } else if (target is Uri) {
            Glide.with(context)
                .load(target)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert)
                .into(imageView)
        }

        layout.addView(imageView)

        if (target is String) {
            val urlNormalizada = normalizarUrlApi(target)
            val btnDescargar = MaterialButton(context).apply {
                text = getString(R.string.descargar_foto)
                setIconResource(android.R.drawable.ic_menu_save)
                setOnClickListener {
                    descargarFoto(urlNormalizada)
                }
            }
            layout.addView(btnDescargar)
        }

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

    private fun pedirPermisoYAbrirCamara(detalleId: Int, tipo: String) {
        val orden = ordenActual
        if (orden?.estado == "rehabilitada") {
            val context = requireContext()
            val input = TextInputEditText(context).apply {
                hint = getString(R.string.ingresa_justificacion)
            }
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 32, 48, 16)
                addView(TextInputLayout(context).apply {
                    addView(input)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                })
            }
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.justificacion_tardanza)
                .setView(container)
                .setPositiveButton(R.string.continuar) { _, _ ->
                    val justif = input.text?.toString()?.trim().orEmpty()
                    if (justif.isBlank()) {
                        Toast.makeText(context, "Debes ingresar una justificación.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    irAInstruccion(detalleId, tipo, justif)
                }
                .setNegativeButton(R.string.cancelar, null)
                .show()
        } else {
            irAInstruccion(detalleId, tipo, null)
        }
    }

    private fun irAInstruccion(detalleId: Int, tipo: String, justificacion: String?) {
        val bundle = Bundle().apply {
            putInt("detalleId", detalleId)
            putInt("ordenId", ordenActual?.id ?: -1)
            putString("tipo", tipo)
            putString("justificacionTardanza", justificacion)
        }
        findNavController().navigate(R.id.instruccionEvidenciaFragment, bundle)
    }

    private fun iniciarContador(venceAperturaIso: String?) {
        if (venceAperturaIso == null) return
        val venceEn = parseFechaIso(venceAperturaIso).time
        val restante = venceEn - System.currentTimeMillis()

        if (restante <= 0) {
            binding.textContador.text = getString(R.string.tiempo_agotado)
            ordenActual?.id?.let { model.cargarOrdenPorId(it) }
            return
        }

        contador = object : CountDownTimer(restante, 1000) {
            override fun onTick(msRestante: Long) {
                val minutos = (msRestante / 1000) / 60
                val segundos = (msRestante / 1000) % 60
                binding.textContador.text = getString(
                    R.string.tiempo_restante,
                    minutos.toString().padStart(2, '0'),
                    segundos.toString().padStart(2, '0')
                )
            }

            override fun onFinish() {
                binding.textContador.text = getString(R.string.tiempo_agotado)
                ordenActual?.id?.let { model.cargarOrdenPorId(it) }
            }
        }.start()
    }

    override fun onDestroyView() {
        contador?.cancel()
        _binding = null
        super.onDestroyView()
    }
}

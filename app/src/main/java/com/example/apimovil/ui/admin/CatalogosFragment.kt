package com.example.apimovil.ui.admin

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.models.Conductor
import com.example.apimovil.data.models.Gasolinera
import com.example.apimovil.data.models.Vehiculo
import com.example.apimovil.databinding.DialogFormConductorBinding
import com.example.apimovil.databinding.DialogFormGasolineraBinding
import com.example.apimovil.databinding.DialogFormVehiculoBinding
import com.example.apimovil.databinding.FragmentCatalogosBinding
import com.example.apimovil.ui.common.configurarNavegacionAdmin
import com.example.apimovil.ui.common.configurarNavegacionSupervisor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Calendar

/**
 * Catálogos de solo lectura (consulta rápida desde el celular). La gestión
 * completa vive en el panel web; aquí solo se necesita ver la lista.
 */
class CatalogosFragment : Fragment(R.layout.fragment_catalogos) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentCatalogosBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCatalogosBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())

        val bottomNav = binding.incluirNavInferior.root
        if (SessionManager.esAdmin) {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_admin)
            bottomNav.configurarNavegacionAdmin(findNavController(), R.id.catalogosFragment)
        } else {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_supervisor)
            bottomNav.configurarNavegacionSupervisor(findNavController(), R.id.catalogosFragment)
        }

        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }

        binding.btnNuevo.isVisible = SessionManager.esAdmin
        binding.btnNuevo.setOnClickListener { mostrarDialogoNuevo() }

        binding.toggleCatalogo.check(R.id.btnConductores)
        mostrarConductores()

        binding.btnConductores.setOnClickListener { mostrarConductores() }
        binding.btnVehiculos.setOnClickListener { mostrarVehiculos() }
        binding.btnGasolineras.setOnClickListener { mostrarGasolineras() }

        binding.inputBuscarConductor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val b = _binding ?: return
                if (b.toggleCatalogo.checkedButtonId == R.id.btnConductores) {
                    val estado = model.conductores.value
                    pintarConductores(estado.loading, estado.error, aplicarFiltroConductores(estado.data))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.inputBuscarVehiculo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val b = _binding ?: return
                if (b.toggleCatalogo.checkedButtonId == R.id.btnVehiculos) {
                    val estado = model.vehiculos.value
                    pintar(estado.loading, estado.error, aplicarFiltrosVehiculos(estado.data))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.toggleFiltroCombustible.addOnButtonCheckedListener { _, _, isChecked ->
            val b = _binding ?: return@addOnButtonCheckedListener
            if (isChecked && b.toggleCatalogo.checkedButtonId == R.id.btnVehiculos) {
                val estado = model.vehiculos.value
                pintar(estado.loading, estado.error, aplicarFiltrosVehiculos(estado.data))
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.conductores.collect { estado ->
                    val b = _binding ?: return@collect
                    if (b.toggleCatalogo.checkedButtonId == R.id.btnConductores) {
                        pintarConductores(estado.loading, estado.error, aplicarFiltroConductores(estado.data))
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.vehiculos.collect { estado ->
                    val b = _binding ?: return@collect
                    if (b.toggleCatalogo.checkedButtonId == R.id.btnVehiculos) {
                        pintar(estado.loading, estado.error, aplicarFiltrosVehiculos(estado.data))
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.gasolineras.collect { estado ->
                    val b = _binding ?: return@collect
                    if (b.toggleCatalogo.checkedButtonId == R.id.btnGasolineras) {
                        pintar(estado.loading, estado.error, estado.data?.map {
                            FilaDoble(
                                id = it.id,
                                titulo = it.nombre,
                                subtitulo = "${it.direccion ?: "—"} · QR: ${it.codigo_qr}" +
                                    if (!it.activo) " · " + getString(R.string.inactivo) else "",
                                iconRes = R.drawable.ic_gas_pump,
                                tintRes = if (!it.activo) R.color.texto_secundario else null
                            )
                        })
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.accionCatalogo.collect { estado ->
                    if (_binding == null) return@collect
                    if (estado.error != null) {
                        Toast.makeText(requireContext(), estado.error, Toast.LENGTH_SHORT).show()
                    } else if (estado.data == true) {
                        val mensajeRes = if (model.ultimoTipoAccion == "reactivado") {
                            R.string.registro_reactivado
                        } else {
                            R.string.registro_exitoso
                        }
                        Toast.makeText(requireContext(), mensajeRes, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun normalizarTexto(texto: String?): String {
        if (texto.isNullOrBlank()) return ""
        val nfd = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").lowercase().trim()
    }

    private fun aplicarFiltroConductores(lista: List<Conductor>?): List<FilaDoble> {
        val b = _binding ?: return emptyList()
        val query = normalizarTexto(b.inputBuscarConductor.text?.toString())

        val filtrados = lista.orEmpty().filter { cond ->
            if (query.isEmpty()) return@filter true
            val nombreNorm = normalizarTexto(cond.usuario?.name)
            val telefonoNorm = normalizarTexto(cond.telefono)
            val cargoNorm = normalizarTexto(cond.cargo)
            nombreNorm.contains(query) || telefonoNorm.contains(query) || cargoNorm.contains(query)
        }

        return filtrados.map { cond ->
            FilaDoble(
                id = cond.id,
                titulo = cond.usuario?.name ?: getString(R.string.conductor_numero, cond.id),
                subtitulo = "${cond.telefono} · ${cond.cargo}" +
                    if (!cond.activo) " · " + getString(R.string.inactivo) else "",
                iconRes = R.drawable.ic_person,
                tintRes = if (!cond.activo) R.color.texto_secundario else null
            )
        }
    }

    private fun pintarConductores(loading: Boolean, error: String?, items: List<FilaDoble>?) {
        val b = _binding ?: return
        b.progress.isVisible = loading
        val query = b.inputBuscarConductor.text?.toString()?.trim().orEmpty()
        val sinResultadosPorBusqueda = !loading && error == null && items.isNullOrEmpty() && query.isNotEmpty()

        if (sinResultadosPorBusqueda) {
            b.textError.isVisible = true
            b.textError.text = getString(R.string.no_se_encontraron_conductores)
            b.textError.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_secundario))
        } else {
            b.textError.isVisible = error != null
            b.textError.text = error
            b.textError.setTextColor(ContextCompat.getColor(requireContext(), R.color.estado_vencida))
        }

        b.recycler.adapter = FilaDobleAdapter(items.orEmpty()) { id, titulo ->
            mostrarDialogoOpciones(id, titulo)
        }
    }

    private fun mostrarDialogoOpciones(id: Int, titulo: String) {
        if (!SessionManager.esAdmin) return
        val context = requireContext()
        val toggleId = binding.toggleCatalogo.checkedButtonId
        val esGasolinera = toggleId == R.id.btnGasolineras

        val esActivo = when (toggleId) {
            R.id.btnConductores -> model.conductores.value.data?.find { it.id == id }?.activo == true
            R.id.btnVehiculos -> model.vehiculos.value.data?.find { it.id == id }?.activo == true
            R.id.btnGasolineras -> model.gasolineras.value.data?.find { it.id == id }?.activo == true
            else -> true
        }

        val opcionesPares = mutableListOf<Pair<String, () -> Unit>>()
        if (esGasolinera) {
            opcionesPares.add(Pair(getString(R.string.ver_codigo_qr)) { mostrarDialogoVerQr(id) })
        }
        opcionesPares.add(Pair(getString(R.string.editar)) { mostrarDialogoEditar(id) })
        if (esActivo) {
            opcionesPares.add(Pair(getString(R.string.desactivar)) { confirmarDesactivar(id) })
        } else {
            opcionesPares.add(Pair(getString(R.string.reactivar)) { confirmarReactivar(id) })
        }

        val textosOpciones = opcionesPares.map { it.first }.toTypedArray()

        MaterialAlertDialogBuilder(context)
            .setTitle(titulo)
            .setItems(textosOpciones) { _, which ->
                opcionesPares.getOrNull(which)?.second?.invoke()
            }
            .show()
    }

    private fun confirmarReactivar(id: Int) {
        val context = requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.reactivar)
            .setMessage(R.string.reactivar_mensaje)
            .setPositiveButton(R.string.reactivar) { _, _ ->
                when (binding.toggleCatalogo.checkedButtonId) {
                    R.id.btnConductores -> model.reactivarConductor(id)
                    R.id.btnVehiculos -> model.reactivarVehiculo(id)
                    R.id.btnGasolineras -> model.reactivarGasolinera(id)
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun mostrarDialogoVerQr(id: Int) {
        val gas = model.gasolineras.value.data?.find { it.id == id } ?: return
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
            gravity = Gravity.CENTER
        }

        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(500, 500).apply { bottomMargin = 16 }
        }
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(gas.codigo_qr, BarcodeFormat.QR_CODE, 500, 500)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            // ignore
        }

        val textQr = TextView(context).apply {
            text = gas.codigo_qr
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        layout.addView(imageView)
        layout.addView(textQr)

        MaterialAlertDialogBuilder(context)
            .setTitle(gas.nombre)
            .setView(layout)
            .setPositiveButton(R.string.aceptar, null)
            .show()
    }

    private fun confirmarDesactivar(id: Int) {
        val context = requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.desactivar)
            .setMessage(R.string.desactivar_mensaje)
            .setPositiveButton(R.string.desactivar) { _, _ ->
                when (binding.toggleCatalogo.checkedButtonId) {
                    R.id.btnConductores -> model.desactivarConductor(id)
                    R.id.btnVehiculos -> model.desactivarVehiculo(id)
                    R.id.btnGasolineras -> model.desactivarGasolinera(id)
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun mostrarDialogoNuevo() {
        mostrarDialogoFormulario(null)
    }

    private fun mostrarDialogoEditar(id: Int) {
        when (binding.toggleCatalogo.checkedButtonId) {
            R.id.btnConductores -> {
                val item = model.conductores.value.data?.find { it.id == id }
                mostrarDialogoFormulario(item)
            }
            R.id.btnVehiculos -> {
                val item = model.vehiculos.value.data?.find { it.id == id }
                mostrarDialogoFormulario(item)
            }
            R.id.btnGasolineras -> {
                val item = model.gasolineras.value.data?.find { it.id == id }
                mostrarDialogoFormulario(item)
            }
        }
    }

    private fun mostrarDialogoFormulario(item: Any?) {
        val context = requireContext()
        val esEdicion = item != null

        val dialog: AlertDialog? = when (binding.toggleCatalogo.checkedButtonId) {
            R.id.btnConductores -> {
                val cond = item as? Conductor
                val diagBinding = DialogFormConductorBinding.inflate(LayoutInflater.from(context))
                if (esEdicion && cond != null) {
                    diagBinding.inputNombre.setText(cond.usuario?.name)
                    diagBinding.inputCorreo.setText(cond.usuario?.email)
                    diagBinding.inputPassword.hint = getString(R.string.contrasena_opcional_hint)
                    diagBinding.inputTelefono.setText(cond.telefono)
                    diagBinding.inputCargo.setText(cond.cargo)
                }

                diagBinding.inputNombre.addTextChangedListener { diagBinding.layoutNombre.error = null }
                diagBinding.inputCorreo.addTextChangedListener { diagBinding.layoutCorreo.error = null }
                diagBinding.inputPassword.addTextChangedListener { diagBinding.layoutPassword.error = null }
                diagBinding.inputTelefono.addTextChangedListener { diagBinding.layoutTelefono.error = null }
                diagBinding.inputCargo.addTextChangedListener { diagBinding.layoutCargo.error = null }

                val d = MaterialAlertDialogBuilder(context)
                    .setTitle(if (esEdicion) getString(R.string.editar_conductor) else getString(R.string.nuevo_conductor))
                    .setView(diagBinding.root)
                    .setPositiveButton(R.string.guardar, null)
                    .setNegativeButton(R.string.cancelar, null)
                    .create()

                d.setOnShowListener {
                    d.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                        val nombre = diagBinding.inputNombre.text?.toString()?.trim().orEmpty()
                        val correo = diagBinding.inputCorreo.text?.toString()?.trim().orEmpty()
                        val pass = diagBinding.inputPassword.text?.toString() ?: ""
                        val telefono = diagBinding.inputTelefono.text?.toString()?.trim().orEmpty()
                        val cargo = diagBinding.inputCargo.text?.toString()?.trim().orEmpty()

                        var hasError = false
                        if (nombre.isBlank()) {
                            diagBinding.layoutNombre.error = getString(R.string.error_nombre_requerido)
                            hasError = true
                        }
                        if (correo.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                            diagBinding.layoutCorreo.error = getString(R.string.error_correo_invalido)
                            hasError = true
                        }
                        if (!esEdicion) {
                            if (pass.isBlank() || pass.length < 8) {
                                diagBinding.layoutPassword.error = getString(R.string.error_contrasena_min_8)
                                hasError = true
                            }
                        } else {
                            if (pass.isNotBlank() && pass.length < 8) {
                                diagBinding.layoutPassword.error = getString(R.string.error_contrasena_min_8)
                                hasError = true
                            }
                        }
                        if (telefono.isBlank()) {
                            diagBinding.layoutTelefono.error = getString(R.string.error_telefono_requerido)
                            hasError = true
                        }
                        if (cargo.isBlank()) {
                            diagBinding.layoutCargo.error = getString(R.string.error_cargo_requerido)
                            hasError = true
                        }

                        if (!hasError) {
                            val map = mutableMapOf<String, Any?>(
                                "nombre" to nombre,
                                "correo" to correo,
                                "telefono" to telefono,
                                "cargo" to cargo
                            )
                            if (pass.isNotBlank()) {
                                map["contrasena"] = pass
                            }
                            if (esEdicion && cond != null) {
                                model.editarConductor(cond.id, map)
                            } else {
                                model.crearConductor(map)
                            }
                            d.dismiss()
                        }
                    }
                }
                d
            }
            R.id.btnVehiculos -> {
                val veh = item as? Vehiculo
                val diagBinding = DialogFormVehiculoBinding.inflate(LayoutInflater.from(context))

                val opcionesCombustible = arrayOf("Diesel", "Gasolina")
                val mapaCombustible = mapOf("Diesel" to "diesel", "Gasolina" to "gasolina", "Diésel" to "diesel")
                val adapterCombustible = ArrayAdapter(context, android.R.layout.simple_list_item_1, opcionesCombustible)
                diagBinding.inputCombustible.setAdapter(adapterCombustible)

                if (esEdicion && veh != null) {
                    diagBinding.inputPlaca.setText(veh.placa)
                    diagBinding.inputAnio.setText(veh.anio?.toString() ?: "")
                    diagBinding.inputMarca.setText(veh.marca)
                    diagBinding.inputModelo.setText(veh.modelo)
                    val combText = when (veh.tipo_combustible.lowercase()) {
                        "gasolina" -> "Gasolina"
                        else -> getString(R.string.diesel_combustible)
                    }
                    diagBinding.inputCombustible.setText(combText, false)
                    diagBinding.inputRendimiento.setText(veh.rendimiento_km_litro?.toString() ?: "")
                } else {
                    diagBinding.inputCombustible.setText(getString(R.string.diesel_combustible), false)
                }

                diagBinding.inputPlaca.addTextChangedListener { diagBinding.layoutPlaca.error = null }
                diagBinding.inputMarca.addTextChangedListener { diagBinding.layoutMarca.error = null }
                diagBinding.inputModelo.addTextChangedListener { diagBinding.layoutModelo.error = null }
                diagBinding.inputAnio.addTextChangedListener { diagBinding.layoutAnio.error = null }
                diagBinding.inputRendimiento.addTextChangedListener { diagBinding.layoutRendimiento.error = null }

                val currentYear = Calendar.getInstance().get(Calendar.YEAR) + 1

                val d = MaterialAlertDialogBuilder(context)
                    .setTitle(if (esEdicion) getString(R.string.editar_vehiculo) else getString(R.string.nuevo_vehiculo))
                    .setView(diagBinding.root)
                    .setPositiveButton(R.string.guardar, null)
                    .setNegativeButton(R.string.cancelar, null)
                    .create()

                d.setOnShowListener {
                    d.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                        val placa = diagBinding.inputPlaca.text?.toString()?.trim().orEmpty()
                        val marca = diagBinding.inputMarca.text?.toString()?.trim().orEmpty()
                        val modelo = diagBinding.inputModelo.text?.toString()?.trim().orEmpty()
                        val anioStr = diagBinding.inputAnio.text?.toString()?.trim().orEmpty()
                        val comb = diagBinding.inputCombustible.text?.toString()?.trim().orEmpty()
                        val rendimientoStr = diagBinding.inputRendimiento.text?.toString()?.trim().orEmpty()

                        var hasError = false
                        if (placa.isBlank()) {
                            diagBinding.layoutPlaca.error = getString(R.string.error_placa_requerida)
                            hasError = true
                        }
                        if (marca.isBlank()) {
                            diagBinding.layoutMarca.error = getString(R.string.error_marca_requerida)
                            hasError = true
                        }
                        if (modelo.isBlank()) {
                            diagBinding.layoutModelo.error = getString(R.string.error_modelo_requerido)
                            hasError = true
                        }

                        var anioVal: Int? = null
                        if (anioStr.isNotBlank()) {
                            val anioParsed = anioStr.toIntOrNull()
                            if (anioParsed == null || anioParsed !in 1980..currentYear) {
                                diagBinding.layoutAnio.error = getString(R.string.error_anio_invalido)
                                hasError = true
                            } else {
                                anioVal = anioParsed
                            }
                        }

                        var rendimientoVal: Double? = null
                        if (rendimientoStr.isNotBlank()) {
                            val rendParsed = rendimientoStr.toDoubleOrNull()
                            if (rendParsed == null || rendParsed !in 0.1..99.9) {
                                diagBinding.layoutRendimiento.error = getString(R.string.error_rendimiento_invalido)
                                hasError = true
                            } else {
                                rendimientoVal = rendParsed
                            }
                        }

                        if (!hasError) {
                            val tipoCombustibleVal = mapaCombustible[comb] ?: "diesel"
                            val map = mutableMapOf<String, Any?>(
                                "placa" to placa,
                                "marca" to marca,
                                "modelo" to modelo,
                                "anio" to anioVal,
                                "tipo_combustible" to tipoCombustibleVal
                            )
                            if (rendimientoVal != null) {
                                map["rendimiento_km_litro"] = rendimientoVal
                            }
                            if (esEdicion && veh != null) {
                                model.editarVehiculo(veh.id, map)
                            } else {
                                model.crearVehiculo(map)
                            }
                            d.dismiss()
                        }
                    }
                }
                d
            }
            R.id.btnGasolineras -> {
                val gas = item as? Gasolinera
                val diagBinding = DialogFormGasolineraBinding.inflate(LayoutInflater.from(context))

                if (esEdicion && gas != null) {
                    diagBinding.inputNombre.setText(gas.nombre)
                    diagBinding.inputDireccion.setText(gas.direccion)
                    diagBinding.inputQr.isEnabled = true
                    diagBinding.inputQr.setText(gas.codigo_qr)
                } else {
                    diagBinding.inputQr.isEnabled = false
                }

                diagBinding.inputNombre.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        diagBinding.layoutNombre.error = null
                        if (!esEdicion) {
                            val nombre = s?.toString()?.trim().orEmpty()
                            val generatedQr = "QR-" + nombre.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')
                            diagBinding.inputQr.setText(generatedQr)
                        }
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })
                diagBinding.inputQr.addTextChangedListener { diagBinding.layoutQr.error = null }

                val d = MaterialAlertDialogBuilder(context)
                    .setTitle(if (esEdicion) getString(R.string.editar_gasolinera) else getString(R.string.nueva_gasolinera))
                    .setView(diagBinding.root)
                    .setPositiveButton(R.string.guardar, null)
                    .setNegativeButton(R.string.cancelar, null)
                    .create()

                d.setOnShowListener {
                    d.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                        val nombre = diagBinding.inputNombre.text?.toString()?.trim().orEmpty()
                        val direccion = diagBinding.inputDireccion.text?.toString()?.trim()
                        val qr = diagBinding.inputQr.text?.toString()?.trim().orEmpty()

                        var hasError = false
                        if (nombre.isBlank()) {
                            diagBinding.layoutNombre.error = getString(R.string.error_gasolinera_nombre)
                            hasError = true
                        }
                        if (qr.isBlank()) {
                            diagBinding.layoutQr.error = getString(R.string.error_qr_requerido)
                            hasError = true
                        }

                        if (!hasError) {
                            val map = mapOf(
                                "nombre" to nombre,
                                "direccion" to direccion,
                                "codigo_qr" to qr
                            )
                            if (esEdicion && gas != null) {
                                model.editarGasolinera(gas.id, map)
                            } else {
                                model.crearGasolinera(map)
                            }
                            d.dismiss()
                        }
                    }
                }
                d
            }
            else -> null
        }

        dialog?.show()
    }

    private fun mostrarConductores() {
        val b = _binding ?: return
        b.toggleCatalogo.check(R.id.btnConductores)
        b.layoutFiltrosConductores.isVisible = true
        b.layoutFiltrosVehiculos.isVisible = false
        b.inputBuscarVehiculo.setText("")
        b.btnNuevo.text = getString(R.string.nuevo_conductor_btn)
        val estado = model.conductores.value
        if (estado.data != null) {
            pintarConductores(estado.loading, estado.error, aplicarFiltroConductores(estado.data))
        }
        model.cargarConductores()
    }

    private fun mostrarVehiculos() {
        val b = _binding ?: return
        b.toggleCatalogo.check(R.id.btnVehiculos)
        b.layoutFiltrosConductores.isVisible = false
        b.layoutFiltrosVehiculos.isVisible = true
        b.inputBuscarConductor.setText("")
        b.btnNuevo.text = getString(R.string.nuevo_vehiculo_btn)
        if (b.toggleFiltroCombustible.checkedButtonId == View.NO_ID) {
            b.toggleFiltroCombustible.check(R.id.btnFiltroTodos)
        }
        val estado = model.vehiculos.value
        if (estado.data != null) {
            pintar(estado.loading, estado.error, aplicarFiltrosVehiculos(estado.data))
        }
        model.cargarVehiculos()
    }

    private fun mostrarGasolineras() {
        val b = _binding ?: return
        b.toggleCatalogo.check(R.id.btnGasolineras)
        b.layoutFiltrosConductores.isVisible = false
        b.layoutFiltrosVehiculos.isVisible = false
        b.inputBuscarConductor.setText("")
        b.inputBuscarVehiculo.setText("")
        b.btnNuevo.text = getString(R.string.nuevo_gasolinera_btn)
        model.cargarGasolineras()
    }

    private fun aplicarFiltrosVehiculos(vehiculos: List<Vehiculo>?): List<FilaDoble> {
        val b = _binding ?: return emptyList()
        val query = b.inputBuscarVehiculo.text?.toString()?.trim()?.lowercase().orEmpty()
        val filtroCombustibleId = b.toggleFiltroCombustible.checkedButtonId

        val filtrados = vehiculos.orEmpty().filter { veh ->
            val matchesCombustible = when (filtroCombustibleId) {
                R.id.btnFiltroGasolina -> veh.tipo_combustible.equals("gasolina", ignoreCase = true)
                R.id.btnFiltroDiesel -> veh.tipo_combustible.contains("diesel", ignoreCase = true) || veh.tipo_combustible.contains("diésel", ignoreCase = true)
                else -> true
            }

            val textoVehiculo = "${veh.placa} ${veh.marca} ${veh.modelo} ${veh.anio ?: ""} ${veh.tipo_combustible}".lowercase()
            val matchesTexto = query.isEmpty() || textoVehiculo.contains(query)

            matchesCombustible && matchesTexto
        }

        return filtrados.map {
            val rendimientoStr = " · ${it.rendimiento_km_litro ?: 8.0} km/L"
            FilaDoble(
                id = it.id,
                titulo = "${it.placa} · ${it.marca} ${it.modelo}",
                subtitulo = it.tipo_combustible + rendimientoStr +
                    if (!it.activo) " · " + getString(R.string.inactivo) else "",
                iconRes = R.drawable.ic_vehiculo,
                tintRes = if (!it.activo) R.color.texto_secundario else null
            )
        }
    }

    private fun pintar(loading: Boolean, error: String?, items: List<FilaDoble>?) {
        val b = _binding ?: return
        b.progress.isVisible = loading
        b.textError.isVisible = error != null
        b.textError.text = error
        b.textError.setTextColor(ContextCompat.getColor(requireContext(), R.color.estado_vencida))
        b.recycler.adapter = FilaDobleAdapter(items.orEmpty()) { id, titulo ->
            mostrarDialogoOpciones(id, titulo)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

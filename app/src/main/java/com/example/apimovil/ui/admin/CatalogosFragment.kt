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
import com.example.apimovil.ui.common.FiltrosEntrada
import com.example.apimovil.ui.common.Validadores
import com.example.apimovil.ui.common.configurarNavegacionAdmin
import com.example.apimovil.ui.common.configurarNavegacionSupervisor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch

class CatalogosFragment : Fragment(R.layout.fragment_catalogos) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentCatalogosBinding? = null
    private val binding get() = _binding!!

    private var activeDialog: AlertDialog? = null
    private var activeDialogType: String? = null
    private var activeConductorBinding: DialogFormConductorBinding? = null
    private var activeVehiculoBinding: DialogFormVehiculoBinding? = null
    private var activeGasolineraBinding: DialogFormGasolineraBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCatalogosBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())

        binding.inputBuscarConductor.filters = FiltrosEntrada.limiteYLargo(50)
        binding.inputBuscarVehiculo.filters = FiltrosEntrada.limiteYLargo(50)

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
                launch {
                    model.conductores.collect { estado ->
                        val b = _binding ?: return@collect
                        if (b.toggleCatalogo.checkedButtonId == R.id.btnConductores) {
                            pintarConductores(estado.loading, estado.error, aplicarFiltroConductores(estado.data))
                        }
                    }
                }
                launch {
                    model.vehiculos.collect { estado ->
                        val b = _binding ?: return@collect
                        if (b.toggleCatalogo.checkedButtonId == R.id.btnVehiculos) {
                            pintar(estado.loading, estado.error, aplicarFiltrosVehiculos(estado.data))
                        }
                    }
                }
                launch {
                    model.gasolineras.collect { estado ->
                        val b = _binding ?: return@collect
                        if (b.toggleCatalogo.checkedButtonId == R.id.btnGasolineras) {
                            pintar(estado.loading, estado.error, estado.data?.map {
                                FilaDoble(
                                    id = it.id,
                                    titulo = it.nombre,
                                    subtitulo = "${it.direccion ?: "—"} · QR: ${it.codigo_qr}"
                                )
                            })
                        }
                    }
                }
                launch {
                    model.accionCatalogo.collect { estado ->
                        val dlg = activeDialog
                        if (dlg != null && dlg.isShowing) {
                            val posBtn = dlg.getButton(AlertDialog.BUTTON_POSITIVE)
                            posBtn?.isEnabled = !estado.loading

                            if (estado.error != null) {
                                val campos = estado.camposError
                                when (activeDialogType) {
                                    "conductor" -> {
                                        activeConductorBinding?.let { cb ->
                                            if (campos.containsKey("nombre")) cb.layoutNombre.error = campos["nombre"]
                                            if (campos.containsKey("correo") || campos.containsKey("email")) cb.layoutCorreo.error = campos["correo"] ?: campos["email"]
                                            if (campos.containsKey("contrasena") || campos.containsKey("password")) cb.layoutPassword.error = campos["contrasena"] ?: campos["password"]
                                            if (campos.containsKey("telefono")) cb.layoutTelefono.error = campos["telefono"]
                                            if (campos.containsKey("cargo")) cb.layoutCargo.error = campos["cargo"]
                                        }
                                    }
                                    "vehiculo" -> {
                                        activeVehiculoBinding?.let { vb ->
                                            if (campos.containsKey("placa")) vb.layoutPlaca.error = campos["placa"]
                                            if (campos.containsKey("marca")) vb.layoutMarca.error = campos["marca"]
                                            if (campos.containsKey("modelo")) vb.layoutModelo.error = campos["modelo"]
                                            if (campos.containsKey("anio")) vb.layoutAnio.error = campos["anio"]
                                            if (campos.containsKey("rendimiento_km_litro")) vb.layoutRendimiento.error = campos["rendimiento_km_litro"]
                                        }
                                    }
                                    "gasolinera" -> {
                                        activeGasolineraBinding?.let { gb ->
                                            if (campos.containsKey("nombre")) gb.layoutNombre.error = campos["nombre"]
                                            if (campos.containsKey("direccion")) gb.layoutDireccion.error = campos["direccion"]
                                            if (campos.containsKey("codigo_qr")) gb.layoutQr.error = campos["codigo_qr"]
                                        }
                                    }
                                }
                            } else if (estado.data == true) {
                                val mensajeRes = if (model.ultimoTipoAccion == "reactivado") {
                                    R.string.registro_reactivado
                                } else {
                                    R.string.registro_exitoso
                                }
                                Toast.makeText(requireContext(), mensajeRes, Toast.LENGTH_SHORT).show()
                                dlg.dismiss()
                                activeDialog = null
                                activeDialogType = null
                                activeConductorBinding = null
                                activeVehiculoBinding = null
                                activeGasolineraBinding = null
                                model.limpiarAccionCatalogo()
                            }
                        } else if (estado.data == true) {
                            val mensajeRes = if (model.ultimoTipoAccion == "reactivado") {
                                R.string.registro_reactivado
                            } else {
                                R.string.registro_exitoso
                            }
                            Toast.makeText(requireContext(), mensajeRes, Toast.LENGTH_SHORT).show()
                            model.limpiarAccionCatalogo()
                        } else if (estado.error != null && _binding != null) {
                            Toast.makeText(requireContext(), estado.error, Toast.LENGTH_SHORT).show()
                            model.limpiarAccionCatalogo()
                        }
                    }
                }
            }
        }
    }

    private fun aplicarFiltroConductores(conductores: List<Conductor>?): List<FilaDoble> {
        val b = _binding ?: return emptyList()
        val query = b.inputBuscarConductor.text?.toString()?.trim()?.lowercase().orEmpty()

        val filtrados = conductores.orEmpty().filter { cond ->
            val nombre = cond.usuario?.name.orEmpty().lowercase()
            val cargo = cond.cargo.lowercase()
            val telefono = cond.telefono.lowercase()

            query.isEmpty() || nombre.contains(query) || cargo.contains(query) || telefono.contains(query)
        }

        return filtrados.map {
            FilaDoble(
                id = it.id,
                titulo = it.usuario?.name ?: getString(R.string.conductor_numero, it.id),
                subtitulo = "${it.telefono} · ${it.cargo}" +
                    if (!it.activo) " · " + getString(R.string.inactivo) else ""
            )
        }
    }

    private fun pintarConductores(loading: Boolean, error: String?, items: List<FilaDoble>?) {
        pintar(loading, error, items)
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
        } catch (_: Exception) {}

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

        when (binding.toggleCatalogo.checkedButtonId) {
            R.id.btnConductores -> {
                val cond = item as? Conductor
                val diagBinding = DialogFormConductorBinding.inflate(LayoutInflater.from(context))

                diagBinding.inputNombre.filters = FiltrosEntrada.limiteYLargo(100, "^[\\p{L} .'-]+$")
                diagBinding.inputCorreo.filters = FiltrosEntrada.limiteYLargo(254)
                diagBinding.inputPassword.filters = FiltrosEntrada.limiteYLargo(128)
                diagBinding.inputTelefono.filters = FiltrosEntrada.limiteYLargo(20, "^[0-9 \\-+()]+$")
                diagBinding.inputCargo.filters = FiltrosEntrada.limiteYLargo(100, "^[\\p{L} .-]+$")

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
                    .setNegativeButton(R.string.cancelar) { dialog, _ ->
                        dialog.dismiss()
                        activeDialog = null
                        activeDialogType = null
                        activeConductorBinding = null
                    }
                    .create()

                activeDialog = d
                activeDialogType = "conductor"
                activeConductorBinding = diagBinding

                d.setOnShowListener {
                    d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nombreStr = diagBinding.inputNombre.text?.toString()
                        val correoStr = diagBinding.inputCorreo.text?.toString()
                        val passStr = diagBinding.inputPassword.text?.toString()
                        val telefonoStr = diagBinding.inputTelefono.text?.toString()
                        val cargoStr = diagBinding.inputCargo.text?.toString()

                        val resNombre = Validadores.nombrePersona(nombreStr)
                        val resCorreo = Validadores.correo(correoStr)
                        val resPass = Validadores.contrasenaNueva(passStr, esEdicion)
                        val resTelefono = Validadores.telefono(telefonoStr)
                        val resCargo = Validadores.cargo(cargoStr)

                        var hasError = false
                        if (!resNombre.esOk) { diagBinding.layoutNombre.error = resNombre.mensajeError(); hasError = true }
                        if (!resCorreo.esOk) { diagBinding.layoutCorreo.error = resCorreo.mensajeError(); hasError = true }
                        if (!resPass.esOk) { diagBinding.layoutPassword.error = resPass.mensajeError(); hasError = true }
                        if (!resTelefono.esOk) { diagBinding.layoutTelefono.error = resTelefono.mensajeError(); hasError = true }
                        if (!resCargo.esOk) { diagBinding.layoutCargo.error = resCargo.mensajeError(); hasError = true }

                        if (!hasError) {
                            val map = mutableMapOf<String, Any?>(
                                "nombre" to resNombre.valorOrNull()!!,
                                "correo" to resCorreo.valorOrNull()!!,
                                "telefono" to resTelefono.valorOrNull()!!,
                                "cargo" to resCargo.valorOrNull()!!
                            )
                            if (resPass.valorOrNull() != null) {
                                map["contrasena"] = resPass.valorOrNull()!!
                            }
                            d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                            if (esEdicion && cond != null) {
                                model.editarConductor(cond.id, map)
                            } else {
                                model.crearConductor(map)
                            }
                        }
                    }
                }
                d.show()
            }
            R.id.btnVehiculos -> {
                val veh = item as? Vehiculo
                val diagBinding = DialogFormVehiculoBinding.inflate(LayoutInflater.from(context))

                diagBinding.inputPlaca.filters = FiltrosEntrada.limiteYLargo(15, "^[a-zA-Z0-9 -]+$")
                diagBinding.inputMarca.filters = FiltrosEntrada.limiteYLargo(50, "^[a-zA-Z0-9\\p{L} .\\-/+]+$")
                diagBinding.inputModelo.filters = FiltrosEntrada.limiteYLargo(80, "^[a-zA-Z0-9\\p{L} .\\-/+]+$")
                diagBinding.inputAnio.filters = FiltrosEntrada.limiteYLargo(4, "^[0-9]+$")
                diagBinding.inputRendimiento.filters = FiltrosEntrada.limiteYLargo(5, "^[0-9.,]+$")

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

                val d = MaterialAlertDialogBuilder(context)
                    .setTitle(if (esEdicion) getString(R.string.editar_vehiculo) else getString(R.string.nuevo_vehiculo))
                    .setView(diagBinding.root)
                    .setPositiveButton(R.string.guardar, null)
                    .setNegativeButton(R.string.cancelar) { dialog, _ ->
                        dialog.dismiss()
                        activeDialog = null
                        activeDialogType = null
                        activeVehiculoBinding = null
                    }
                    .create()

                activeDialog = d
                activeDialogType = "vehiculo"
                activeVehiculoBinding = diagBinding

                d.setOnShowListener {
                    d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val placaStr = diagBinding.inputPlaca.text?.toString()
                        val marcaStr = diagBinding.inputMarca.text?.toString()
                        val modeloStr = diagBinding.inputModelo.text?.toString()
                        val anioStr = diagBinding.inputAnio.text?.toString()
                        val combStr = diagBinding.inputCombustible.text?.toString()
                        val rendStr = diagBinding.inputRendimiento.text?.toString()

                        val resPlaca = Validadores.placa(placaStr)
                        val resMarca = Validadores.marcaModelo(marcaStr, "la marca", 50)
                        val resModelo = Validadores.marcaModelo(modeloStr, "el modelo", 80)
                        val resAnio = Validadores.anio(anioStr)
                        val resRend = Validadores.rendimiento(rendStr)

                        var hasError = false
                        if (!resPlaca.esOk) { diagBinding.layoutPlaca.error = resPlaca.mensajeError(); hasError = true }
                        if (!resMarca.esOk) { diagBinding.layoutMarca.error = resMarca.mensajeError(); hasError = true }
                        if (!resModelo.esOk) { diagBinding.layoutModelo.error = resModelo.mensajeError(); hasError = true }
                        if (!resAnio.esOk) { diagBinding.layoutAnio.error = resAnio.mensajeError(); hasError = true }
                        if (!resRend.esOk) { diagBinding.layoutRendimiento.error = resRend.mensajeError(); hasError = true }

                        if (!hasError) {
                            val tipoCombustibleVal = mapaCombustible[combStr?.trim()] ?: "diesel"
                            val map = mutableMapOf<String, Any?>(
                                "placa" to resPlaca.valorOrNull()!!,
                                "marca" to resMarca.valorOrNull()!!,
                                "modelo" to resModelo.valorOrNull()!!,
                                "anio" to resAnio.valorOrNull(),
                                "tipo_combustible" to tipoCombustibleVal
                            )
                            if (resRend.valorOrNull() != null) {
                                map["rendimiento_km_litro"] = resRend.valorOrNull()!!
                            }
                            d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                            if (esEdicion && veh != null) {
                                model.editarVehiculo(veh.id, map)
                            } else {
                                model.crearVehiculo(map)
                            }
                        }
                    }
                }
                d.show()
            }
            R.id.btnGasolineras -> {
                val gas = item as? Gasolinera
                val diagBinding = DialogFormGasolineraBinding.inflate(LayoutInflater.from(context))

                diagBinding.inputNombre.filters = FiltrosEntrada.limiteYLargo(100, "^[\\p{L}0-9 .,\\-&'#()]+$")
                diagBinding.inputDireccion.filters = FiltrosEntrada.limiteYLargo(255, "^[\\p{L}0-9 .,\\-#/()]+$")
                diagBinding.inputQr.filters = FiltrosEntrada.limiteYLargo(100, "^[a-zA-Z0-9_\\-:.]+$")

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
                            val generatedQr = ("QR-" + nombre.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')).take(100)
                            diagBinding.inputQr.setText(generatedQr)
                        }
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })
                diagBinding.inputQr.addTextChangedListener { diagBinding.layoutQr.error = null }
                diagBinding.inputDireccion.addTextChangedListener { diagBinding.layoutDireccion.error = null }

                val d = MaterialAlertDialogBuilder(context)
                    .setTitle(if (esEdicion) getString(R.string.editar_gasolinera) else getString(R.string.nueva_gasolinera))
                    .setView(diagBinding.root)
                    .setPositiveButton(R.string.guardar, null)
                    .setNegativeButton(R.string.cancelar) { dialog, _ ->
                        dialog.dismiss()
                        activeDialog = null
                        activeDialogType = null
                        activeGasolineraBinding = null
                    }
                    .create()

                activeDialog = d
                activeDialogType = "gasolinera"
                activeGasolineraBinding = diagBinding

                d.setOnShowListener {
                    d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nombreStr = diagBinding.inputNombre.text?.toString()
                        val dirStr = diagBinding.inputDireccion.text?.toString()
                        val qrStr = diagBinding.inputQr.text?.toString()

                        val resNombre = Validadores.nombreGasolinera(nombreStr)
                        val resDireccion = Validadores.direccion(dirStr)
                        val resQr = Validadores.codigoQrNuevo(qrStr)

                        var hasError = false
                        if (!resNombre.esOk) { diagBinding.layoutNombre.error = resNombre.mensajeError(); hasError = true }
                        if (!resDireccion.esOk) { diagBinding.layoutDireccion.error = resDireccion.mensajeError(); hasError = true }
                        if (!resQr.esOk) { diagBinding.layoutQr.error = resQr.mensajeError(); hasError = true }

                        if (!hasError) {
                            val map = mapOf(
                                "nombre" to resNombre.valorOrNull()!!,
                                "direccion" to resDireccion.valorOrNull(),
                                "codigo_qr" to resQr.valorOrNull()!!
                            )
                            d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                            if (esEdicion && gas != null) {
                                model.editarGasolinera(gas.id, map)
                            } else {
                                model.crearGasolinera(map)
                            }
                        }
                    }
                }
                d.show()
            }
        }
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
                iconRes = R.drawable.ic_vehiculo
            )
        }
    }

    private fun pintar(loading: Boolean, error: String?, items: List<FilaDoble>?) {
        val b = _binding ?: return
        b.progress.isVisible = loading
        b.textError.isVisible = error != null
        b.textError.text = error
        b.recycler.adapter = FilaDobleAdapter(items.orEmpty()) { id, titulo ->
            mostrarDialogoOpciones(id, titulo)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

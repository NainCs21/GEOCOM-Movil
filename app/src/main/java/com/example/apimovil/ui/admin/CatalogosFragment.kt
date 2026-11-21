package com.example.apimovil.ui.admin

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
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
import com.example.apimovil.databinding.FragmentCatalogosBinding
import com.example.apimovil.ui.common.configurarNavegacionAdmin
import com.example.apimovil.ui.common.configurarNavegacionSupervisor
import com.example.apimovil.ui.common.formatoCargo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch

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
                        pintar(estado.loading, estado.error, estado.data?.map {
                            FilaDoble(
                                id = it.id,
                                titulo = it.usuario?.name ?: getString(R.string.conductor_numero, it.id),
                                subtitulo = "${it.telefono} · ${formatoCargo(it.cargo)}" +
                                    if (!it.activo) " · " + getString(R.string.inactivo) else "",
                                iconRes = R.drawable.ic_person
                            )
                        })
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
                                iconRes = R.drawable.ic_gas_pump
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

    private fun campoTexto(view: View, bottomMarginDp: Int = 16): TextInputLayout {
        val density = resources.displayMetrics.density
        return TextInputLayout(
            requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            addView(view)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (bottomMarginDp * density).toInt() }
        }
    }

    private fun mostrarDialogoFormulario(item: Any?) {
        val context = requireContext()
        val builder = MaterialAlertDialogBuilder(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val d = resources.displayMetrics.density
            setPadding((24 * d).toInt(), (16 * d).toInt(), (24 * d).toInt(), (8 * d).toInt())
        }

        val esEdicion = item != null

        when (binding.toggleCatalogo.checkedButtonId) {
            R.id.btnConductores -> {
                val cond = item as? Conductor
                builder.setTitle(if (esEdicion) "Editar conductor" else getString(R.string.nuevo_conductor))
                val inputNombre = TextInputEditText(context).apply { hint = getString(R.string.nombre); setText(cond?.usuario?.name) }
                val inputCorreo = TextInputEditText(context).apply { hint = getString(R.string.correo); inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS; setText(cond?.usuario?.email) }
                val inputPassword = TextInputEditText(context).apply { hint = getString(R.string.contrasena) + if (esEdicion) " (opcional)" else ""; inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD }
                val inputTelefono = TextInputEditText(context).apply { hint = getString(R.string.telefono); inputType = InputType.TYPE_CLASS_PHONE; setText(cond?.telefono) }
                val inputCargo = TextInputEditText(context).apply { hint = getString(R.string.cargo); setText(cond?.cargo) }

                layout.addView(campoTexto(inputNombre))
                layout.addView(campoTexto(inputCorreo))
                layout.addView(campoTexto(inputPassword))
                layout.addView(campoTexto(inputTelefono))
                layout.addView(campoTexto(inputCargo, 0))

                builder.setView(layout)
                builder.setPositiveButton(R.string.guardar) { _, _ ->
                    val pass = inputPassword.text?.toString()
                    val map = mutableMapOf<String, Any?>(
                        "nombre" to inputNombre.text?.toString()?.trim(),
                        "correo" to inputCorreo.text?.toString()?.trim(),
                        "telefono" to inputTelefono.text?.toString()?.trim(),
                        "cargo" to inputCargo.text?.toString()?.trim()
                    )
                    if (!pass.isNullOrBlank()) {
                        map["contrasena"] = pass
                    }
                    if (esEdicion && cond != null) {
                        model.editarConductor(cond.id, map)
                    } else {
                        model.crearConductor(map)
                    }
                }
            }
            R.id.btnVehiculos -> {
                val veh = item as? Vehiculo
                builder.setTitle(if (esEdicion) "Editar vehículo" else getString(R.string.nuevo_vehiculo))
                val inputPlaca = TextInputEditText(context).apply { hint = getString(R.string.placa); setText(veh?.placa) }
                val inputMarca = TextInputEditText(context).apply { hint = getString(R.string.marca); setText(veh?.marca) }
                val inputModelo = TextInputEditText(context).apply { hint = getString(R.string.modelo); setText(veh?.modelo) }
                val inputAnio = TextInputEditText(context).apply { hint = getString(R.string.anio); inputType = InputType.TYPE_CLASS_NUMBER; setText(veh?.anio?.toString()) }

                val opcionesCombustible = arrayOf("Diesel", "Gasolina")
                val mapaCombustible = mapOf("Diesel" to "diesel", "Gasolina" to "gasolina", "Diésel" to "diesel")
                val textoInicialCombustible = if (esEdicion && veh != null) {
                    when (veh.tipo_combustible.lowercase()) {
                        "gasolina" -> "Gasolina"
                        else -> "Diesel"
                    }
                } else {
                    ""
                }

                val inputCombustible = TextInputEditText(context).apply {
                    hint = getString(R.string.tipo_combustible)
                    setText(textoInicialCombustible)
                    isFocusable = false
                    isClickable = true
                    keyListener = null
                    setOnClickListener {
                        MaterialAlertDialogBuilder(context)
                            .setTitle(getString(R.string.tipo_combustible))
                            .setItems(opcionesCombustible) { _, which ->
                                setText(opcionesCombustible[which])
                            }
                            .show()
                    }
                }

                layout.addView(campoTexto(inputPlaca))
                layout.addView(campoTexto(inputMarca))
                layout.addView(campoTexto(inputModelo))
                layout.addView(campoTexto(inputAnio))
                layout.addView(campoTexto(inputCombustible, 0))

                builder.setView(layout)
                builder.setPositiveButton(R.string.guardar) { _, _ ->
                    val anioVal = inputAnio.text?.toString()?.toIntOrNull()
                    val seleccionCombustible = inputCombustible.text?.toString()?.trim()
                    val tipoCombustibleVal = mapaCombustible[seleccionCombustible] ?: "diesel"
                    val map = mapOf(
                        "placa" to inputPlaca.text?.toString()?.trim(),
                        "marca" to inputMarca.text?.toString()?.trim(),
                        "modelo" to inputModelo.text?.toString()?.trim(),
                        "anio" to anioVal,
                        "tipo_combustible" to tipoCombustibleVal
                    )
                    if (esEdicion && veh != null) {
                        model.editarVehiculo(veh.id, map)
                    } else {
                        model.crearVehiculo(map)
                    }
                }
            }
            R.id.btnGasolineras -> {
                val gas = item as? Gasolinera
                builder.setTitle(if (esEdicion) "Editar gasolinera" else getString(R.string.nueva_gasolinera))
                val inputNombre = TextInputEditText(context).apply { hint = getString(R.string.nombre); setText(gas?.nombre) }
                val inputDireccion = TextInputEditText(context).apply { hint = getString(R.string.direccion); setText(gas?.direccion) }
                val inputQr = TextInputEditText(context).apply { hint = getString(R.string.codigo_qr); setText(gas?.codigo_qr) }

                if (!esEdicion) {
                    inputNombre.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            val nombre = s?.toString()?.trim().orEmpty()
                            val generatedQr = "QR-" + nombre.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')
                            inputQr.setText(generatedQr)
                        }
                        override fun afterTextChanged(s: Editable?) {}
                    })
                }

                layout.addView(campoTexto(inputNombre))
                layout.addView(campoTexto(inputDireccion))
                layout.addView(campoTexto(inputQr, 0))

                builder.setView(layout)
                builder.setPositiveButton(R.string.guardar) { _, _ ->
                    val map = mapOf(
                        "nombre"     to inputNombre.text?.toString()?.trim(),
                        "direccion"  to inputDireccion.text?.toString()?.trim(),
                        "codigo_qr"  to inputQr.text?.toString()?.trim()
                    )
                    if (esEdicion && gas != null) {
                        model.editarGasolinera(gas.id, map)
                    } else {
                        model.crearGasolinera(map)
                    }
                }
            }
        }
        builder.setNegativeButton(R.string.cancelar, null)
        builder.show()
    }

    private fun mostrarConductores() {
        val b = _binding ?: return
        b.toggleCatalogo.check(R.id.btnConductores)
        b.layoutFiltrosVehiculos.isVisible = false
        b.btnNuevo.text = getString(R.string.nuevo_conductor_btn)
        model.cargarConductores()
    }

    private fun mostrarVehiculos() {
        val b = _binding ?: return
        b.toggleCatalogo.check(R.id.btnVehiculos)
        b.layoutFiltrosVehiculos.isVisible = true
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
            FilaDoble(
                id = it.id,
                titulo = "${it.placa} · ${it.marca} ${it.modelo}",
                subtitulo = it.tipo_combustible +
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

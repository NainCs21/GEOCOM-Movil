package com.example.apimovil.ui.admin

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.conductores.collect { estado ->
                    if (binding.toggleCatalogo.checkedButtonId == R.id.btnConductores) {
                        pintar(estado.loading, estado.error, estado.data?.map {
                            FilaDoble(
                                id = it.id,
                                titulo = it.usuario?.name ?: getString(R.string.conductor_numero, it.id),
                                subtitulo = "${it.telefono} · ${it.cargo}" +
                                    if (!it.activo) " · " + getString(R.string.inactivo) else ""
                            )
                        })
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.vehiculos.collect { estado ->
                    if (binding.toggleCatalogo.checkedButtonId == R.id.btnVehiculos) {
                        pintar(estado.loading, estado.error, estado.data?.map {
                            FilaDoble(
                                id = it.id,
                                titulo = "${it.placa} · ${it.marca} ${it.modelo}",
                                subtitulo = it.tipo_combustible +
                                    if (!it.activo) " · " + getString(R.string.inactivo) else ""
                            )
                        })
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.gasolineras.collect { estado ->
                    if (binding.toggleCatalogo.checkedButtonId == R.id.btnGasolineras) {
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
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.accionCatalogo.collect { estado ->
                    if (estado.error != null) {
                        Toast.makeText(requireContext(), estado.error, Toast.LENGTH_SHORT).show()
                    } else if (estado.data == true) {
                        Toast.makeText(requireContext(), R.string.registro_exitoso, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun mostrarDialogoOpciones(id: Int, titulo: String) {
        if (!SessionManager.esAdmin) return
        val context = requireContext()
        val esGasolinera = binding.toggleCatalogo.checkedButtonId == R.id.btnGasolineras

        val opciones = if (esGasolinera) {
            arrayOf(getString(R.string.ver_codigo_qr), getString(R.string.editar), getString(R.string.eliminar))
        } else {
            arrayOf(getString(R.string.editar), getString(R.string.eliminar))
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(titulo)
            .setItems(opciones) { _, which ->
                if (esGasolinera) {
                    when (which) {
                        0 -> mostrarDialogoVerQr(id)
                        1 -> mostrarDialogoEditar(id)
                        2 -> confirmarEliminar(id)
                    }
                } else {
                    when (which) {
                        0 -> mostrarDialogoEditar(id)
                        1 -> confirmarEliminar(id)
                    }
                }
            }
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

    private fun confirmarEliminar(id: Int) {
        val context = requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.eliminar)
            .setMessage(R.string.que_deseas_hacer)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                when (binding.toggleCatalogo.checkedButtonId) {
                    R.id.btnConductores -> model.eliminarConductor(id)
                    R.id.btnVehiculos -> model.eliminarVehiculo(id)
                    R.id.btnGasolineras -> model.eliminarGasolinera(id)
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
        val builder = MaterialAlertDialogBuilder(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
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

                layout.addView(TextInputLayout(context).apply { addView(inputNombre); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 } })
                layout.addView(TextInputLayout(context).apply { addView(inputCorreo); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 } })
                layout.addView(TextInputLayout(context).apply { addView(inputPassword); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 } })
                layout.addView(TextInputLayout(context).apply { addView(inputTelefono); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 } })
                layout.addView(TextInputLayout(context).apply { addView(inputCargo); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) })

                builder.setView(layout)
                builder.setPositiveButton(R.string.guardar) { _, _ ->
                    val pass = inputPassword.text?.toString()
                    val map = mutableMapOf<String, Any?>(
                        "name" to inputNombre.text?.toString()?.trim(),
                        "email" to inputCorreo.text?.toString()?.trim(),
                        "telefono" to inputTelefono.text?.toString()?.trim(),
                        "cargo" to inputCargo.text?.toString()?.trim()
                    )
                    if (!pass.isNullOrBlank()) {
                        map["password"] = pass
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

                val opcionesCombustible = arrayOf("Regular", "Super", "Diesel")
                val adapterCombustible = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, opcionesCombustible)
                val inputCombustible = AutoCompleteTextView(context).apply {
                    hint = getString(R.string.tipo_combustible)
                    setAdapter(adapterCombustible)
                    setText(veh?.tipo_combustible ?: "Regular", false)
                    inputType = InputType.TYPE_NULL
                }

                layout.addView(TextInputLayout(context).apply { addView(inputPlaca); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 } })
                layout.addView(TextInputLayout(context).apply { addView(inputMarca); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 } })
                layout.addView(TextInputLayout(context).apply { addView(inputModelo); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 } })
                layout.addView(TextInputLayout(context).apply { addView(inputAnio); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 } })
                layout.addView(TextInputLayout(context, null, com.google.android.material.R.attr.textInputStyle).apply {
                    addView(inputCombustible)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                })

                builder.setView(layout)
                builder.setPositiveButton(R.string.guardar) { _, _ ->
                    val anioVal = inputAnio.text?.toString()?.toIntOrNull()
                    val map = mapOf(
                        "placa" to inputPlaca.text?.toString()?.trim(),
                        "marca" to inputMarca.text?.toString()?.trim(),
                        "modelo" to inputModelo.text?.toString()?.trim(),
                        "anio" to anioVal,
                        "tipo_combustible" to inputCombustible.text?.toString()?.trim()
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

                layout.addView(TextInputLayout(context).apply { addView(inputNombre); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 } })
                layout.addView(TextInputLayout(context).apply { addView(inputDireccion); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 } })
                layout.addView(TextInputLayout(context).apply { addView(inputQr); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) })

                builder.setView(layout)
                builder.setPositiveButton(R.string.guardar) { _, _ ->
                    val map = mapOf(
                        "nombre" to inputNombre.text?.toString()?.trim(),
                        "direccion" to inputDireccion.text?.toString()?.trim(),
                        "codigo_qr" to inputQr.text?.toString()?.trim()
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
        binding.toggleCatalogo.check(R.id.btnConductores)
        model.cargarConductores()
    }

    private fun mostrarVehiculos() {
        binding.toggleCatalogo.check(R.id.btnVehiculos)
        model.cargarVehiculos()
    }

    private fun mostrarGasolineras() {
        binding.toggleCatalogo.check(R.id.btnGasolineras)
        model.cargarGasolineras()
    }

    private fun pintar(loading: Boolean, error: String?, items: List<FilaDoble>?) {
        binding.progress.isVisible = loading
        binding.textError.isVisible = error != null
        binding.textError.text = error
        binding.recycler.adapter = FilaDobleAdapter(items.orEmpty()) { id, titulo ->
            mostrarDialogoOpciones(id, titulo)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

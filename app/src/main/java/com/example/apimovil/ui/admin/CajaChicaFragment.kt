package com.example.apimovil.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.models.Conductor
import com.example.apimovil.databinding.FragmentCajaChicaBinding
import com.example.apimovil.databinding.ItemFilaDobleBinding
import com.example.apimovil.ui.common.FiltrosEntrada
import com.example.apimovil.ui.common.Validadores
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CajaChicaFragment : Fragment(R.layout.fragment_caja_chica) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentCajaChicaBinding? = null
    private val binding get() = _binding!!

    private var listaConductoresActivos: List<Conductor> = emptyList()
    private var conductorIdSeleccionado: Int? = null
    private var nombreConductorSeleccionado: String? = null

    private var fechaEntregaApi: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCajaChicaBinding.bind(view)

        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.contenedorRegistro.isVisible = SessionManager.esAdmin

        binding.inputMonto.filters = FiltrosEntrada.limiteYLargo(11, "^[0-9.,]+$")
        binding.inputDescripcion.filters = FiltrosEntrada.limiteYLargo(255)

        val sdfDisplay = SimpleDateFormat("dd MMM yyyy", Locale("es", "HN"))
        binding.inputFecha.setText(sdfDisplay.format(Date()))

        // Listener para selector de fecha
        val abrirDatePicker = View.OnClickListener {
            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build()

            val picker = MaterialDatePicker.Builder.datePicker()
                .setCalendarConstraints(constraints)
                .setTitleText(R.string.fecha_de_entrega)
                .build()

            picker.addOnPositiveButtonClickListener { selectionMillis ->
                val dateUtc = Date(selectionMillis)
                val sdfApi = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val sdfView = SimpleDateFormat("dd MMM yyyy", Locale("es", "HN")).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                fechaEntregaApi = sdfApi.format(dateUtc)
                binding.inputFecha.setText(sdfView.format(dateUtc))
                binding.layoutFecha.error = null
            }
            picker.show(parentFragmentManager, "datePickerCajaChica")
        }
        binding.inputFecha.setOnClickListener(abrirDatePicker)
        binding.layoutFecha.setOnClickListener(abrirDatePicker)

        // Chips de montos rápidos
        binding.chipMonto100.setOnClickListener {
            binding.inputMonto.setText("100")
            binding.layoutMonto.error = null
            actualizarResumen()
        }
        binding.chipMonto200.setOnClickListener {
            binding.inputMonto.setText("200")
            binding.layoutMonto.error = null
            actualizarResumen()
        }
        binding.chipMonto500.setOnClickListener {
            binding.inputMonto.setText("500")
            binding.layoutMonto.error = null
            actualizarResumen()
        }
        binding.chipMonto1000.setOnClickListener {
            binding.inputMonto.setText("1000")
            binding.layoutMonto.error = null
            actualizarResumen()
        }

        // Chips de motivos frecuentes
        binding.chipMotivoViaticos.setOnClickListener {
            binding.inputDescripcion.setText(R.string.motivo_viaticos)
            binding.layoutDescripcion.error = null
        }
        binding.chipMotivoCombustible.setOnClickListener {
            binding.inputDescripcion.setText(R.string.motivo_combustible)
            binding.layoutDescripcion.error = null
        }
        binding.chipMotivoMantenimiento.setOnClickListener {
            binding.inputDescripcion.setText(R.string.motivo_mantenimiento)
            binding.layoutDescripcion.error = null
        }
        binding.chipMotivoCompra.setOnClickListener {
            binding.inputDescripcion.setText(R.string.motivo_compra)
            binding.layoutDescripcion.error = null
        }
        binding.chipMotivoOtro.setOnClickListener {
            binding.inputDescripcion.setText(R.string.motivo_otro)
            binding.layoutDescripcion.error = null
        }

        // Limpieza de errores al escribir y actualización de resumen
        binding.inputMonto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.layoutMonto.error = null
                binding.textError.isVisible = false
                actualizarResumen()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.inputDescripcion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.layoutDescripcion.error = null
                binding.textError.isVisible = false
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnRegistrar.setOnClickListener {
            binding.layoutMonto.error = null
            binding.layoutDescripcion.error = null
            binding.layoutFecha.error = null
            binding.textError.isVisible = false

            val resMonto = Validadores.monto(binding.inputMonto.text?.toString(), "monto")
            if (!resMonto.esOk) {
                binding.layoutMonto.error = resMonto.mensajeError()
                return@setOnClickListener
            }

            val resDesc = Validadores.textoLibre(
                binding.inputDescripcion.text?.toString(),
                "el motivo o descripción",
                3,
                255,
                esObligatorio = true
            )
            if (!resDesc.esOk) {
                binding.layoutDescripcion.error = resDesc.mensajeError()
                return@setOnClickListener
            }

            val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            if (fechaEntregaApi.isBlank()) {
                binding.layoutFecha.error = "Elige la fecha de entrega."
                return@setOnClickListener
            }
            if (fechaEntregaApi > hoyStr) {
                binding.layoutFecha.error = "La fecha no puede ser futura."
                return@setOnClickListener
            }

            model.registrarCajaChica(
                conductorIdSeleccionado,
                resMonto.valorOrNull()!!,
                resDesc.valorOrNull()!!,
                fechaEntregaApi
            )
        }

        if (SessionManager.esAdmin) {
            model.cargarConductores()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.conductores.collect { estado ->
                        val conductoresActivos = estado.data.orEmpty().filter { it.activo }
                        listaConductoresActivos = conductoresActivos

                        val opciones = mutableListOf(getString(R.string.entrega_general))
                        opciones.addAll(conductoresActivos.map {
                            it.usuario?.name ?: getString(R.string.conductor_numero, it.id)
                        })

                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, opciones)
                        binding.inputConductor.setAdapter(adapter)

                        if (binding.inputConductor.text.isNullOrEmpty()) {
                            binding.inputConductor.setText(getString(R.string.entrega_general), false)
                        }

                        binding.inputConductor.setOnItemClickListener { _, _, position, _ ->
                            if (position == 0) {
                                conductorIdSeleccionado = null
                                nombreConductorSeleccionado = null
                            } else if (position - 1 in conductoresActivos.indices) {
                                val cond = conductoresActivos[position - 1]
                                conductorIdSeleccionado = cond.id
                                nombreConductorSeleccionado = cond.usuario?.name
                                    ?: getString(R.string.conductor_numero, cond.id)
                            }
                            actualizarResumen()
                        }
                    }
                }

                launch {
                    model.registroCajaChica.collect { estado ->
                        binding.progress.isVisible = estado.loading
                        binding.btnRegistrar.isEnabled = !estado.loading

                        if (estado.error != null) {
                            binding.textError.isVisible = true
                            binding.textError.text = estado.error
                        }

                        if (estado.data == true) {
                            Snackbar.make(binding.root, R.string.entrega_registrada_exito, Snackbar.LENGTH_LONG).show()

                            // Limpiar campos
                            conductorIdSeleccionado = null
                            nombreConductorSeleccionado = null
                            binding.inputConductor.setText(getString(R.string.entrega_general), false)
                            binding.inputMonto.text?.clear()
                            binding.inputDescripcion.text?.clear()

                            fechaEntregaApi = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            binding.inputFecha.setText(SimpleDateFormat("dd MMM yyyy", Locale("es", "HN")).format(Date()))

                            binding.layoutMonto.error = null
                            binding.layoutDescripcion.error = null
                            binding.layoutFecha.error = null
                            binding.textError.isVisible = false
                            actualizarResumen()

                            model.cargarCajaChica()
                        }
                    }
                }

                launch {
                    model.cajaChica.collect { estado ->
                        binding.progressLista.isVisible = estado.loading
                        binding.contenedorListado.removeAllViews()
                        estado.data.orEmpty().forEach { entrega ->
                            val fila = ItemFilaDobleBinding.inflate(
                                LayoutInflater.from(requireContext()), binding.contenedorListado, false
                            )
                            fila.textTitulo.text = entrega.conductor?.usuario?.name
                                ?: getString(R.string.conductor_numero, entrega.conductor_id ?: 0)
                            fila.textSubtitulo.text = getString(
                                R.string.monto_y_descripcion, entrega.monto, entrega.descripcion
                            )
                            binding.contenedorListado.addView(fila.root)
                        }
                    }
                }
            }
        }

        actualizarResumen()
        model.cargarCajaChica()
    }

    private fun actualizarResumen() {
        val montoText = binding.inputMonto.text?.toString()?.trim().orEmpty()
        val montoVal = Validadores.monto(montoText, "monto").valorOrNull()
        val montoFmt = if (montoVal != null) String.format(Locale.US, "%,.2f", montoVal) else "0.00"

        val textoResumen = if (nombreConductorSeleccionado != null) {
            getString(R.string.resumen_entrega_conductor, montoFmt, nombreConductorSeleccionado)
        } else {
            getString(R.string.resumen_entrega_general, montoFmt)
        }

        binding.textResumenEntrega.text = textoResumen
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

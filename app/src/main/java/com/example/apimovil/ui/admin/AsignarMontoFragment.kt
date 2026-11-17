package com.example.apimovil.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.apimovil.data.models.DetalleLineaRequest
import com.example.apimovil.data.models.Gasolinera
import com.example.apimovil.data.models.Vehiculo
import com.example.apimovil.databinding.FragmentAsignarMontoBinding
import kotlinx.coroutines.launch

class AsignarMontoFragment : Fragment(R.layout.fragment_asignar_monto) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentAsignarMontoBinding? = null
    private val binding get() = _binding!!

    private var listaGasolineras: List<Gasolinera> = emptyList()
    private var gasolineraIdSeleccionada: Int? = null
    private var listaVehiculos: List<Vehiculo> = emptyList()

    private val actualizarTotal = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) = recalcularTotal()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentAsignarMontoBinding.bind(view)

        val solicitudId = arguments?.getInt("solicitudId") ?: -1
        val tipoDestino = arguments?.getString("tipoDestino") ?: "vehiculo"
        var vehiculoId = arguments?.takeIf { it.containsKey("vehiculoId") }?.getInt("vehiculoId")
        val descripcionMgp = arguments?.getString("descripcionMgp")
        val descripcionActividad = arguments?.getString("descripcionActividad").orEmpty()
        val conductorNombre = arguments?.getString("conductorNombre")
        val conductorId = arguments?.getInt("conductorId") ?: 0

        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.textResumenConductor.text = conductorNombre ?: getString(R.string.conductor_numero, conductorId)
        binding.textResumenActividad.text = descripcionActividad
        binding.textResumenDestino.text = when {
            tipoDestino.equals("vehiculo", ignoreCase = true) -> getString(R.string.vehiculo)
            tipoDestino.equals("mgp", ignoreCase = true) -> getString(R.string.planta_generadora)
            else -> getString(R.string.vehiculo) + " + " + getString(R.string.planta_generadora)
        }

        val esVehiculo = tipoDestino.equals("vehiculo", ignoreCase = true) ||
                         tipoDestino.equals("ambos", ignoreCase = true) ||
                         tipoDestino.contains("vehiculo", ignoreCase = true) ||
                         tipoDestino.contains("+", ignoreCase = true)
        val esMgp = tipoDestino.equals("mgp", ignoreCase = true) ||
                    tipoDestino.equals("ambos", ignoreCase = true) ||
                    tipoDestino.contains("mgp", ignoreCase = true) ||
                    tipoDestino.contains("+", ignoreCase = true)

        binding.tarjetaMontoVehiculo.isVisible = esVehiculo
        binding.tarjetaMontoMgp.isVisible = esMgp

        binding.inputMontoVehiculo.addTextChangedListener(actualizarTotal)
        binding.inputMontoMgp.addTextChangedListener(actualizarTotal)

        binding.btnMonto500Vehiculo.setOnClickListener { binding.inputMontoVehiculo.setText("500") }
        binding.btnMonto1000Vehiculo.setOnClickListener { binding.inputMontoVehiculo.setText("1000") }
        binding.btnMonto1500Vehiculo.setOnClickListener { binding.inputMontoVehiculo.setText("1500") }
        binding.btnMonto500Mgp.setOnClickListener { binding.inputMontoMgp.setText("500") }
        binding.btnMonto1000Mgp.setOnClickListener { binding.inputMontoMgp.setText("1000") }
        binding.btnMonto1500Mgp.setOnClickListener { binding.inputMontoMgp.setText("1500") }

        recalcularTotal()

        binding.spinnerGasolinera.setOnItemClickListener { _, _, position, _ ->
            if (position in listaGasolineras.indices) {
                gasolineraIdSeleccionada = listaGasolineras[position].id
            }
        }

        model.cargarGasolineras()
        model.cargarVehiculos()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.vehiculos.collect { estado ->
                        listaVehiculos = estado.data.orEmpty()
                        if (vehiculoId == null && listaVehiculos.isNotEmpty()) {
                            vehiculoId = listaVehiculos.first().id
                        }
                    }
                }

                launch {
                    model.gasolineras.collect { estado ->
                        val gasolineras = estado.data.orEmpty().filter { it.activo }
                        listaGasolineras = gasolineras
                        if (gasolineras.isNotEmpty()) {
                            val nombres = gasolineras.map { g ->
                                if (!g.direccion.isNullOrBlank()) "${g.nombre} (${g.direccion})" else g.nombre
                            }
                            binding.spinnerGasolinera.setAdapter(
                                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, nombres),
                            )
                            if ((gasolineraIdSeleccionada == null) || binding.spinnerGasolinera.text.isNullOrEmpty()) {
                                val primera = gasolineras.first()
                                gasolineraIdSeleccionada = primera.id
                                binding.spinnerGasolinera.setText(nombres.first(), false)
                            }
                        } else {
                            binding.spinnerGasolinera.setText("", false)
                            gasolineraIdSeleccionada = null
                        }
                    }
                }

                launch {
                    model.asignacion.collect { estado ->
                        binding.progress.isVisible = estado.loading
                        binding.btnAsignar.isEnabled = !estado.loading
                        if (estado.error != null) {
                            binding.textError.isVisible = true
                            binding.textError.text = estado.error
                        }
                        if (estado.data == true) {
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }

        binding.btnAsignar.setOnClickListener {
            val gasolineraId = gasolineraIdSeleccionada ?: run {
                val txt = binding.spinnerGasolinera.text?.toString()?.trim().orEmpty()
                listaGasolineras.find { g -> txt.contains(g.nombre, ignoreCase = true) }?.id
            }
            val horas = binding.inputHorasVigencia.text?.toString()?.toIntOrNull()
            if (gasolineraId == null || horas == null || horas !in 1..3) {
                binding.textError.isVisible = true
                binding.textError.text = getString(R.string.completa_gasolinera_y_vigencia)
                return@setOnClickListener
            }

            val detalles = mutableListOf<DetalleLineaRequest>()
            if (esVehiculo) {
                val monto = binding.inputMontoVehiculo.text?.toString()?.toDoubleOrNull()
                if (monto != null && monto > 0) {
                    val targetVehiculoId = vehiculoId ?: listaVehiculos.firstOrNull()?.id ?: 1
                    detalles += DetalleLineaRequest("vehiculo", vehiculo_id = targetVehiculoId, monto = monto)
                }
            }
            if (esMgp) {
                val monto = binding.inputMontoMgp.text?.toString()?.toDoubleOrNull()
                if (monto != null && monto > 0) {
                    detalles += DetalleLineaRequest("mgp", descripcion_mgp = descripcionMgp, monto = monto)
                }
            }

            if (detalles.isEmpty()) {
                binding.textError.isVisible = true
                binding.textError.text = getString(R.string.ingresa_al_menos_un_monto)
                return@setOnClickListener
            }

            model.asignarOrden(solicitudId, gasolineraId, horas, detalles)
        }
    }

    private fun recalcularTotal() {
        val montoVehiculo = if (binding.tarjetaMontoVehiculo.isVisible)
            binding.inputMontoVehiculo.text?.toString()?.toDoubleOrNull() ?: 0.0 else 0.0
        val montoMgp = if (binding.tarjetaMontoMgp.isVisible)
            binding.inputMontoMgp.text?.toString()?.toDoubleOrNull() ?: 0.0 else 0.0
        binding.textTotalAsignado.text = getString(R.string.total_de_la_solicitud, montoVehiculo + montoMgp)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

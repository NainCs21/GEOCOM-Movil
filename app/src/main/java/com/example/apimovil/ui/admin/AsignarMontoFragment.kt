package com.example.apimovil.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
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
import com.example.apimovil.R
import com.example.apimovil.data.models.DetalleLineaRequest
import com.example.apimovil.data.models.Gasolinera
import com.example.apimovil.data.models.Vehiculo
import com.example.apimovil.databinding.FragmentAsignarMontoBinding
import com.example.apimovil.ui.common.FiltrosEntrada
import com.example.apimovil.ui.common.Validadores
import com.example.apimovil.ui.common.formatearFechaIsoShort
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AsignarMontoFragment : Fragment(R.layout.fragment_asignar_monto) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentAsignarMontoBinding? = null
    private val binding get() = _binding!!

    private var listaGasolineras: List<Gasolinera> = emptyList()
    private var gasolineraIdSeleccionada: Int? = null
    private var listaVehiculos: List<Vehiculo> = emptyList()
    private var vehiculoIdSolicitud: Int? = null

    private var galonesVehiculo: Double = 0.0
    private var galonesMgp: Double = 0.0

    private val actualizarTotal = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            binding.layoutMontoVehiculo.error = null
            binding.layoutMontoMgp.error = null
            binding.textError.isVisible = false
        }
        override fun afterTextChanged(s: Editable?) = recalcularTotal()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentAsignarMontoBinding.bind(view)

        val solicitudId = arguments?.getInt("solicitudId") ?: -1
        if (!Validadores.idValido(solicitudId)) {
            Toast.makeText(requireContext(), "No se pudo abrir esta pantalla. Vuelve a intentarlo.", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        val tipoDestino = arguments?.getString("tipoDestino") ?: "vehiculo"
        vehiculoIdSolicitud = arguments?.takeIf { it.containsKey("vehiculoId") }?.getInt("vehiculoId")
        val descripcionMgp = arguments?.getString("descripcionMgp")
        val descripcionActividad = arguments?.getString("descripcionActividad").orEmpty()
        val conductorNombre = arguments?.getString("conductorNombre")
        val conductorId = arguments?.getInt("conductorId") ?: 0

        binding.inputHorasVigencia.filters = FiltrosEntrada.limiteYLargo(1, "^[1-3]$")
        binding.inputMontoVehiculo.filters = FiltrosEntrada.limiteYLargo(11, "^[0-9.,]+$")
        binding.inputMontoMgp.filters = FiltrosEntrada.limiteYLargo(11, "^[0-9.,]+$")

        binding.inputHorasVigencia.addTextChangedListener { binding.layoutHorasVigencia.error = null }
        binding.spinnerGasolinera.addTextChangedListener { binding.layoutGasolinera.error = null }

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

        binding.btnMonto100Vehiculo.setOnClickListener {
            binding.inputMontoVehiculo.setText("100")
            binding.layoutMontoVehiculo.error = null
            recalcularTotal()
        }
        binding.btnMonto200Vehiculo.setOnClickListener {
            binding.inputMontoVehiculo.setText("200")
            binding.layoutMontoVehiculo.error = null
            recalcularTotal()
        }
        binding.btnMonto500Vehiculo.setOnClickListener {
            binding.inputMontoVehiculo.setText("500")
            binding.layoutMontoVehiculo.error = null
            recalcularTotal()
        }
        binding.btnMonto1000Vehiculo.setOnClickListener {
            binding.inputMontoVehiculo.setText("1000")
            binding.layoutMontoVehiculo.error = null
            recalcularTotal()
        }

        binding.btnMonto100Mgp.setOnClickListener {
            binding.inputMontoMgp.setText("100")
            binding.layoutMontoMgp.error = null
            recalcularTotal()
        }
        binding.btnMonto200Mgp.setOnClickListener {
            binding.inputMontoMgp.setText("200")
            binding.layoutMontoMgp.error = null
            recalcularTotal()
        }
        binding.btnMonto500Mgp.setOnClickListener {
            binding.inputMontoMgp.setText("500")
            binding.layoutMontoMgp.error = null
            recalcularTotal()
        }
        binding.btnMonto1000Mgp.setOnClickListener {
            binding.inputMontoMgp.setText("1000")
            binding.layoutMontoMgp.error = null
            recalcularTotal()
        }

        recalcularTotal()

        binding.spinnerGasolinera.setOnItemClickListener { _, _, position, _ ->
            if (position in listaGasolineras.indices) {
                gasolineraIdSeleccionada = listaGasolineras[position].id
                binding.layoutGasolinera.error = null
            }
        }

        model.cargarGasolineras()
        model.cargarVehiculos()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.vehiculos.collect { estado ->
                        listaVehiculos = estado.data.orEmpty()
                        recalcularTotal()
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

                launch {
                    model.estimacionVehiculo.collect { estado ->
                        if (estado.error != null) {
                            galonesVehiculo = 0.0
                            binding.textEstimacionVehiculo.isVisible = true
                            binding.textEstimacionVehiculo.text = getString(R.string.estimacion_no_disponible)
                            recalcularTotal()
                        } else if (estado.data != null) {
                            val est = estado.data
                            galonesVehiculo = est.galones_aproximados
                            val combNombre = capitalizarCombustible(est.combustible)
                            val fechaVigente = formatearFechaIsoShort(est.vigente_desde)
                            binding.textEstimacionVehiculo.isVisible = true
                            binding.textEstimacionVehiculo.text = getString(
                                R.string.estimacion_vehiculo,
                                est.galones_aproximados,
                                est.litros_aproximados,
                                est.kilometros_aproximados ?: 0.0,
                                combNombre,
                                est.precio_por_galon,
                                fechaVigente
                            )
                            recalcularTotal()
                        } else if (!estado.loading) {
                            galonesVehiculo = 0.0
                            binding.textEstimacionVehiculo.isVisible = false
                            recalcularTotal()
                        }
                    }
                }

                launch {
                    model.estimacionMgp.collect { estado ->
                        if (estado.error != null) {
                            galonesMgp = 0.0
                            binding.textEstimacionMgp.isVisible = true
                            binding.textEstimacionMgp.text = getString(R.string.estimacion_no_disponible)
                            recalcularTotal()
                        } else if (estado.data != null) {
                            val est = estado.data
                            galonesMgp = est.galones_aproximados
                            val combNombre = capitalizarCombustible(est.combustible)
                            val fechaVigente = formatearFechaIsoShort(est.vigente_desde)
                            binding.textEstimacionMgp.isVisible = true
                            binding.textEstimacionMgp.text = getString(
                                R.string.estimacion_mgp,
                                est.galones_aproximados,
                                est.litros_aproximados,
                                combNombre,
                                est.precio_por_galon,
                                fechaVigente
                            )
                            recalcularTotal()
                        } else if (!estado.loading) {
                            galonesMgp = 0.0
                            binding.textEstimacionMgp.isVisible = false
                            recalcularTotal()
                        }
                    }
                }
            }
        }

        binding.btnAsignar.setOnClickListener {
            binding.textError.isVisible = false
            binding.layoutGasolinera.error = null
            binding.layoutHorasVigencia.error = null
            binding.layoutMontoVehiculo.error = null
            binding.layoutMontoMgp.error = null

            val gasolineraId = gasolineraIdSeleccionada ?: run {
                val txt = binding.spinnerGasolinera.text?.toString()?.trim().orEmpty()
                listaGasolineras.find { g -> txt.contains(g.nombre, ignoreCase = true) }?.id
            }

            if (gasolineraId == null) {
                binding.layoutGasolinera.error = "Selecciona una gasolinera."
                return@setOnClickListener
            }

            val resHoras = Validadores.horasVigencia(binding.inputHorasVigencia.text?.toString())
            if (!resHoras.esOk) {
                binding.layoutHorasVigencia.error = resHoras.mensajeError()
                return@setOnClickListener
            }
            val horasVal = resHoras.valorOrNull()!!

            var hasError = false
            val detalles = mutableListOf<DetalleLineaRequest>()

            if (esVehiculo) {
                val targetVehiculoId = vehiculoIdSolicitud ?: listaVehiculos.firstOrNull()?.id
                if (!Validadores.idValido(targetVehiculoId)) {
                    binding.textError.isVisible = true
                    binding.textError.text = "No se pudo identificar el vehículo de la solicitud. Vuelve a abrirla."
                    return@setOnClickListener
                }

                val resMontoV = Validadores.monto(binding.inputMontoVehiculo.text?.toString(), "monto del vehículo")
                if (!resMontoV.esOk) {
                    binding.layoutMontoVehiculo.error = resMontoV.mensajeError()
                    hasError = true
                } else {
                    detalles += DetalleLineaRequest("vehiculo", vehiculo_id = targetVehiculoId, monto = resMontoV.valorOrNull()!!)
                }
            }

            if (esMgp) {
                val resMontoM = Validadores.monto(binding.inputMontoMgp.text?.toString(), "monto de MGP")
                if (!resMontoM.esOk) {
                    binding.layoutMontoMgp.error = resMontoM.mensajeError()
                    hasError = true
                } else {
                    detalles += DetalleLineaRequest("mgp", descripcion_mgp = descripcionMgp, monto = resMontoM.valorOrNull()!!)
                }
            }

            if (hasError) return@setOnClickListener

            if (detalles.isEmpty()) {
                binding.textError.isVisible = true
                binding.textError.text = getString(R.string.ingresa_al_menos_un_monto)
                return@setOnClickListener
            }

            mostrarDialogoConfirmarContrasena(solicitudId, gasolineraId, horasVal, detalles)
        }
    }

    private fun mostrarDialogoConfirmarContrasena(
        solicitudId: Int,
        gasolineraId: Int,
        horas: Int,
        detalles: List<DetalleLineaRequest>
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmar_contrasena, null)
        val inputContrasena = dialogView.findViewById<TextInputEditText>(R.id.inputContrasenaAdmin)
        val layoutContrasena = dialogView.findViewById<TextInputLayout>(R.id.layoutContrasenaAdmin)
        val progress = dialogView.findViewById<ProgressBar>(R.id.progressVerificacion)
        val textError = dialogView.findViewById<TextView>(R.id.textErrorVerificacion)

        inputContrasena.filters = FiltrosEntrada.limiteYLargo(128)
        inputContrasena.addTextChangedListener { layoutContrasena.error = null }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_confirmar_contrasena_titulo)
            .setView(dialogView)
            .setPositiveButton(R.string.aceptar, null)
            .setNegativeButton(R.string.cancelar) { d, _ ->
                model.limpiarVerificacion()
                d.dismiss()
            }
            .setOnDismissListener {
                model.limpiarVerificacion()
            }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val contrasena = inputContrasena.text?.toString()?.trim().orEmpty()
            val valPass = Validadores.contrasenaLogin(contrasena)
            if (!valPass.esOk) {
                layoutContrasena.error = valPass.mensajeError()
                return@setOnClickListener
            }
            layoutContrasena.error = null
            textError.isVisible = false
            progress.isVisible = true
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
            inputContrasena.isEnabled = false

            model.verificarContrasena(valPass.valorOrNull()!!) {
                dialog.dismiss()
                model.asignarOrden(solicitudId, gasolineraId, horas, detalles)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.verificacion.collect { estado ->
                    if (!dialog.isShowing) return@collect
                    progress.isVisible = estado.loading
                    val buttonsEnabled = !estado.loading
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = buttonsEnabled
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = buttonsEnabled
                    inputContrasena.isEnabled = buttonsEnabled

                    if (estado.error != null) {
                        layoutContrasena.error = estado.error
                        inputContrasena.setText("")
                    }
                }
            }
        }
    }

    private fun capitalizarCombustible(combustible: String?): String {
        return when (combustible?.lowercase()?.trim()) {
            "diesel", "diésel" -> "Diésel"
            "regular" -> "Regular"
            "super", "súper" -> "Súper"
            else -> combustible?.replaceFirstChar { it.uppercase() } ?: "Combustible"
        }
    }

    private fun recalcularTotal() {
        val montoVehiculo = if (binding.tarjetaMontoVehiculo.isVisible)
            binding.inputMontoVehiculo.text?.toString()?.toDoubleOrNull() ?: 0.0 else 0.0
        val montoMgp = if (binding.tarjetaMontoMgp.isVisible)
            binding.inputMontoMgp.text?.toString()?.toDoubleOrNull() ?: 0.0 else 0.0

        val targetVehiculoId = vehiculoIdSolicitud ?: listaVehiculos.firstOrNull()?.id
        if (binding.tarjetaMontoVehiculo.isVisible && targetVehiculoId != null) {
            model.estimarVehiculo(montoVehiculo, targetVehiculoId)
        } else {
            galonesVehiculo = 0.0
            binding.textEstimacionVehiculo.isVisible = false
        }

        if (binding.tarjetaMontoMgp.isVisible) {
            model.estimarMgp(montoMgp)
        } else {
            galonesMgp = 0.0
            binding.textEstimacionMgp.isVisible = false
        }

        val totalMonto = montoVehiculo + montoMgp
        val totalGalones = galonesVehiculo + galonesMgp
        val totalMontoStr = getString(R.string.total_de_la_solicitud, totalMonto)
        val textoTotal = if (totalGalones > 0) {
            "$totalMontoStr\n${getString(R.string.estimacion_total_galones, totalGalones)}"
        } else {
            totalMontoStr
        }
        binding.textTotalAsignado.text = textoTotal
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

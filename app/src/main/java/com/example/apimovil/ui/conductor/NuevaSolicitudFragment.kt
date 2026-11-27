package com.example.apimovil.ui.conductor

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.apimovil.R
import com.example.apimovil.data.models.Vehiculo
import com.example.apimovil.databinding.FragmentNuevaSolicitudBinding
import com.example.apimovil.ui.common.FiltrosEntrada
import com.example.apimovil.ui.common.Validadores
import kotlinx.coroutines.launch

/**
 * El conductor describe QUÉ necesita (vehículo, planta MGP, o ambos) y PARA QUÉ.
 * Permite crear una nueva solicitud o editar una existente si se pasa solicitudId.
 */
class NuevaSolicitudFragment : Fragment(R.layout.fragment_nueva_solicitud) {

    private val model: ConductorViewModel by viewModels()
    private var _binding: FragmentNuevaSolicitudBinding? = null
    private val binding get() = _binding!!

    private var listaVehiculos: List<Vehiculo> = emptyList()
    private var vehiculoSeleccionadoId: Int? = null
    private var solicitudId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentNuevaSolicitudBinding.bind(view)
        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }

        solicitudId = arguments?.getInt("solicitudId") ?: -1
        val esEdicion = solicitudId != -1

        if (esEdicion && !Validadores.idValido(solicitudId)) {
            Toast.makeText(requireContext(), "No se pudo abrir esta pantalla. Vuelve a intentarlo.", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        binding.inputDescripcionMgp.filters = FiltrosEntrada.limiteYLargo(150)
        binding.inputActividad.filters = FiltrosEntrada.limiteYLargo(1000)

        binding.inputActividad.addTextChangedListener {
            binding.layoutActividad.error = null
            binding.textError.isVisible = false
        }
        binding.inputDescripcionMgp.addTextChangedListener {
            binding.layoutDescripcionMgp.error = null
            binding.textError.isVisible = false
        }

        if (esEdicion) {
            binding.textTitulo.text = getString(R.string.editar_solicitud)
            binding.btnEnviar.text = getString(R.string.guardar_cambios)
            model.cargarSolicitudPorId(solicitudId)
        }

        binding.toggleDestino.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val codigo = when (checkedId) {
                    R.id.btnDestinoMgp -> "mgp"
                    R.id.btnDestinoAmbos -> "ambos"
                    else -> "vehiculo"
                }
                actualizarCamposVisibles(codigo)
            }
        }
        if (!esEdicion) {
            binding.toggleDestino.check(R.id.btnDestinoVehiculo)
            actualizarCamposVisibles("vehiculo")
        }

        binding.spinnerVehiculo.setOnItemClickListener { _, _, position, _ ->
            if (position in listaVehiculos.indices) {
                vehiculoSeleccionadoId = listaVehiculos[position].id
                binding.layoutVehiculo.error = null
            }
        }

        binding.btnEnviar.setOnClickListener { enviarSolicitud() }

        model.cargarVehiculos()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.vehiculos.collect { estado ->
                        val vehiculos = estado.data.orEmpty()
                        listaVehiculos = vehiculos
                        if (vehiculos.isNotEmpty()) {
                            val nombresVehiculos = vehiculos.map { v ->
                                val partes = listOfNotNull(
                                    v.placa.takeIf { it.isNotBlank() },
                                    v.marca.takeIf { it.isNotBlank() },
                                    v.modelo.takeIf { it.isNotBlank() },
                                )
                                if (partes.isNotEmpty()) partes.joinToString(" - ") else "Vehículo #${v.id}"
                            }
                            val adapterVehiculos = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, nombresVehiculos)
                            binding.spinnerVehiculo.setAdapter(adapterVehiculos)

                            if (!esEdicion && (vehiculoSeleccionadoId == null || binding.spinnerVehiculo.text.isNullOrEmpty())) {
                                val primerVehiculo = vehiculos.first()
                                vehiculoSeleccionadoId = primerVehiculo.id
                                binding.spinnerVehiculo.setText(nombresVehiculos.first(), false)
                            } else if (esEdicion && vehiculoSeleccionadoId != null) {
                                val veh = vehiculos.find { it.id == vehiculoSeleccionadoId }
                                if (veh != null) {
                                    val idx = vehiculos.indexOf(veh)
                                    if (idx in nombresVehiculos.indices) {
                                        binding.spinnerVehiculo.setText(nombresVehiculos[idx], false)
                                    }
                                }
                            }
                        } else {
                            binding.spinnerVehiculo.setText("", false)
                            vehiculoSeleccionadoId = null
                        }
                    }
                }

                if (esEdicion) {
                    launch {
                        model.solicitudDetalle.collect { estado ->
                            binding.progress.isVisible = estado.loading
                            estado.data?.let { sol ->
                                when (sol.tipo_destino) {
                                    "vehiculo" -> binding.toggleDestino.check(R.id.btnDestinoVehiculo)
                                    "mgp" -> binding.toggleDestino.check(R.id.btnDestinoMgp)
                                    "ambos" -> binding.toggleDestino.check(R.id.btnDestinoAmbos)
                                    else -> binding.toggleDestino.check(R.id.btnDestinoVehiculo)
                                }
                                actualizarCamposVisibles(sol.tipo_destino)

                                if (sol.vehiculo_id != null) {
                                    vehiculoSeleccionadoId = sol.vehiculo_id
                                    val veh = listaVehiculos.find { it.id == sol.vehiculo_id }
                                    if (veh != null) {
                                        val nombresVehiculos = listaVehiculos.map { v ->
                                            listOfNotNull(v.placa.takeIf { it.isNotBlank() }, v.marca, v.modelo).joinToString(" - ")
                                        }
                                        val idx = listaVehiculos.indexOf(veh)
                                        if (idx in nombresVehiculos.indices) {
                                            binding.spinnerVehiculo.setText(nombresVehiculos[idx], false)
                                        }
                                    }
                                }

                                binding.inputDescripcionMgp.setText(sol.descripcion_mgp.orEmpty())
                                binding.inputActividad.setText(sol.descripcion_actividad)
                            }
                            if (estado.error != null) {
                                binding.textError.isVisible = true
                                binding.textError.text = estado.error
                            }
                        }
                    }

                    launch {
                        model.edicionSolicitud.collect { estado ->
                            binding.progress.isVisible = estado.loading
                            binding.btnEnviar.isEnabled = !estado.loading
                            binding.textError.isVisible = estado.error != null
                            binding.textError.text = estado.error

                            if (estado.camposError.isNotEmpty()) {
                                if (estado.camposError.containsKey("descripcion_actividad")) {
                                    binding.layoutActividad.error = estado.camposError["descripcion_actividad"]
                                }
                                if (estado.camposError.containsKey("descripcion_mgp")) {
                                    binding.layoutDescripcionMgp.error = estado.camposError["descripcion_mgp"]
                                }
                                if (estado.camposError.containsKey("vehiculo_id")) {
                                    binding.layoutVehiculo.error = estado.camposError["vehiculo_id"]
                                }
                            }

                            if (estado.data == true) {
                                Toast.makeText(requireContext(), R.string.solicitud_actualizada, Toast.LENGTH_SHORT).show()
                                findNavController().navigateUp()
                            }
                        }
                    }
                } else {
                    launch {
                        model.solicitud.collect { estado ->
                            binding.progress.isVisible = estado.loading
                            binding.btnEnviar.isEnabled = !estado.loading
                            binding.textError.isVisible = estado.error != null
                            binding.textError.text = estado.error

                            if (estado.camposError.isNotEmpty()) {
                                if (estado.camposError.containsKey("descripcion_actividad")) {
                                    binding.layoutActividad.error = estado.camposError["descripcion_actividad"]
                                }
                                if (estado.camposError.containsKey("descripcion_mgp")) {
                                    binding.layoutDescripcionMgp.error = estado.camposError["descripcion_mgp"]
                                }
                                if (estado.camposError.containsKey("vehiculo_id")) {
                                    binding.layoutVehiculo.error = estado.camposError["vehiculo_id"]
                                }
                            }

                            if (estado.data == true) {
                                findNavController().navigateUp()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun actualizarCamposVisibles(codigoDestino: String) {
        val necesitaVehiculo = codigoDestino == "vehiculo" || codigoDestino == "ambos"
        val necesitaMgp = codigoDestino == "mgp" || codigoDestino == "ambos"

        binding.layoutVehiculo.isVisible = necesitaVehiculo
        binding.layoutDescripcionMgp.isVisible = necesitaMgp
    }

    private fun enviarSolicitud() {
        binding.layoutActividad.error = null
        binding.layoutDescripcionMgp.error = null
        binding.layoutVehiculo.error = null
        binding.textError.isVisible = false

        val codigoDestino = when (binding.toggleDestino.checkedButtonId) {
            R.id.btnDestinoMgp -> "mgp"
            R.id.btnDestinoAmbos -> "ambos"
            else -> "vehiculo"
        }

        val necesitaVehiculo = codigoDestino == "vehiculo" || codigoDestino == "ambos"
        val necesitaMgp = codigoDestino == "mgp" || codigoDestino == "ambos"

        val vehiculoId = if (necesitaVehiculo) {
            vehiculoSeleccionadoId ?: run {
                val txt = binding.spinnerVehiculo.text?.toString()?.trim().orEmpty()
                listaVehiculos.find { v -> txt.contains(v.placa, ignoreCase = true) }?.id
            }
        } else null

        var hasError = false

        if (necesitaVehiculo && !Validadores.idValido(vehiculoId)) {
            binding.layoutVehiculo.error = getString(R.string.elige_un_destino_valido)
            hasError = true
        }

        val actividadInput = binding.inputActividad.text?.toString()
        val resActividad = Validadores.textoLibre(actividadInput, "la actividad", 5, 1000, esObligatorio = true)
        if (!resActividad.esOk) {
            binding.layoutActividad.error = resActividad.mensajeError()
            hasError = true
        }

        var descripcionMgpLimpia: String? = null
        if (necesitaMgp) {
            val mgpInput = binding.inputDescripcionMgp.text?.toString()
            val resMgp = Validadores.textoLibre(mgpInput, "la descripción del equipo MGP", 3, 150, esObligatorio = true)
            if (!resMgp.esOk) {
                binding.layoutDescripcionMgp.error = resMgp.mensajeError()
                hasError = true
            } else {
                descripcionMgpLimpia = resMgp.valorOrNull()
            }
        }

        if (hasError) return

        if (solicitudId != -1) {
            model.editarSolicitud(
                id = solicitudId,
                tipoDestino = codigoDestino,
                vehiculoId = vehiculoId,
                descripcionMgp = descripcionMgpLimpia,
                descripcionActividad = resActividad.valorOrNull()!!
            )
        } else {
            model.crearSolicitud(
                tipoDestino = codigoDestino,
                vehiculoId = vehiculoId,
                descripcionMgp = descripcionMgpLimpia,
                descripcionActividad = resActividad.valorOrNull()!!
            )
        }
    }

    override fun onDestroyView() {
        model.limpiarEdicionSolicitud()
        _binding = null
        super.onDestroyView()
    }
}

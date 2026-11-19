package com.example.apimovil.ui.conductor

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.apimovil.R
import com.example.apimovil.data.models.Vehiculo
import com.example.apimovil.databinding.FragmentNuevaSolicitudBinding
import kotlinx.coroutines.launch

/**
 * El conductor describe QUÉ necesita (vehículo, planta MGP, o ambos) y PARA QUÉ.
 * Permite crear una nueva solicitud o editar una existente si se pasa solicitudId.
 */
class NuevaSolicitudFragment : Fragment(R.layout.fragment_nueva_solicitud) {

    private val model: ConductorViewModel by viewModels()
    private var _binding: FragmentNuevaSolicitudBinding? = null
    private val binding get() = _binding!!

    private val opcionesMap = LinkedHashMap<String, String>().apply {
        put("Vehículo", "vehiculo")
        put("Planta generadora (MGP)", "mgp")
        put("Ambos (Vehículo + MGP)", "ambos")
    }
    private val opcionesDestino = opcionesMap.keys.toList()

    private var listaVehiculos: List<Vehiculo> = emptyList()
    private var vehiculoSeleccionadoId: Int? = null
    private var solicitudId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentNuevaSolicitudBinding.bind(view)
        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }

        solicitudId = arguments?.getInt("solicitudId") ?: -1
        val esEdicion = solicitudId != -1

        if (esEdicion) {
            binding.textTitulo.text = getString(R.string.editar_solicitud)
            binding.btnEnviar.text = getString(R.string.guardar_cambios)
            model.cargarSolicitudPorId(solicitudId)
        }

        // Configurar dropdown de destino
        val adapterDestino = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, opcionesDestino)
        binding.spinnerDestino.setAdapter(adapterDestino)
        binding.spinnerDestino.setText(opcionesDestino.first(), false)
        actualizarCamposVisibles(opcionesMap[opcionesDestino.first()] ?: "vehiculo")

        binding.spinnerDestino.setOnItemClickListener { _, _, position, _ ->
            val opcion = opcionesDestino.getOrNull(position) ?: opcionesDestino.first()
            val codigo = opcionesMap[opcion] ?: "vehiculo"
            actualizarCamposVisibles(codigo)
        }

        binding.spinnerVehiculo.setOnItemClickListener { _, _, position, _ ->
            if (position in listaVehiculos.indices) {
                vehiculoSeleccionadoId = listaVehiculos[position].id
            }
        }

        binding.btnEnviar.setOnClickListener { enviarSolicitud() }

        // Cargar vehículos (placas)
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
                                val labelDestino = when (sol.tipo_destino) {
                                    "vehiculo" -> "Vehículo"
                                    "mgp" -> "Planta generadora (MGP)"
                                    "ambos" -> "Ambos (Vehículo + MGP)"
                                    else -> "Vehículo"
                                }
                                binding.spinnerDestino.setText(labelDestino, false)
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

                            if (estado.error != null && estado.error.contains("422", ignoreCase = true)) {
                                findNavController().navigateUp()
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
        val textoDestino = binding.spinnerDestino.text?.toString()?.trim().orEmpty()
        val codigoDestino = opcionesMap[textoDestino] ?: when {
            textoDestino.contains("vehiculo", ignoreCase = true) && textoDestino.contains("mgp", ignoreCase = true) -> "ambos"
            textoDestino.contains("ambos", ignoreCase = true) -> "ambos"
            textoDestino.contains("mgp", ignoreCase = true) -> "mgp"
            else -> "vehiculo"
        }

        val necesitaVehiculo = codigoDestino == "vehiculo" || codigoDestino == "ambos"
        val vehiculoId = if (necesitaVehiculo) {
            vehiculoSeleccionadoId ?: run {
                val txt = binding.spinnerVehiculo.text?.toString()?.trim().orEmpty()
                listaVehiculos.find { v -> txt.contains(v.placa, ignoreCase = true) }?.id
            }
        } else null

        if (necesitaVehiculo && vehiculoId == null) {
            binding.textError.isVisible = true
            binding.textError.text = getString(R.string.elige_un_destino_valido)
            return
        }

        val descripcionMgp = binding.inputDescripcionMgp.text?.toString()?.trim()
        val actividad = binding.inputActividad.text?.toString()?.trim().orEmpty()

        if (solicitudId != -1) {
            model.editarSolicitud(
                id = solicitudId,
                tipoDestino = codigoDestino,
                vehiculoId = vehiculoId,
                descripcionMgp = descripcionMgp,
                descripcionActividad = actividad
            )
        } else {
            model.crearSolicitud(
                tipoDestino = codigoDestino,
                vehiculoId = vehiculoId,
                descripcionMgp = descripcionMgp,
                descripcionActividad = actividad
            )
        }
    }

    override fun onDestroyView() {
        model.limpiarEdicionSolicitud()
        _binding = null
        super.onDestroyView()
    }
}

package com.example.apimovil.ui.admin

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apimovil.R
import com.example.apimovil.databinding.FragmentNotificacionesBinding
import com.example.apimovil.ui.common.formatoLegible
import kotlinx.coroutines.launch

/**
 * El backend notifica por CORREO (RF-21), pero no tiene un endpoint de
 * notificaciones dentro de la app. Esta pantalla arma una vista equivalente
 * a partir de las órdenes completadas y vencidas más recientes, que es la
 * misma información que ya le llega al administrador por correo.
 */
class NotificacionesFragment : Fragment(R.layout.fragment_notificaciones) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentNotificacionesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentNotificacionesBinding.bind(view)
        binding.recyclerNotificaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.swipeRefresh.setOnRefreshListener { model.cargarOrdenes() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ordenes.collect { estado ->
                    binding.swipeRefresh.isRefreshing = estado.loading

                    val relevantes = estado.data.orEmpty()
                        .filter { it.estado == "completada" || it.estado == "vencida" }
                        .sortedByDescending { it.fecha_completada ?: it.vence_apertura ?: it.fecha_asignacion }
                        .take(30)

                    val items = relevantes.map { orden ->
                        if (orden.estado == "completada") {
                            NotificacionItem(
                                ordenId = orden.id,
                                titulo = getString(R.string.completada_badge) + " · ${orden.numero_orden}",
                                cuerpo = getString(
                                    R.string.orden_completada_notif,
                                    orden.conductor?.usuario?.name ?: "",
                                    orden.gasolinera?.nombre ?: ""
                                ),
                                hora = orden.fecha_completada?.let { formatoLegible(it) } ?: ""
                            )
                        } else {
                            NotificacionItem(
                                ordenId = orden.id,
                                titulo = getString(R.string.vencida_badge) + " · ${orden.numero_orden}",
                                cuerpo = getString(
                                    R.string.orden_vencida_notif,
                                    orden.conductor?.usuario?.name ?: "",
                                    orden.numero_orden
                                ),
                                hora = orden.vence_apertura?.let { formatoLegible(it) } ?: ""
                            )
                        }
                    }

                    binding.textVacio.isVisible = !estado.loading && items.isEmpty()
                    binding.recyclerNotificaciones.adapter = NotificacionAdapter(items) { ordenId ->
                        val bundle = Bundle().apply { putInt("ordenId", ordenId) }
                        findNavController().navigate(R.id.ordenDetalleAdminFragment, bundle)
                    }
                }
            }
        }

        model.cargarOrdenes()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

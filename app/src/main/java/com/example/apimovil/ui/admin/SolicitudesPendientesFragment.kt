package com.example.apimovil.ui.admin

import android.os.Bundle
import android.view.View
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
import com.example.apimovil.databinding.FragmentSolicitudesPendientesBinding
import com.example.apimovil.ui.common.SolicitudAdapter
import com.example.apimovil.ui.common.configurarNavegacionAdmin
import com.example.apimovil.ui.common.configurarNavegacionSupervisor
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SolicitudesPendientesFragment : Fragment(R.layout.fragment_solicitudes_pendientes) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentSolicitudesPendientesBinding? = null
    private val binding get() = _binding!!

    private val adapter = SolicitudAdapter(
        mostrarAccionAsignar = SessionManager.esAdmin,
        onRechazar = { solicitud ->
            if (SessionManager.esAdmin) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.rechazar)
                    .setMessage(R.string.confirmar_rechazo_solicitud)
                    .setPositiveButton(R.string.aceptar) { _, _ ->
                        model.rechazarSolicitud(solicitud.id)
                    }
                    .setNegativeButton(R.string.cancelar, null)
                    .show()
            }
        }
    ) { solicitud ->
        // El supervisor (contabilidad) puede ver la lista pero no asignar monto;
        // el backend de todas formas rechazaría el POST /ordenes si lo intentara.
        if (SessionManager.esAdmin) {
            val bundle = Bundle().apply {
                putInt("solicitudId", solicitud.id)
                putInt("conductorId", solicitud.conductor_id)
                putString("conductorNombre", solicitud.conductor?.usuario?.name)
                putString("tipoDestino", solicitud.tipo_destino)
                putString("descripcionActividad", solicitud.descripcion_actividad)
                solicitud.vehiculo_id?.let { putInt("vehiculoId", it) }
                solicitud.descripcion_mgp?.let { putString("descripcionMgp", it) }
            }
            findNavController().navigate(R.id.asignarMontoFragment, bundle)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSolicitudesPendientesBinding.bind(view)
        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.textAyuda.isVisible = !SessionManager.esAdmin
        binding.swipeRefresh.setOnRefreshListener { model.cargarSolicitudesPendientes() }

        val bottomNav = binding.incluirNavInferior.root as BottomNavigationView
        if (SessionManager.esAdmin) {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_admin)
            bottomNav.configurarNavegacionAdmin(findNavController(), R.id.solicitudesPendientesFragment)
        } else {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_supervisor)
            bottomNav.configurarNavegacionSupervisor(findNavController(), R.id.solicitudesPendientesFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.solicitudesPendientes.collect { estado ->
                        binding.swipeRefresh.isRefreshing = estado.loading
                        binding.textVacio.isVisible = !estado.loading && estado.error == null && estado.data.isNullOrEmpty()
                        binding.textError.isVisible = estado.error != null
                        binding.textError.text = estado.error
                        adapter.submitList(estado.data.orEmpty())
                    }
                }

                launch {
                    model.rechazoSolicitud.collect { estado ->
                        if (estado.data == true) {
                            Toast.makeText(requireContext(), R.string.solicitud_rechazada, Toast.LENGTH_SHORT).show()
                            model.cargarSolicitudesPendientes()
                        }
                        if (estado.error != null) {
                            Toast.makeText(requireContext(), estado.error, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        model.cargarSolicitudesPendientes()
    }

    override fun onResume() {
        super.onResume()
        model.cargarSolicitudesPendientes()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

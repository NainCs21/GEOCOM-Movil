package com.example.apimovil.ui.admin

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.notificaciones.NotificacionesRepository
import com.example.apimovil.data.notificaciones.NotificacionesStore
import com.example.apimovil.data.notificaciones.Notificador
import com.example.apimovil.databinding.FragmentNotificacionesBinding
import kotlinx.coroutines.launch

class NotificacionesFragment : Fragment(R.layout.fragment_notificaciones) {

    private var _binding: FragmentNotificacionesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotificacionListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentNotificacionesBinding.bind(view)

        if (SessionManager.haySesion) {
            NotificacionesStore.cargarPara(SessionManager.usuarioId)
        }

        adapter = NotificacionListAdapter { item ->
            NotificacionesStore.marcarLeida(item.id)
            when (item.destino) {
                "orden" -> {
                    if (item.ordenId != null) {
                        val bundle = Bundle().apply { putInt("ordenId", item.ordenId) }
                        findNavController().navigate(R.id.ordenDetalleAdminFragment, bundle)
                    }
                }
                "solicitudes" -> {
                    findNavController().navigate(R.id.solicitudesPendientesFragment)
                }
            }
        }

        binding.recyclerNotificaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotificaciones.adapter = adapter

        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.btnMarcarTodasLeidas.setOnClickListener {
            NotificacionesStore.marcarTodasLeidas()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                binding.swipeRefresh.isRefreshing = true
                NotificacionesRepository.sincronizar(requireContext())
                binding.swipeRefresh.isRefreshing = false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotificacionesStore.notificaciones.collect { lista ->
                    adapter.submitList(lista)
                    binding.textVacio.isVisible = lista.isEmpty()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            NotificacionesRepository.sincronizar(requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        Notificador.isNotificacionesScreenActive = true
    }

    override fun onPause() {
        Notificador.isNotificacionesScreenActive = false
        super.onPause()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

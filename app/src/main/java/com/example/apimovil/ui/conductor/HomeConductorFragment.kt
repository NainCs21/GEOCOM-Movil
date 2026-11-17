package com.example.apimovil.ui.conductor

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
import com.example.apimovil.data.SessionManager
import com.example.apimovil.databinding.FragmentHomeConductorBinding
import com.example.apimovil.ui.common.OrdenAdapter
import kotlinx.coroutines.launch

/**
 * "Mis solicitudes": el conductor toca cualquier orden (sin importar su
 * estado) para ver su detalle. Si está "pendiente", el detalle es quien
 * ofrece el botón para escanear el QR; aquí ya no hay un botón de escaneo
 * suelto, siguiendo el flujo real del Figma.
 */
class HomeConductorFragment : Fragment(R.layout.fragment_home_conductor) {

    private val model: ConductorViewModel by viewModels()
    private var _binding: FragmentHomeConductorBinding? = null
    private val binding get() = _binding!!
    private val adapter = OrdenAdapter { orden ->
        val bundle = Bundle().apply { putInt("ordenId", orden.id) }
        findNavController().navigate(R.id.ordenDetalleFragment, bundle)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeConductorBinding.bind(view)
        binding.textSaludo.text = getString(R.string.hola_usuario, SessionManager.nombre ?: "")

        binding.recyclerOrdenes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerOrdenes.adapter = adapter

        binding.btnNuevaSolicitud.setOnClickListener {
            findNavController().navigate(R.id.nuevaSolicitudFragment)
        }
        binding.btnHistorial.setOnClickListener {
            findNavController().navigate(R.id.historialFragment)
        }
        binding.btnPerfil.setOnClickListener {
            findNavController().navigate(R.id.perfilConductorFragment)
        }
        binding.swipeRefresh.setOnRefreshListener { model.cargarMisOrdenes() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ordenes.collect { estado ->
                    binding.swipeRefresh.isRefreshing = estado.loading
                    val ordenesActivas = estado.data.orEmpty().filter { it.estado.lowercase() != "completada" }
                    binding.textVacio.isVisible = !estado.loading && ordenesActivas.isEmpty()
                    binding.textError.isVisible = estado.error != null
                    binding.textError.text = estado.error
                    adapter.submitList(ordenesActivas)
                }
            }
        }

        model.cargarMisOrdenes()
    }

    override fun onResume() {
        super.onResume()
        model.cargarMisOrdenes()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

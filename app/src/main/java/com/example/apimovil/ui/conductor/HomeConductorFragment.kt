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
import android.widget.Toast
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.databinding.FragmentHomeConductorBinding
import com.example.apimovil.ui.common.OrdenAdapter
import com.example.apimovil.ui.common.configurarNavegacionConductor
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

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

        val bottomNav = binding.root.findViewById<BottomNavigationView>(R.id.incluirNavInferior)
            ?: binding.root.findViewById<BottomNavigationView>(R.id.bottomNavConductor)
        bottomNav?.configurarNavegacionConductor(findNavController(), R.id.homeConductorFragment) {
            val ordenPendiente = model.ordenes.value.data.orEmpty().find { it.estado == "pendiente" }
            if (ordenPendiente != null) {
                findNavController().navigate(R.id.escanearQrFragment)
            } else {
                Toast.makeText(requireContext(), "No tienes órdenes pendientes de escanear", Toast.LENGTH_SHORT).show()
            }
        }

        binding.recyclerOrdenes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerOrdenes.adapter = adapter

        binding.btnNuevaSolicitud.setOnClickListener {
            findNavController().navigate(R.id.nuevaSolicitudFragment)
        }
        binding.swipeRefresh.setOnRefreshListener { model.cargarMisOrdenes() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ordenes.collect { estado ->
                    binding.swipeRefresh.isRefreshing = estado.loading
                    val ordenesActivas = estado.data.orEmpty().filter {
                        it.estado == "pendiente" || it.estado == "abierta" || it.estado == "abierta_tarde"
                    }

                    val ordenPendiente = ordenesActivas.find { it.estado == "pendiente" }
                    binding.cardBannerPendiente.isVisible = ordenPendiente != null
                    binding.btnEscanearBanner.setOnClickListener {
                        findNavController().navigate(R.id.escanearQrFragment)
                    }

                    binding.cardEstadoVacio.isVisible = !estado.loading && estado.error == null && ordenesActivas.isEmpty()
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

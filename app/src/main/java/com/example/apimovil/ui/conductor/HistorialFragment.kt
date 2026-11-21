package com.example.apimovil.ui.conductor

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import com.example.apimovil.R
import com.example.apimovil.data.models.Orden
import com.example.apimovil.databinding.FragmentHistorialBinding
import com.example.apimovil.ui.common.OrdenAdapter
import com.example.apimovil.ui.common.configurarNavegacionConductor
import kotlinx.coroutines.launch

class HistorialFragment : Fragment(R.layout.fragment_historial) {

    private val model: ConductorViewModel by viewModels()
    private var _binding: FragmentHistorialBinding? = null
    private val binding get() = _binding!!
    private val adapter = OrdenAdapter { orden ->
        val bundle = Bundle().apply { putInt("ordenId", orden.id) }
        findNavController().navigate(R.id.ordenDetalleFragment, bundle)
    }
    private var todasLasOrdenes: List<Orden> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHistorialBinding.bind(view)
        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }

        val bottomNav = binding.root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.incluirNavInferior)
            ?: binding.root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavConductor)
        bottomNav?.configurarNavegacionConductor(findNavController(), R.id.historialFragment) {
            val ordenPendiente = model.ordenes.value.data.orEmpty().find { it.estado == "pendiente" }
            if (ordenPendiente != null) {
                findNavController().navigate(R.id.escanearQrFragment)
            } else {
                Toast.makeText(requireContext(), "No tienes órdenes pendientes de escanear", Toast.LENGTH_SHORT).show()
            }
        }
        binding.recyclerHistorial.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistorial.adapter = adapter

        binding.toggleFiltro.check(R.id.btnTodas)
        binding.btnTodas.setOnClickListener { aplicarFiltro() }
        binding.btnCompletadas.setOnClickListener { aplicarFiltro() }
        binding.btnVencidas.setOnClickListener { aplicarFiltro() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ordenes.collect { estado ->
                    todasLasOrdenes = estado.data.orEmpty()
                    aplicarFiltro()
                }
            }
        }

        model.cargarMisOrdenes()
    }

    private fun aplicarFiltro() {
        val filtradas = when (binding.toggleFiltro.checkedButtonId) {
            R.id.btnCompletadas -> todasLasOrdenes.filter { it.estado == "completada" || it.estado == "completada_tarde" }
            R.id.btnVencidas -> todasLasOrdenes.filter { it.estado == "vencida" }
            else -> todasLasOrdenes
        }
        adapter.submitList(filtradas)
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

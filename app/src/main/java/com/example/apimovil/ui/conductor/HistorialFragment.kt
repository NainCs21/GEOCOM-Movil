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
import com.example.apimovil.R
import com.example.apimovil.data.models.Orden
import com.example.apimovil.databinding.FragmentHistorialBinding
import com.example.apimovil.ui.common.OrdenAdapter
import com.example.apimovil.ui.common.esCompletada
import kotlinx.coroutines.launch

/**
 * El historial del conductor incluye órdenes donde esCompletada(estado).
 * Permite filtrar entre todas las completadas o solo las completadas tarde.
 */
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
        binding.recyclerHistorial.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistorial.adapter = adapter

        binding.toggleFiltro.check(R.id.btnTodas)
        binding.btnTodas.setOnClickListener { aplicarFiltro() }
        binding.btnCompletadas.setOnClickListener { aplicarFiltro() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ordenes.collect { estado ->
                    todasLasOrdenes = estado.data.orEmpty().filter { esCompletada(it.estado) }
                    aplicarFiltro()
                }
            }
        }

        model.cargarMisOrdenes()
    }

    private fun aplicarFiltro() {
        val filtradas = if (binding.toggleFiltro.checkedButtonId == R.id.btnCompletadas) {
            todasLasOrdenes.filter { it.estado == "completada_tarde" }
        } else {
            todasLasOrdenes
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

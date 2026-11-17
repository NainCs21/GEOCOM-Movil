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
import com.example.apimovil.databinding.FragmentReportesBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale
import kotlinx.coroutines.launch

class ReportesFragment : Fragment(R.layout.fragment_reportes) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentReportesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentReportesBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())

        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.toggleReporte.check(R.id.btnPorConductor)
        model.cargarReportePorConductor()

        binding.btnPorConductor.setOnClickListener { model.cargarReportePorConductor() }
        binding.btnPorGasolinera.setOnClickListener { model.cargarReportePorGasolinera() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.reporteConductor.collect { estado ->
                    if (binding.toggleReporte.checkedButtonId == R.id.btnPorConductor) {
                        binding.progress.isVisible = estado.loading
                        val items = estado.data.orEmpty().map {
                            FilaDoble(
                                id = it.conductor_id,
                                titulo = it.nombre_conductor,
                                subtitulo = getString(R.string.ordenes_y_monto, it.cantidad_ordenes, it.monto_total)
                            )
                        }
                        binding.recycler.adapter = FilaDobleAdapter(items) { id, _ ->
                            val rep = estado.data?.find { it.conductor_id == id }
                            if (rep != null) {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(rep.nombre_conductor)
                                    .setMessage("ID Conductor: ${rep.conductor_id}\nÓrdenes totales: ${rep.cantidad_ordenes}\nMonto total: L ${String.format(Locale.US, "%.2f", rep.monto_total)}")
                                    .setPositiveButton(R.string.aceptar, null)
                                    .show()
                            }
                        }
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.reporteGasolinera.collect { estado ->
                    if (binding.toggleReporte.checkedButtonId == R.id.btnPorGasolinera) {
                        binding.progress.isVisible = estado.loading
                        val items = estado.data.orEmpty().map {
                            FilaDoble(
                                id = it.gasolinera_id,
                                titulo = it.nombre_gasolinera,
                                subtitulo = getString(R.string.ordenes_y_monto, it.cantidad_ordenes, it.monto_total)
                            )
                        }
                        binding.recycler.adapter = FilaDobleAdapter(items) { id, _ ->
                            val rep = estado.data?.find { it.gasolinera_id == id }
                            if (rep != null) {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(rep.nombre_gasolinera)
                                    .setMessage("ID Gasolinera: ${rep.gasolinera_id}\nÓrdenes totales: ${rep.cantidad_ordenes}\nMonto total: L ${String.format(Locale.US, "%.2f", rep.monto_total)}")
                                    .setPositiveButton(R.string.aceptar, null)
                                    .show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

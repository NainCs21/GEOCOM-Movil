package com.example.apimovil.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
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
import com.example.apimovil.databinding.FragmentReportesBinding
import com.example.apimovil.ui.common.*
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

class ReportesFragment : Fragment(R.layout.fragment_reportes) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentReportesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentReportesBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())

        val bottomNav = binding.root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.incluirNavInferior)
            ?: binding.root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavAdmin)
        if (SessionManager.esAdmin) {
            bottomNav?.menu?.clear()
            bottomNav?.inflateMenu(R.menu.bottom_nav_admin)
            bottomNav?.configurarNavegacionAdmin(findNavController(), R.id.reportesFragment)
            bottomNav?.vincularBadgeAvisos(viewLifecycleOwner, R.id.nav_mas)
            bottomNav?.vincularBadgeSolicitudes(viewLifecycleOwner, R.id.nav_solicitudes, model.solicitudesPendientes.map { it.data?.size ?: 0 })
            model.cargarSolicitudesPendientes()
        } else {
            bottomNav?.menu?.clear()
            bottomNav?.inflateMenu(R.menu.bottom_nav_supervisor)
            bottomNav?.configurarNavegacionSupervisor(findNavController(), R.id.reportesFragment)
            bottomNav?.vincularBadgeAvisos(viewLifecycleOwner, R.id.nav_avisos)
        }

        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.btnPeriodo.text = model.periodoSeleccionadoEtiqueta

        binding.btnPeriodo.setOnClickListener {
            val opciones = arrayOf(
                getString(R.string.periodo_todo),
                getString(R.string.periodo_este_mes),
                getString(R.string.periodo_mes_anterior),
                getString(R.string.periodo_ultimos_7_dias),
                getString(R.string.periodo_personalizado),
            )
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Seleccionar periodo")
                .setItems(opciones) { _, which ->
                    when (which) {
                        0 -> {
                            model.periodoSeleccionadoEtiqueta = getString(R.string.periodo_todo)
                            binding.btnPeriodo.text = model.periodoSeleccionadoEtiqueta
                            cargarReporteActual(null, null)
                        }
                        1 -> {
                            val desde = obtenerInicioMesString()
                            val hasta = obtenerFechaActualString()
                            model.periodoSeleccionadoEtiqueta = getString(R.string.periodo_este_mes)
                            binding.btnPeriodo.text = model.periodoSeleccionadoEtiqueta
                            cargarReporteActual(desde, hasta)
                        }
                        2 -> {
                            val (desdeMesAnt, hastaMesAnt) = obtenerMesAnteriorRango()
                            val etiqueta = "${formatearFechaIsoShort(desdeMesAnt)} - ${formatearFechaIsoShort(hastaMesAnt)}"
                            model.periodoSeleccionadoEtiqueta = etiqueta
                            binding.btnPeriodo.text = etiqueta
                            cargarReporteActual(desdeMesAnt, hastaMesAnt)
                        }
                        3 -> {
                            val (desde7d, hasta7d) = obtenerUltimos7DiasRango()
                            val etiqueta = "${formatearFechaIsoShort(desde7d)} - ${formatearFechaIsoShort(hasta7d)}"
                            model.periodoSeleccionadoEtiqueta = etiqueta
                            binding.btnPeriodo.text = etiqueta
                            cargarReporteActual(desde7d, hasta7d)
                        }
                        4 -> {
                            val picker = MaterialDatePicker.Builder.dateRangePicker()
                                .setTitleText("Seleccionar rango de fechas")
                                .build()
                            picker.addOnPositiveButtonClickListener { selection ->
                                val desde = convertirMillisAFecha(selection.first)
                                val hasta = convertirMillisAFecha(selection.second)
                                val etiqueta = "${formatearFechaIsoShort(desde)} - ${formatearFechaIsoShort(hasta)}"
                                model.periodoSeleccionadoEtiqueta = etiqueta
                                binding.btnPeriodo.text = etiqueta
                                cargarReporteActual(desde, hasta)
                            }
                            picker.show(parentFragmentManager, "dateRangePicker")
                        }
                    }
                }
                .show()
        }

        binding.btnExportarPdf.setOnClickListener {
            val esConductor = binding.toggleReporte.checkedButtonId == R.id.btnPorConductor
            val titulo = if (esConductor) getString(R.string.reporte_consumo_conductor) else getString(R.string.reporte_consumo_gasolinera)
            val columnaEntidad = if (esConductor) "Conductor" else "Gasolinera"
            val textoPeriodo = if (model.desdePeriodo.isNullOrBlank() || model.hastaPeriodo.isNullOrBlank()) {
                ""
            } else {
                "${formatearFechaIsoShort(model.desdePeriodo)} al ${formatearFechaIsoShort(model.hastaPeriodo)}"
            }

            val filas = if (esConductor) {
                model.reporteConductor.value.data.orEmpty().map { FilaReportePdf(it.nombre_conductor, it.cantidad_ordenes, it.monto_total) }
            } else {
                model.reporteGasolinera.value.data.orEmpty().map { FilaReportePdf(it.nombre_gasolinera, it.cantidad_ordenes, it.monto_total) }
            }

            if (filas.isEmpty()) return@setOnClickListener

            val totalOrdenes = filas.sumOf { it.cantidadOrdenes }
            val montoTotal = filas.sumOf { it.montoTotal }
            val nombreGenerador = SessionManager.nombre

            viewLifecycleOwner.lifecycleScope.launch {
                val pdfFile = generarReportePdf(
                    context = requireContext(),
                    tituloReporte = titulo,
                    textoPeriodo = textoPeriodo,
                    nombreGenerador = nombreGenerador,
                    nombreColumnaEntidad = columnaEntidad,
                    filas = filas,
                    totalOrdenes = totalOrdenes,
                    montoTotalGeneral = montoTotal,
                    esConductor = esConductor,
                    desde = model.desdePeriodo,
                    hasta = model.hastaPeriodo
                )

                if (pdfFile != null && pdfFile.exists()) {
                    try {
                        val uri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            pdfFile
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(intent, "Compartir reporte"))
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.toggleReporte.check(R.id.btnPorConductor)
        model.cargarReportePorConductor(model.desdePeriodo, model.hastaPeriodo)

        binding.btnPorConductor.setOnClickListener {
            binding.toggleReporte.check(R.id.btnPorConductor)
            model.cargarReportePorConductor(model.desdePeriodo, model.hastaPeriodo)
        }
        binding.btnPorGasolinera.setOnClickListener {
            binding.toggleReporte.check(R.id.btnPorGasolinera)
            model.cargarReportePorGasolinera(model.desdePeriodo, model.hastaPeriodo)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.reporteConductor.collect { estado ->
                    if (binding.toggleReporte.checkedButtonId == R.id.btnPorConductor) {
                        binding.progress.isVisible = estado.loading
                        val data = estado.data.orEmpty()
                        binding.textSinResultados.isVisible = (!estado.loading && estado.error == null && data.isEmpty())
                        binding.btnExportarPdf.isEnabled = !estado.loading && data.isNotEmpty()

                        binding.textTituloReporte.text = getString(R.string.reporte_consumo_conductor)
                        binding.textSubtituloPeriodo.text = if (model.desdePeriodo.isNullOrBlank() || model.hastaPeriodo.isNullOrBlank()) {
                            getString(R.string.ordenes_completadas_todo)
                        } else {
                            getString(R.string.ordenes_completadas_periodo, formatearFechaIsoShort(model.desdePeriodo), formatearFechaIsoShort(model.hastaPeriodo))
                        }

                        val totalOrdenes = data.sumOf { it.cantidad_ordenes }
                        val totalMonto = data.sumOf { it.monto_total }
                        val totalMontoFmt = String.format(Locale.US, "%,.2f", totalMonto)
                        binding.textTotalesGenerales.text = getString(R.string.resumen_ordenes_monto, totalOrdenes, totalMontoFmt)

                        val items = data.map {
                            val montoFmt = String.format(Locale.US, "%,.2f", it.monto_total)
                            FilaDoble(
                                id = it.conductor_id,
                                titulo = it.nombre_conductor,
                                subtitulo = "${it.cantidad_ordenes} órdenes · L $montoFmt"
                            )
                        }
                        binding.recycler.adapter = FilaDobleAdapter(items) { id, _ ->
                            val rep = data.find { it.conductor_id == id }
                            if (rep != null) {
                                val montoFmt = String.format(Locale.US, "%,.2f", rep.monto_total)
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(rep.nombre_conductor)
                                    .setMessage("ID Conductor: ${rep.conductor_id}\nÓrdenes totales: ${rep.cantidad_ordenes}\nMonto total: L $montoFmt")
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
                        val data = estado.data.orEmpty()
                        binding.textSinResultados.isVisible = (!estado.loading && estado.error == null && data.isEmpty())
                        binding.btnExportarPdf.isEnabled = !estado.loading && data.isNotEmpty()

                        binding.textTituloReporte.text = getString(R.string.reporte_consumo_gasolinera)
                        binding.textSubtituloPeriodo.text = if (model.desdePeriodo.isNullOrBlank() || model.hastaPeriodo.isNullOrBlank()) {
                            getString(R.string.ordenes_completadas_todo)
                        } else {
                            getString(R.string.ordenes_completadas_periodo, formatearFechaIsoShort(model.desdePeriodo), formatearFechaIsoShort(model.hastaPeriodo))
                        }

                        val totalOrdenes = data.sumOf { it.cantidad_ordenes }
                        val totalMonto = data.sumOf { it.monto_total }
                        val totalMontoFmt = String.format(Locale.US, "%,.2f", totalMonto)
                        binding.textTotalesGenerales.text = getString(R.string.resumen_ordenes_monto, totalOrdenes, totalMontoFmt)

                        val items = data.map {
                            val montoFmt = String.format(Locale.US, "%,.2f", it.monto_total)
                            FilaDoble(
                                id = it.gasolinera_id,
                                titulo = it.nombre_gasolinera,
                                subtitulo = "${it.cantidad_ordenes} órdenes · L $montoFmt"
                            )
                        }
                        binding.recycler.adapter = FilaDobleAdapter(items) { id, _ ->
                            val rep = data.find { it.gasolinera_id == id }
                            if (rep != null) {
                                val montoFmt = String.format(Locale.US, "%,.2f", rep.monto_total)
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(rep.nombre_gasolinera)
                                    .setMessage("ID Gasolinera: ${rep.gasolinera_id}\nÓrdenes totales: ${rep.cantidad_ordenes}\nMonto total: L $montoFmt")
                                    .setPositiveButton(R.string.aceptar, null)
                                    .show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cargarReporteActual(desde: String?, hasta: String?) {
        if (binding.toggleReporte.checkedButtonId == R.id.btnPorConductor) {
            model.cargarReportePorConductor(desde, hasta)
        } else {
            model.cargarReportePorGasolinera(desde, hasta)
        }
    }

    private fun convertirMillisAFecha(millis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(millis))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

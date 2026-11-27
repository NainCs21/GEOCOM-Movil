package com.example.apimovil.ui.admin

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.annotation.OptIn
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.notificaciones.NotificacionesStore
import com.example.apimovil.databinding.BottomSheetHistorialPreciosBinding
import com.example.apimovil.databinding.FragmentHomeAdminBinding
import com.example.apimovil.ui.common.*
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Suppress("RestrictedApi")
class HomeAdminFragment : Fragment(R.layout.fragment_home_admin) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentHomeAdminBinding? = null
    private val binding get() = _binding!!

    @OptIn(ExperimentalBadgeUtils::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeAdminBinding.bind(view)

        binding.textRolBarra.text = if (SessionManager.esAdmin)
            getString(R.string.rol_administrador) else getString(R.string.rol_supervisor)
        binding.textSaludo.text = getString(R.string.buen_dia_usuario, SessionManager.nombre ?: "")
        binding.textFecha.text = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("es", "HN"))
            .format(Date()).uppercase()

        // La barra inferior es distinta para admin (5 pestañas de gestión)
        // y supervisor (5 pestañas de solo consulta).
        val bottomNav = binding.incluirNavInferior.root as BottomNavigationView
        if (SessionManager.esAdmin) {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_admin)
            bottomNav.configurarNavegacionAdmin(findNavController(), R.id.homeAdminFragment)
            bottomNav.vincularBadgeAvisos(viewLifecycleOwner, R.id.nav_mas)
        } else {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_supervisor)
            bottomNav.configurarNavegacionSupervisor(findNavController(), R.id.homeAdminFragment)
            bottomNav.vincularBadgeAvisos(viewLifecycleOwner, R.id.nav_avisos)
        }

        val badgeDrawable = BadgeDrawable.create(requireContext())
        BadgeUtils.attachBadgeDrawable(badgeDrawable, binding.btnNotificaciones)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotificacionesStore.noLeidas.collect { count ->
                    badgeDrawable.isVisible = count > 0
                    badgeDrawable.number = count
                }
            }
        }

        binding.btnNotificaciones.setOnClickListener {
            findNavController().navigate(R.id.notificacionesFragment)
        }
        if (SessionManager.esAdmin) {
            binding.btnAsignarMontos.text = getString(R.string.asignar_montos)
        } else {
            binding.btnAsignarMontos.text = getString(R.string.ver_solicitudes)
        }
        binding.btnAsignarMontos.setOnClickListener {
            findNavController().navigate(R.id.solicitudesPendientesFragment)
        }
        binding.btnCatalogosRapido.setOnClickListener {
            findNavController().navigate(R.id.catalogosFragment)
        }

        binding.cardPreciosCombustible.setOnClickListener {
            mostrarBottomSheetPrecios()
        }

        binding.cardAbiertas.setOnClickListener { irAlHistorial("abierta,abierta_tarde") }
        binding.cardCompletadas.setOnClickListener { irAlHistorial("completada,completada_tarde") }
        binding.cardVencidas.setOnClickListener { irAlHistorial("vencida") }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.conteosOrdenes.collect { estado ->
                        estado.data?.let { (abiertas, completadas, vencidas) ->
                            binding.textAbiertas.text = abiertas.toString()
                            binding.textCompletadas.text = completadas.toString()
                            binding.textVencidas.text = vencidas.toString()
                        }
                    }
                }
                launch {
                    model.solicitudesPendientes.collect { estado ->
                        binding.textConteoPendientes.text = (estado.data?.size ?: 0).toString()
                    }
                }
                launch {
                    model.ordenes.collect { estado ->
                        val ordenes = estado.data.orEmpty()

                        val proxima = ordenes
                            .filter { esAbierta(it.estado) && it.vence_apertura != null }
                            .minByOrNull { parseFechaIso(it.vence_apertura!!).time }

                        if (proxima != null) {
                            binding.contenedorProximaAVencer.isVisible = true
                            binding.textOrdenProxima.text = proxima.numero_orden
                            val vence = parseFechaIso(proxima.vence_apertura!!).time
                            val minutosRestantes = ((vence - System.currentTimeMillis()) / 60000).coerceAtLeast(0)
                            binding.textGasolineraYVence.text = getString(
                                R.string.vence_a_las,
                                proxima.gasolinera?.nombre ?: "",
                                formatoLegible(proxima.vence_apertura!!)
                            )
                            binding.textMinutosRestantes.text = getString(R.string.quedan_n_min, minutosRestantes.toInt())
                            binding.btnVerOrden.setOnClickListener {
                                val bundle = Bundle().apply { putInt("ordenId", proxima.id) }
                                findNavController().navigate(R.id.ordenDetalleAdminFragment, bundle)
                            }
                        } else {
                            binding.contenedorProximaAVencer.isVisible = false
                        }
                    }
                }
                launch {
                    model.precios.collect { estado ->
                        binding.progressPrecios.isVisible = estado.loading
                        val lista = estado.data.orEmpty()
                        if (estado.error != null || lista.isEmpty()) {
                            binding.cardPreciosCombustible.isVisible = false
                        } else {
                            binding.cardPreciosCombustible.isVisible = true
                            binding.progressPrecios.isVisible = false

                            val primeraVigencia = lista.firstOrNull()?.vigente_desde
                            if (!primeraVigencia.isNullOrBlank()) {
                                binding.textPreciosSemana.text = getString(R.string.semana_del_formato, formatearFechaIsoShort(primeraVigencia))
                            } else {
                                binding.textPreciosSemana.text = getString(R.string.san_pedro_sula)
                            }

                            for (item in lista) {
                                val precioFmt = String.format(Locale.US, "L %.2f", item.precio)
                                val varAbs = abs(item.variacion)
                                val varFmt = String.format(Locale.US, "%.2f", varAbs)
                                val (varTexto, colorRes) = when {
                                    item.variacion > 0 -> Pair("▲ $varFmt", R.color.estado_vencida)
                                    item.variacion < 0 -> Pair("▼ $varFmt", R.color.estado_completada)
                                    else -> Pair("= 0.00", R.color.texto_secundario)
                                }
                                val colorInt = ContextCompat.getColor(requireContext(), colorRes)

                                when (item.combustible.lowercase().trim()) {
                                    "regular" -> {
                                        binding.textPrecioRegular.text = precioFmt
                                        binding.textVariacionRegular.text = varTexto
                                        binding.textVariacionRegular.setTextColor(colorInt)
                                    }
                                    "super", "súper" -> {
                                        binding.textPrecioSuper.text = precioFmt
                                        binding.textVariacionSuper.text = varTexto
                                        binding.textVariacionSuper.setTextColor(colorInt)
                                    }
                                    "diesel", "diésel" -> {
                                        binding.textPrecioDiesel.text = precioFmt
                                        binding.textVariacionDiesel.text = varTexto
                                        binding.textVariacionDiesel.setTextColor(colorInt)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        model.cargarConteosOrdenes()
        model.cargarSolicitudesPendientes()
        model.cargarOrdenes()
        model.cargarPrecios()
        model.cargarHistorialPrecios()
    }

    private fun mostrarBottomSheetPrecios() {
        val sheetBinding = BottomSheetHistorialPreciosBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        var tipoCombustibleActual = "diesel"
        sheetBinding.toggleCombustible.check(R.id.btnDiesel)

        val actualizarGraficaYMinMax = {
            val hp = model.historialPrecios.value.data
            if (hp != null && hp.semanas.isNotEmpty()) {
                val barras = mutableListOf<BarChartView.Barra>()
                val preciosList = mutableListOf<Double>()

                for (semana in hp.semanas) {
                    val fechaStr = semana["vigente_desde"] as? String ?: ""
                    val etiquetaFecha = formatearFechaIsoShort(fechaStr)
                    val valorObj = semana[tipoCombustibleActual]
                    val precio = (valorObj as? Number)?.toDouble()
                    if (precio != null) {
                        barras.add(BarChartView.Barra(etiquetaFecha, precio))
                        preciosList.add(precio)
                    }
                }

                sheetBinding.chartHistorial.setDatos(barras)
                if (preciosList.isNotEmpty()) {
                    val min = preciosList.minOrNull() ?: 0.0
                    val max = preciosList.maxOrNull() ?: 0.0
                    val actual = preciosList.lastOrNull() ?: 0.0
                    sheetBinding.textMinMaxActual.text = getString(
                        R.string.min_max_actual_formato,
                        min, max, actual
                    )
                } else {
                    sheetBinding.textMinMaxActual.text = ""
                }
            } else {
                sheetBinding.chartHistorial.setDatos(emptyList())
                sheetBinding.textMinMaxActual.text = ""
            }
        }

        sheetBinding.toggleCombustible.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                tipoCombustibleActual = when (checkedId) {
                    R.id.btnRegular -> "regular"
                    R.id.btnSuper -> "super"
                    else -> "diesel"
                }
                actualizarGraficaYMinMax()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.historialPrecios.collect { estado ->
                    if (estado.data != null) {
                        actualizarGraficaYMinMax()
                    }
                }
            }
        }

        actualizarGraficaYMinMax()
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        model.cargarConteosOrdenes()
        model.cargarSolicitudesPendientes()
        model.cargarOrdenes()
        model.cargarPrecios()
        model.cargarHistorialPrecios()
    }

    private fun irAlHistorial(filtroEstado: String) {
        val opciones = NavOptions.Builder()
            .setPopUpTo(R.id.homeAdminFragment, false)
            .build()
        findNavController().navigate(
            R.id.ordenesAdminFragment,
            bundleOf("filtroEstado" to filtroEstado),
            opciones
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

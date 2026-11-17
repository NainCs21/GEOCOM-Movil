package com.example.apimovil.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.databinding.FragmentCajaChicaBinding
import com.example.apimovil.databinding.ItemFilaDobleBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registro de caja chica para trabajos foráneos. El mecanismo de validación
 * física para estos casos sigue pendiente de definir con el cliente, así
 * que esto es solo un registro de montos entregados, sin cruce automático.
 */
class CajaChicaFragment : Fragment(R.layout.fragment_caja_chica) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentCajaChicaBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCajaChicaBinding.bind(view)

        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.contenedorRegistro.isVisible = SessionManager.esAdmin

        binding.btnRegistrar.setOnClickListener {
            val conductorId = binding.inputConductorId.text?.toString()?.toIntOrNull()
            val monto = binding.inputMonto.text?.toString()?.toDoubleOrNull()
            val descripcion = binding.inputDescripcion.text?.toString()?.trim().orEmpty()
            val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            if (monto == null || descripcion.isBlank()) {
                binding.textError.isVisible = true
                binding.textError.text = getString(R.string.completa_monto_y_descripcion)
                return@setOnClickListener
            }
            model.registrarCajaChica(conductorId, monto, descripcion, fechaHoy)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.registroCajaChica.collect { estado ->
                    binding.progress.isVisible = estado.loading
                    if (estado.error != null) {
                        binding.textError.isVisible = true
                        binding.textError.text = estado.error
                    }
                    if (estado.data == true) {
                        binding.inputConductorId.text?.clear()
                        binding.inputMonto.text?.clear()
                        binding.inputDescripcion.text?.clear()
                        model.cargarCajaChica()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.cajaChica.collect { estado ->
                    binding.progressLista.isVisible = estado.loading
                    binding.contenedorListado.removeAllViews()
                    estado.data.orEmpty().forEach { entrega ->
                        val fila = ItemFilaDobleBinding.inflate(
                            LayoutInflater.from(requireContext()), binding.contenedorListado, false
                        )
                        fila.textTitulo.text = entrega.conductor?.usuario?.name
                            ?: getString(R.string.conductor_numero, entrega.conductor_id ?: 0)
                        fila.textSubtitulo.text = getString(
                            R.string.monto_y_descripcion, entrega.monto, entrega.descripcion
                        )
                        binding.contenedorListado.addView(fila.root)
                    }
                }
            }
        }

        model.cargarCajaChica()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

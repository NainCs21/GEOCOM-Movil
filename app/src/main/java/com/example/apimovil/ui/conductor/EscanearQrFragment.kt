package com.example.apimovil.ui.conductor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.apimovil.R
import com.example.apimovil.databinding.FragmentEscanearQrBinding
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import kotlinx.coroutines.launch

class EscanearQrFragment : Fragment(R.layout.fragment_escanear_qr) {

    private val model: ConductorViewModel by viewModels()
    private var _binding: FragmentEscanearQrBinding? = null
    private val binding get() = _binding!!

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            iniciarCamara()
        }
    }

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            val text = result?.text
            if (!text.isNullOrBlank()) {
                binding.barcodeScanner.pause()
                model.escanear(text)
            }
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentEscanearQrBinding.bind(view)
        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        model.limpiarEscaneo()

        verificarPermisoYIniciar()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ordenEscaneada.collect { estado ->
                    binding.progress.isVisible = estado.loading
                    binding.contenedorMensaje.isVisible = estado.error != null
                    binding.textMensaje.text = estado.error

                    if (estado.error != null) {
                        // Si hubo error, reanudar la cámara para poder intentar de nuevo
                        binding.barcodeScanner.resume()
                    }

                    val orden = estado.data
                    if (orden != null) {
                        val bundle = Bundle().apply { putInt("ordenId", orden.id) }
                        findNavController().navigate(R.id.ordenDetalleFragment, bundle)
                    }
                }
            }
        }
    }

    private fun verificarPermisoYIniciar() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                iniciarCamara()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun iniciarCamara() {
        binding.barcodeScanner.decodeContinuous(callback)
        binding.barcodeScanner.resume()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            binding.barcodeScanner.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

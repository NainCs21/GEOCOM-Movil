package com.example.apimovil.ui.conductor

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.apimovil.R
import com.example.apimovil.databinding.FragmentInstruccionEvidenciaBinding
import com.example.apimovil.ui.common.Validadores
import kotlinx.coroutines.launch
import java.io.File

class InstruccionEvidenciaFragment : Fragment(R.layout.fragment_instruccion_evidencia) {

    private val model: ConductorViewModel by viewModels()
    private var _binding: FragmentInstruccionEvidenciaBinding? = null
    private val binding get() = _binding!!

    private var detalleId: Int = -1
    private var ordenId: Int = -1
    private var tipo: String = "bomba"
    private var justificacionTardanza: String? = null

    private var archivoFotoTemporal: File? = null

    private val lanzadorCamara = registerForActivityResult(ActivityResultContracts.TakePicture()) { exitoso ->
        val archivo = archivoFotoTemporal
        if (exitoso) {
            if (archivo != null && archivo.exists() && archivo.length() > 0) {
                model.subirEvidencia(detalleId, tipo, justificacionTardanza, archivo, ordenId)
            } else {
                archivo?.delete()
                Toast.makeText(requireContext(), "La foto no se guardó bien. Vuelve a tomarla.", Toast.LENGTH_SHORT).show()
            }
        } else {
            archivo?.delete()
        }
        archivoFotoTemporal = null
    }

    private val lanzadorPermisoCamara = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            abrirCamara()
        } else {
            Toast.makeText(requireContext(), "Se requiere permiso de cámara para tomar la evidencia.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            val path = savedInstanceState.getString("archivoFotoTemporalPath")
            if (!path.isNullOrBlank()) {
                val f = File(path)
                val evidenciasDir = File(requireContext().cacheDir, "evidencias")
                if (f.canonicalPath.startsWith(evidenciasDir.canonicalPath)) {
                    archivoFotoTemporal = f
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        archivoFotoTemporal?.let {
            outState.putString("archivoFotoTemporalPath", it.absolutePath)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentInstruccionEvidenciaBinding.bind(view)

        detalleId = arguments?.getInt("detalleId") ?: -1
        ordenId = arguments?.getInt("ordenId") ?: -1
        tipo = arguments?.getString("tipo").orEmpty()
        justificacionTardanza = arguments?.getString("justificacionTardanza")

        if (!Validadores.idValido(detalleId) || !Validadores.idValido(ordenId) || !Validadores.tipoEvidenciaValido(tipo)) {
            Toast.makeText(requireContext(), "No se pudo abrir esta pantalla. Vuelve a intentarlo.", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }

        when (tipo) {
            "bomba" -> {
                binding.textTituloInstruccion.text = getString(R.string.instruccion_bomba_titulo)
                binding.textDescripcionInstruccion.text = getString(R.string.instruccion_bomba_desc)
                binding.imgEjemplo.setImageResource(R.drawable.ejemplo_bomba)
            }
            "tablero" -> {
                binding.textTituloInstruccion.text = getString(R.string.instruccion_tablero_titulo)
                binding.textDescripcionInstruccion.text = getString(R.string.instruccion_tablero_desc)
                binding.imgEjemplo.setImageResource(R.drawable.ejemplo_tablero)
            }
            "tanque" -> {
                binding.textTituloInstruccion.text = getString(R.string.instruccion_tanque_titulo)
                binding.textDescripcionInstruccion.text = getString(R.string.instruccion_tanque_desc)
                binding.imgEjemplo.setImageResource(R.drawable.ejemplo_tanque_gas)
            }
            "factura" -> {
                binding.textTituloInstruccion.text = getString(R.string.instruccion_factura_titulo)
                binding.textDescripcionInstruccion.text = getString(R.string.instruccion_factura_desc)
                binding.imgEjemplo.setImageResource(R.drawable.ejemplo_factura)
            }
            else -> {
                binding.textTituloInstruccion.text = getString(R.string.evidencia)
                binding.textDescripcionInstruccion.text = getString(R.string.recomendacion_foto_clara)
            }
        }

        binding.btnAbrirCamara.setOnClickListener {
            verificarPermisoYTomarFoto()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.evidencia.collect { estado ->
                    binding.progress.isVisible = estado.loading
                    binding.btnAbrirCamara.isEnabled = !estado.loading
                    binding.textError.isVisible = estado.error != null
                    binding.textError.text = estado.error

                    if (estado.data != null) {
                        Toast.makeText(requireContext(), "¡Evidencia subida con éxito!", Toast.LENGTH_SHORT).show()
                        model.limpiarEvidencia()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun verificarPermisoYTomarFoto() {
        val tienePermiso = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (tienePermiso) {
            abrirCamara()
        } else {
            lanzadorPermisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        val evidenciasDir = File(requireContext().cacheDir, "evidencias")
        if (!evidenciasDir.exists()) evidenciasDir.mkdirs()

        val archivo = File.createTempFile("evidencia_${tipo}_${detalleId}_", ".jpg", evidenciasDir)
        archivoFotoTemporal = archivo
        val uri: Uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            archivo
        )
        lanzadorCamara.launch(uri)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

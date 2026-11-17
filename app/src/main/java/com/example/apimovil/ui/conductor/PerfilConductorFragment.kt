package com.example.apimovil.ui.conductor

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.databinding.FragmentPerfilConductorBinding

class PerfilConductorFragment : Fragment(R.layout.fragment_perfil_conductor) {

    private var _binding: FragmentPerfilConductorBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPerfilConductorBinding.bind(view)
        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.textNombre.text = SessionManager.nombre ?: ""
        binding.textCorreo.text = SessionManager.correo ?: ""

        binding.btnCerrarSesion.setOnClickListener {
            SessionManager.cerrarSesion()
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

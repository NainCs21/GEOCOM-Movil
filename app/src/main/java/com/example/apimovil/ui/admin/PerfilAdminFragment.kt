package com.example.apimovil.ui.admin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.databinding.FragmentPerfilAdminBinding

class PerfilAdminFragment : Fragment(R.layout.fragment_perfil_admin) {

    private var _binding: FragmentPerfilAdminBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPerfilAdminBinding.bind(view)

        binding.textRolBadge.text = if (SessionManager.esAdmin)
            getString(R.string.rol_administrador) else getString(R.string.rol_supervisor)
        binding.textNombre.text = SessionManager.nombre ?: ""
        binding.textCorreo.text = SessionManager.correo ?: ""

        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
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

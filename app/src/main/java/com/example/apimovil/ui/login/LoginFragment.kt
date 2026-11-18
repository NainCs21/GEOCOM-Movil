package com.example.apimovil.ui.login

import android.os.Bundle
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
import com.example.apimovil.data.SesionEventos
import com.example.apimovil.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val model: LoginViewModel by viewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentLoginBinding.bind(view)

        // Mostrar mensaje de sesion expirada o inactividad si existe
        val mensajeSalida = SessionManager.obtenerYLimpiarMensajeSalida()
        if (!mensajeSalida.isNullOrBlank()) {
            Snackbar.make(binding.root, mensajeSalida, Snackbar.LENGTH_LONG).show()
        }
        SesionEventos.reiniciarGuardia()

        binding.btnIngresar.setOnClickListener {
            val correo = binding.inputCorreo.text?.toString().orEmpty()
            val contrasena = binding.inputContrasena.text?.toString().orEmpty()
            model.login(correo, contrasena)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { estado ->
                    binding.progress.isVisible = estado.loading
                    binding.btnIngresar.isEnabled = !estado.loading
                    binding.textError.isVisible = estado.error != null
                    binding.textError.text = estado.error

                    if (estado.data == true) {
                        irAHome()
                    }
                }
            }
        }
    }

    private fun irAHome() {
        val destino = when {
            SessionManager.esConductor -> R.id.homeConductorFragment
            else -> R.id.homeAdminFragment // admin y contabilidad comparten el mismo panel de inicio
        }
        findNavController().navigate(destino)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

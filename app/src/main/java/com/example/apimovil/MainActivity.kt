package com.example.apimovil

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.apimovil.data.SessionManager
import com.example.apimovil.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController: NavController = navHost.navController

        // Si ya hay sesión guardada, nos saltamos el login directo al panel que corresponda.
        if (SessionManager.haySesion) {
            val destino = if (SessionManager.esConductor) R.id.homeConductorFragment else R.id.homeAdminFragment
            val graph = navController.navInflater.inflate(R.navigation.nav_graph)
            graph.setStartDestination(destino)
            navController.graph = graph
        }
    }
}

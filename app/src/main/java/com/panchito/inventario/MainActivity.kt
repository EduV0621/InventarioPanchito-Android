package com.panchito.inventario

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.panchito.inventario.navigation.NavGraph
import com.panchito.inventario.ui.theme.InventarioPanchitoTheme

/**
 * Actividad unica del proyecto (patron de una sola Activity + Compose Navigation),
 * requisito tecnico del Sprint 1: el esqueleto navega entre pantallas sin logica de negocio aun.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InventarioPanchitoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph()
                }
            }
        }
    }
}

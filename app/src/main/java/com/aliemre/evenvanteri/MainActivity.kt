package com.aliemre.evenvanteri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aliemre.evenvanteri.ui.AppNavigation
import com.aliemre.evenvanteri.ui.theme.EvEnvanteriTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as EvEnvanteriApp).container

        setContent {
            EvEnvanteriTheme {
                AppNavigation(repository = container.inventoryRepository)
            }
        }
    }
}

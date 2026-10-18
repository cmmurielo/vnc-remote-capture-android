package com.capturapantalla.vnc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.capturapantalla.vnc.ui.MainViewModel
import com.capturapantalla.vnc.ui.screens.MainScreen
import com.capturapantalla.vnc.ui.theme.CapturaPantallaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CapturaPantallaTheme {
                MainScreen(
                    viewModel = viewModel,
                    onShareIntent = { intent ->
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

package com.engine.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val engineViewModel: EngineViewModel = viewModel()
                    val rootNode = engineViewModel.rootNode
                    val isLoading = engineViewModel.isLoading
                    val loadError = engineViewModel.loadError

                    LaunchedEffect(Unit) {
                        if (rootNode == null) {
                            // Load immediately the first time safely
                            engineViewModel.loadApplication(this@MainActivity)
                        }
                    }

                    if (isLoading) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(color = Color.Blue)
                        }
                    } else if (loadError != null && rootNode == null) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text(
                                text = "Ocurrió un error cargando el Motor:\n\n$loadError",
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (rootNode != null) {
                        // It loaded from network or local fallback! Just render it.
                        RenderNode(node = rootNode, onAction = engineViewModel::onNodeAction)
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Pantalla en blanco. JSON no cargado.", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

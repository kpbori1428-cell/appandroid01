package com.engine.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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

                    if (rootNode == null) {
                        // Load immediately the first time
                        engineViewModel.loadApplication(this)
                    } else {
                        // It survived rotation! Just render it.
                        RenderNode(node = rootNode)
                    }
                }
            }
        }
    }
}

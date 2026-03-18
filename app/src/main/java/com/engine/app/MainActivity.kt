package com.engine.app

import android.os.Bundle
import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

                    // Preparar los permisos base que la Super App podría necesitar
                    val permissionsToRequest = mutableListOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )

                    // Bluetooth Permissions (API 31+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH)
                    }

                    // Media/Storage Permissions (API 33+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                        permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                    } else {
                        permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }

                    // Lanzador nativo de Android para solicitar permisos
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        // Aquí podríamos bloquear la app si rechazan algo vital,
                        // pero la filosofía del Motor JSON es que todo es dinámico,
                        // así que dejamos que la app cargue y si el JSON luego usa la cámara y no hay permiso, fallará en ese nodo.

                        // Una vez respondido el popup, cargamos el Motor si no estaba cargado
                        if (rootNode == null) {
                            engineViewModel.loadApplication(this@MainActivity)
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (rootNode == null) {
                            // Primero lanzamos la solicitud masiva de permisos al abrir la App
                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
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

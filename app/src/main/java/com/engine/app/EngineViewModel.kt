package com.engine.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engine.core.CoreAlgorithms
import com.engine.core.LifecycleManager
import com.engine.core.Node
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class EngineViewModel : ViewModel() {
    private val telemetryBus = MockTelemetryBus()
    private val coreAlgorithms = CoreAlgorithms(telemetryBus)

    // Pass the viewModelScope so the engine can manage real background jobs tied to this ViewModel
    val lifecycleManager = LifecycleManager(telemetryBus, coreAlgorithms, viewModelScope)

    var rootNode by mutableStateOf<Node?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var loadError by mutableStateOf<String?>(null)
        private set

    // Este es el punto de anclaje estático en la App compilada.
    // Esta es tu URL maestra pública y real en internet que acabamos de crear:
    private val MASTER_CONFIG_URL = "https://kelokura00-creator.github.io/public/android_url.json"

    fun loadApplication(context: Context) {
        if (rootNode != null) return // Already loaded, survive rotation

        viewModelScope.launch {
            isLoading = true
            loadError = null

            try {
                // 1. Fetch Remote Config JSON (El archivo Maestro en Internet REAL)
                val configRawJson = fetchRemoteJson(MASTER_CONFIG_URL)

                val configMap = JsonParser.parsePatch(configRawJson)
                val remoteAppUrl = configMap["URL"] as? String ?: ""

                if (remoteAppUrl.isEmpty()) {
                    throw Exception("No se encontró el campo 'URL' en el config maestro.")
                }

                // 2. Fetch Remote JSON App Config (La App completa desde la URL inyectada en el config)
                val rawAppJson = fetchRemoteJson(remoteAppUrl)

                // 3. Parse and Mount App
                val parsedRoot = JsonParser.parseUiTree(rawAppJson)
                lifecycleManager.mount(parsedRoot)
                rootNode = parsedRoot

            } catch (e: Exception) {
                e.printStackTrace()
                loadError = "Error descargando la App: ${e.message}"

                // Si falla tu internet, cargar el diseño local de respaldo (Login App)
                loadFallbackLocalApp(context)
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun fetchRemoteJson(urlString: String): String {
        return withContext(Dispatchers.IO) {
            // Este es un GET real a la URL que le pasemos, sin simulaciones ni trampas.
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("Servidor respondió con código: $responseCode")
            }
        }
    }

    private fun loadFallbackLocalApp(context: Context) {
        try {
            val stream = context.assets.open("ui_tree.json")
            val rawJson = InputStreamReader(stream).readText()
            val parsedRoot = JsonParser.parseUiTree(rawJson)
            lifecycleManager.mount(parsedRoot)
            rootNode = parsedRoot
        } catch (e: Exception) {
            // Error fatal si el archivo local tampoco existe
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Unmount everything and cancel pending jobs
        rootNode?.let { lifecycleManager.unmount(it) }
    }
}

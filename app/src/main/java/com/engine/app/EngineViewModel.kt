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

    fun loadApplication(context: Context) {
        if (rootNode != null) return // Already loaded, survive rotation

        viewModelScope.launch {
            isLoading = true
            loadError = null

            try {
                // 1. Read config.json to get the Remote URL
                val assetManager = context.assets
                val configStream = assetManager.open("config.json")
                val configReader = InputStreamReader(configStream)
                val configRawJson = configReader.readText()
                configReader.close()

                val configMap = JsonParser.parsePatch(configRawJson)
                val remoteUrl = configMap["remoteUrl"] as? String ?: ""

                // 2. Fetch Remote JSON App Config
                val rawAppJson = fetchRemoteJson(remoteUrl, context)

                // 3. Parse and Mount App
                val parsedRoot = JsonParser.parseUiTree(rawAppJson)
                lifecycleManager.mount(parsedRoot)
                rootNode = parsedRoot

                // Simulating Server Push Update (Optional)
                // simulateServerPatch(context)

            } catch (e: Exception) {
                e.printStackTrace()
                loadError = "Error descargando la App: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun fetchRemoteJson(urlString: String, context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                // For demonstration, if it's the default Github mock URL, we fallback to our local test
                // To actually test, update config.json with a real RAW Github URL.
                if (urlString.contains("TuRepositorio")) {
                    val stream = context.assets.open("ui_tree.json")
                    return@withContext InputStreamReader(stream).readText()
                }

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
            } catch (e: Exception) {
                // Fallback to local ui_tree.json if network fails
                val stream = context.assets.open("ui_tree.json")
                InputStreamReader(stream).readText()
            }
        }
    }

    private fun simulateServerPatch(context: Context) {
        viewModelScope.launch {
            delay(5000)
            val assetManager = context.assets
            val patchInputStream = assetManager.open("patch.json")
            val patchReader = InputStreamReader(patchInputStream)
            val patchRawJson = patchReader.readText()
            patchReader.close()

            val patchData = JsonParser.parsePatch(patchRawJson)
            rootNode?.let { root ->
                lifecycleManager.update(root, patchData) { newChildren ->
                    JsonParser.parseNodes(newChildren)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Unmount everything and cancel pending jobs
        rootNode?.let { lifecycleManager.unmount(it) }
    }
}

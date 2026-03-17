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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStreamReader

class EngineViewModel : ViewModel() {
    private val telemetryBus = MockTelemetryBus()
    private val coreAlgorithms = CoreAlgorithms(telemetryBus)

    // Pass the viewModelScope so the engine can manage real background jobs tied to this ViewModel
    val lifecycleManager = LifecycleManager(telemetryBus, coreAlgorithms, viewModelScope)

    var rootNode by mutableStateOf<Node?>(null)
        private set

    fun loadApplication(context: Context) {
        if (rootNode != null) return // Already loaded, survive rotation

        viewModelScope.launch {
            // Load Initial JSON
            val assetManager = context.assets
            val inputStream = assetManager.open("ui_tree.json")
            val reader = InputStreamReader(inputStream)
            val rawJson = reader.readText()
            reader.close()

            val parsedRoot = JsonParser.parseUiTree(rawJson)
            lifecycleManager.mount(parsedRoot)
            rootNode = parsedRoot

            // Simulating Server Push Update
            simulateServerPatch(context)
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

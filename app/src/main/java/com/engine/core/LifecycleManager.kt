package com.engine.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

interface ComponentInstance {
    fun applyProperties(properties: Map<String, Any>)
    fun destroy()
}

class LifecycleManager(
    private val telemetryBus: TelemetryBus,
    private val coreAlgorithms: CoreAlgorithms,
    private val coroutineScope: CoroutineScope
) {

    // Manages native component instances mapped by node ID
    private val componentInstances = mutableMapOf<String, ComponentInstance>()

    // Tracks active coroutines/jobs mapped by node ID
    private val activeJobs = mutableMapOf<String, Job>()

    // Fase de Montaje (Mount)
    fun mount(node: Node, currentDepth: Int = 0) {
        if (currentDepth > 32) {
            throw StackOverflowError("Profundidad Excedida: Exceeded maximum depth of 32 levels during mount.")
        }

        // Resolves Type and creates physical entity (simulated here)
        val instance = createComponentInstance(node)
        componentInstances[node.id] = instance

        // Request async hardware permissions if LogicDirectives require it
        handleHardwarePermissions(node)

        // Recursively mount children
        for (child in node.children) {
            mount(child, currentDepth + 1)
        }
    }

    // Fase de Actualización (Update)
    fun update(node: Node, patchData: Map<String, Any>, parseNodesFunc: ((List<Map<String, Any>>) -> List<Node>)? = null) {

        // Handle structural updates (children replacement)
        if (patchData.containsKey("children") && patchData["children"] is List<*>) {
            @Suppress("UNCHECKED_CAST")
            val newChildrenRaw = patchData["children"] as List<Map<String, Any>>

            if (parseNodesFunc != null) {
                 node.children = parseNodesFunc(newChildrenRaw)
            }
        }

        // Separate logic directives from aesthetic properties
        val logicPatch = patchData["logicDirectives"] as? Map<String, Any>
        if (logicPatch != null) {
             coreAlgorithms.merge(node.logicDirectives, logicPatch)
        }

        // Remove structural keys to only leave aesthetic properties
        val aestheticPatch = patchData.filterKeys { it != "children" && it != "logicDirectives" && it != "id" && it != "path" && it != "type" }

        // Copia profunda previa para el diffing
        val oldProps = deepCopyMap(node.aestheticProperties)

        // Ejecuta una Fusión Profunda directamente en las propiedades del nodo (Parche Remoto sobre Estado Base)
        coreAlgorithms.merge(node.aestheticProperties, aestheticPatch)

        // Matriz de Prioridad: Interacción de Usuario (Cámara_Memoria) > Parche Remoto > Estado Base
        // Overwrite patched properties with any local user state stored in memoryCamera
        for ((key, value) in node.memoryCamera) {
            node.aestheticProperties[key] = value
        }

        // Calcula las propiedades modificadas (Diffing)
        val diffProps = mutableMapOf<String, Any>()
        for ((key, value) in node.aestheticProperties) {
            if (oldProps[key] != value) {
                diffProps[key] = value
            }
        }

        // Re-render and notify Compose reactive state
        componentInstances[node.id]?.applyProperties(diffProps)
        node.notifyStateChanged()
    }

    private fun deepCopyMap(map: Map<String, Any>): Map<String, Any> {
        val copy = mutableMapOf<String, Any>()
        for ((key, value) in map) {
            when (value) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    copy[key] = deepCopyMap(value as Map<String, Any>)
                }
                is List<*> -> {
                    // Si es una lista, también hacemos una copia superficial de ella (simplificado para este contexto)
                    copy[key] = ArrayList(value)
                }
                else -> {
                    copy[key] = value
                }
            }
        }
        return copy
    }

    // Fase de Desmontaje (Unmount)
    fun unmount(node: Node) {
        // Cancelar todos los trabajos asíncronos vinculados a la identidad del nodo.
        cancelJobs(node.id)

        // Destruir cachés de memoria pesada y liberar recursos hardware
        componentInstances[node.id]?.destroy()
        componentInstances.remove(node.id)

        // Desvincular escuchadores globales y vaciar la Cámara_Memoria
        node.memoryCamera.clear()

        // Recursively unmount children
        for (child in node.children) {
            unmount(child)
        }
    }

    private fun createComponentInstance(node: Node): ComponentInstance {
        // Factory logic to instantiate native components based on node.type
        // Fallback validation: Unknown type instantiates an empty space cube (0x0 dp container)
        return object : ComponentInstance {
            override fun applyProperties(properties: Map<String, Any>) {
                // Apply visual style and update native view
            }

            override fun destroy() {
                // Release hardware resources (MediaRecorder, CameraDevice, LocationManager)
                // Destroy heavy caches (Bitmaps)
            }
        }
    }

    private fun handleHardwarePermissions(node: Node) {
        // Evaluate node.logicDirectives to check for permissions (e.g., allowVideo)
        val directives = node.logicDirectives

        // Example check: if (directives["requiresCamera"] == true) {
        //   ContextCompat.checkSelfPermission(...)
        //   If denied permanently -> telemetryBus.emit(errorPayload)
        // }
    }

    // Delegación Asíncrona (Background Thread execution for heavy logic directives like compression)
    fun executeAsyncOperation(node: Node, operation: suspend () -> Unit) {
        // Execute on background thread
        val job = coroutineScope.launch(Dispatchers.IO) {
            operation()
        }

        // Track the job tied to the node's lifecycle
        activeJobs[node.id] = job
    }

    private fun cancelJobs(nodeId: String) {
        // Enforce cleanup contract: Cancel ongoing coroutines/threads tied to this node
        activeJobs[nodeId]?.cancel()
        activeJobs.remove(nodeId)
    }
}

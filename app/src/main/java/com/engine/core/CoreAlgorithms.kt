package com.engine.core

class CoreAlgorithms(private val telemetryBus: TelemetryBus) {

    // A. Gestión de la Cámara de Memoria (Restauración y Transmisión)

    fun saveLocalState(node: Node, property: String, value: Any) {
        if (!node.memoryCamera.containsKey(property)) {
            node.memoryCamera[property] = value
        }
    }

    fun transmitState(node: Node, event: String, clearMemory: Boolean = false) {
        val payload = mapOf(
            "origen" to node.path,
            "evento" to event,
            "memoria" to HashMap(node.memoryCamera) // Send a copy
        )

        telemetryBus.emit(payload)

        if (clearMemory) {
            node.memoryCamera.clear()
        }
    }

    // --- V5 Advanced Functionalities (Derived from V4 JS Engine logic) ---
    // Finds a node in the tree using the full path recursively
    fun getNodeByPath(root: Node, path: String): Node? {
        if (root.path == path || root.id == path) {
            return root
        }
        for (child in root.reactiveChildren) {
            val found = getNodeByPath(child, path)
            if (found != null) {
                return found
            }
        }
        return null
    }

    // Handles cross-element interactions ('acciones' logic from V4 JS Engine)
    fun executeActions(sourceNode: Node, root: Node, triggerType: String, actionsList: List<Map<String, Any>>?) {
        if (actionsList == null) return

        for (action in actionsList) {
            val trigger = action["disparador"] as? String ?: "click"
            val targetPath = action["objetivo"] as? String
            val styles = action["estilos"] as? Map<String, Any>

            if (trigger == triggerType && targetPath != null && styles != null) {
                val targetNode = getNodeByPath(root, targetPath)
                if (targetNode != null) {
                    val isActive = targetNode.internalState["action_active"] as? Boolean ?: false
                    targetNode.internalState["action_active"] = !isActive

                    if (!isActive) {
                        // Backup original styles before applying the new ones
                        val originals = mutableMapOf<String, Any>()
                        for (key in styles.keys) {
                            if (targetNode.reactiveAesthetics.containsKey(key)) {
                                targetNode.reactiveAesthetics[key]?.let { originals[key] = it }
                            } else {
                                // If the property didn't exist before, we flag it for removal on revert (e.g., store null)
                                originals[key] = "ENGINE_NULL_PROPERTY_FLAG"
                            }
                        }
                        targetNode.internalState["action_originals"] = originals

                        // Apply new styles
                        merge(targetNode.reactiveAesthetics, styles)
                    } else {
                        // Revert to original backed-up styles
                        @Suppress("UNCHECKED_CAST")
                        val originals = targetNode.internalState["action_originals"] as? Map<String, Any>
                        if (originals != null) {
                            for ((k, v) in originals) {
                                if (v == "ENGINE_NULL_PROPERTY_FLAG") {
                                    targetNode.reactiveAesthetics.remove(k)
                                } else {
                                    targetNode.reactiveAesthetics[k] = v
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // B. Algoritmo de Fusión de Datos (Patching)

    fun merge(principal: MutableMap<String, Any>, parcial: Map<String, Any>, nivelActual: Int = 0) {
        if (nivelActual > 32) {
            throw StackOverflowError("Profundidad Excedida: Exceeded maximum depth of 32 levels during data merge.")
        }

        for ((key, value) in parcial) {
            when (value) {
                is List<*> -> {
                    // SI es Lista ENTONCES Sobrescribir Principal[Llave]
                    principal[key] = value
                }
                is Map<*, *> -> {
                    // SI es Objeto ENTONCES
                    if (principal.containsKey(key) && principal[key] is Map<*, *>) {
                        // SI existe en Principal ENTONCES
                        @Suppress("UNCHECKED_CAST")
                        val existingMap = principal[key] as Map<String, Any>
                        val mutableExistingMap = if (existingMap is MutableMap) {
                            existingMap
                        } else {
                            existingMap.toMutableMap()
                        }

                        // We must set it back in case it wasn't a MutableMap originally
                        principal[key] = mutableExistingMap

                        @Suppress("UNCHECKED_CAST")
                        merge(
                            mutableExistingMap,
                            value as Map<String, Any>,
                            nivelActual + 1
                        )
                    } else {
                        // SI NO ENTONCES Principal[Llave] = Parcial[Llave]
                        // Recursively convert to MutableMap to allow further deep merging if needed later
                        @Suppress("UNCHECKED_CAST")
                        principal[key] = deepMutableMap(value as Map<String, Any>)
                    }
                }
                else -> {
                    // SI NO ENTONCES Principal[Llave] = Parcial[Llave]
                    principal[key] = value
                }
            }
        }
    }

    private fun deepMutableMap(map: Map<String, Any>): MutableMap<String, Any> {
        val result = mutableMapOf<String, Any>()
        for ((k, v) in map) {
            when (v) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    result[k] = deepMutableMap(v as Map<String, Any>)
                }
                else -> result[k] = v
            }
        }
        return result
    }
}

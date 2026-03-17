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

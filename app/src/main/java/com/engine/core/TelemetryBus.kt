package com.engine.core

interface TelemetryBus {
    fun emit(payload: Map<String, Any>)
}

// Emitting gestures to the bus and handling asynchronous tasks asynchronously
object InputNormalizer {
    fun handleEvent(node: Node, eventType: String, eventData: Map<String, Any>?, telemetryBus: TelemetryBus) {
        // Normalización de Inputs (Input Mapping)
        val payload = mutableMapOf<String, Any>(
            "origen" to node.path,
            "evento" to eventType,
            "memoria" to HashMap(node.memoryCamera)
        )
        eventData?.let { payload["datosEvento"] = it }

        telemetryBus.emit(payload)
    }
}

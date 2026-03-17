package com.engine.app

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.engine.core.Node
import java.io.File

object JsonParser {
    private val gson = Gson()

    // Deserializar Árbol Completo
    fun parseUiTree(jsonString: String): Node {
        val root = gson.fromJson(jsonString, Node::class.java)
        root.initDefaults() // Initialize missing fields
        return root
    }

    // Deserializar Parche (Deep Merge Delta)
    fun parsePatch(jsonString: String): Map<String, Any> {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(jsonString, type)
    }

    fun parseNodes(nodesListMap: List<Map<String, Any>>): List<Node> {
        val jsonString = gson.toJson(nodesListMap)
        val type = object : TypeToken<List<Node>>() {}.type
        val nodes: List<Node> = gson.fromJson(jsonString, type)
        nodes.forEach { it.initDefaults() }
        return nodes
    }

    fun loadFromFile(filePath: String): String {
        return File(filePath).readText()
    }
}

class MockTelemetryBus : com.engine.core.TelemetryBus {
    override fun emit(payload: Map<String, Any>) {
        println("[Telemetry Bus] Dispatched: $payload")
    }
}

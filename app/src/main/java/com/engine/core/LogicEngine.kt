package com.engine.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Basic Rule Engine that executes logic directives defined in JSON.
 */
object LogicEngine {

    // Evaluates "onEvent" arrays of actions defined in logicDirectives
    fun executeActions(node: Node, root: Node, eventName: String, coroutineScope: CoroutineScope, coreAlgorithms: CoreAlgorithms, onNavigate: ((String) -> Unit)? = null) {
        val directives = node.logicDirectives
        val events = directives["onEvent"] as? List<*> ?: return

        events.forEach { actionRaw ->
            val action = actionRaw as? Map<*, *> ?: return@forEach
            val trigger = action["event"] as? String ?: return@forEach

            if (trigger == eventName) {
                val operation = action["operation"] as? String ?: return@forEach
                val params = action["params"] as? Map<*, *> ?: emptyMap<Any, Any>()

                when (operation) {
                    "SET_STATE" -> {
                        // Modify memory locally across brother/sister nodes based on path
                        val targetId = params["targetId"] as? String
                        val key = params["key"] as? String
                        val value = params["value"]

                        if (targetId != null && key != null && value != null) {
                            // Find target node from actual root
                            val targetNode = findNodeById(root, targetId)
                            targetNode?.let {
                                it.memoryCamera[key] = value
                                // Trick to force recomposition manually (or let deep diffing do it on next cycle)
                                it.notifyStateChanged()
                            }
                        }
                    }
                    "NAVIGATE" -> {
                        val url = params["url"] as? String
                        if (url != null) {
                            onNavigate?.invoke(url)
                        }
                    }
                    "PRINT_LOG" -> {
                        val message = params["message"] as? String ?: "LOG"
                        println("[LogicEngine] $message")
                    }
                    "HTTP_POST" -> {
                        val urlString = params["url"] as? String
                        val payloadKeys = params["body"] as? List<*> ?: emptyList<Any>()

                        if (urlString != null) {
                            // Extract data from global memory camera based on requested keys
                            val payloadMap = mutableMapOf<String, Any>()

                            payloadKeys.forEach { keyRaw ->
                                val key = keyRaw as? String ?: return@forEach
                                // Simple global search for the value in memory cameras starting from the actual root
                                val value = findValueInMemory(root, key)
                                if (value != null) {
                                    payloadMap[key] = value
                                }
                            }

                            // Launch real HTTP POST in background
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val jsonPayload = com.google.gson.Gson().toJson(payloadMap)
                                    val url = java.net.URL(urlString)
                                    val connection = url.openConnection() as java.net.HttpURLConnection
                                    connection.requestMethod = "POST"
                                    connection.setRequestProperty("Content-Type", "application/json; utf-8")
                                    connection.setRequestProperty("Accept", "application/json")
                                    connection.doOutput = true

                                    connection.outputStream.use { os ->
                                        val input = jsonPayload.toByteArray(Charsets.UTF_8)
                                        os.write(input, 0, input.size)
                                    }

                                    val responseCode = connection.responseCode
                                    println("[LogicEngine] HTTP_POST a $urlString completado con código $responseCode. Payload enviado: $jsonPayload")

                                    // Optional: Trigger success/error actions defined in JSON
                                    if (responseCode in 200..299) {
                                        val onSuccess = action["onSuccess"] as? List<Map<String, Any>>
                                        if (onSuccess != null) {
                                            launch(Dispatchers.Main) {
                                                coreAlgorithms.executeActions(node, root, "success", onSuccess)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    println("[LogicEngine] Error en HTTP_POST: ${e.message}")
                                }
                            }
                        }
                    }
                    else -> println("[LogicEngine] Unknown operation: $operation")
                }
            }
        }
    }

    private fun findValueInMemory(root: Node, key: String): Any? {
        if (root.memoryCamera.containsKey(key)) return root.memoryCamera[key]
        for (child in root.reactiveChildren) {
            val found = findValueInMemory(child, key)
            if (found != null) return found
        }
        return null
    }


    private fun findNodeById(root: Node, id: String): Node? {
        if (root.id == id) return root
        for (child in root.reactiveChildren) {
            val found = findNodeById(child, id)
            if (found != null) return found
        }
        return null
    }
}

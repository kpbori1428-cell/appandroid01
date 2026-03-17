package com.engine.core

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf

data class Node(
    val id: String,
    val path: String,
    val type: String, // String representation of the native component identifier
    val instanceRule: String? = null, // Regla de Instancias: ej. lista.item:ins[4]
    var aestheticProperties: MutableMap<String, Any> = mutableMapOf(),
    var logicDirectives: MutableMap<String, Any> = mutableMapOf(),
    var children: List<Node> = emptyList(),
    var memoryCamera: MutableMap<String, Any> = mutableMapOf(),
    val baseConstraints: Map<String, Any> = emptyMap()
) {
    // Composables observe these reactive properties
    @Transient var reactiveAesthetics = mutableStateMapOf<String, Any>()
    @Transient var reactiveDirectives = mutableStateMapOf<String, Any>()
    @Transient var reactiveChildren = mutableStateListOf<Node>()

    // Garantiza que tras la deserialización de GSON, los objetos no queden nulos si no venían en el JSON
    // Y sincroniza las colecciones estáticas con las colecciones reactivas de Compose
    fun initDefaults() {
        if (this.aestheticProperties as Any? == null) this.aestheticProperties = mutableMapOf()
        if (this.logicDirectives as Any? == null) this.logicDirectives = mutableMapOf()
        if (this.children as Any? == null) this.children = emptyList()
        if (this.memoryCamera as Any? == null) this.memoryCamera = mutableMapOf()

        // Sync to Compose State
        reactiveAesthetics.clear()
        reactiveAesthetics.putAll(this.aestheticProperties)

        reactiveDirectives.clear()
        reactiveDirectives.putAll(this.logicDirectives)

        reactiveChildren.clear()
        reactiveChildren.addAll(this.children)

        this.children.forEach { it.initDefaults() }
    }

    // When the core algorithm merges data, it must also update the reactive map
    fun notifyStateChanged() {
        // Find keys that changed or were added
        for ((key, value) in this.aestheticProperties) {
            if (reactiveAesthetics[key] != value) {
                reactiveAesthetics[key] = value
            }
        }

        // Find keys that were removed
        val removedKeys = reactiveAesthetics.keys.filter { !this.aestheticProperties.containsKey(it) }
        removedKeys.forEach { reactiveAesthetics.remove(it) }

        // Mismo proceso para los hijos si hay parches estructurales
        reactiveChildren.clear()
        reactiveChildren.addAll(this.children)

        // Propagar a los hijos
        this.children.forEach { it.notifyStateChanged() }
    }
}

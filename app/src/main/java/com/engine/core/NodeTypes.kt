package com.engine.core

object NodeTypes {
    // Basic types
    const val CONTAINER = "CONTAINER"
    const val TEXT = "TEXT"
    const val IMAGE = "IMAGE"
    const val INPUT = "INPUT"
    const val CHECKBOX = "CHECKBOX"
    const val DROPDOWN = "DROPDOWN"

    // Universal Base Types and behaviors
    const val RECYCLER_VIEW = "RECYCLER_VIEW"
    const val MEDIA_CAPTURE = "MEDIA_CAPTURE"
    const val MAP_VIEW = "MAP_VIEW"
    const val ACTION_EMITTER = "ACTION_EMITTER"
    const val WEB_VIEW = "WEB_VIEW" // Useful for loading any unsupported complex content
}

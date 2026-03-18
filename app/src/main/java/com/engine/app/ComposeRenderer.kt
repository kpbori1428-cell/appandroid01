package com.engine.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import coil.compose.AsyncImage
import com.engine.core.Node
import com.engine.core.NodeTypes
import com.engine.core.LogicEngine

@Composable
fun RenderNode(node: Node, onAction: (sourceNode: Node, trigger: String, params: Any?) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    // Escuchar el estado reactivo del nodo en lugar del mapa estático
    val aesthetics = node.reactiveAesthetics

    // Modifier mapping
    var modifier = Modifier.fillMaxWidth()

    // Padding
    val padding = aesthetics["padding"] as? Double
    if (padding != null) {
        modifier = modifier.padding(padding.dp)
    }

    // Padding Bottom
    val paddingBottom = aesthetics["paddingBottom"] as? Double
    if (paddingBottom != null) {
        modifier = modifier.padding(bottom = paddingBottom.dp)
    }

    // Background Color
    val bgColorStr = aesthetics["backgroundColor"] as? String
    if (bgColorStr != null && node.type != NodeTypes.ACTION_EMITTER) {
        try {
            modifier = modifier.background(Color(android.graphics.Color.parseColor(bgColorStr)))
        } catch (e: Exception) {
            // Ignorar color inválido
        }
    }

    when (node.type) {
        NodeTypes.CONTAINER -> {
            Column(modifier = modifier) {
                // Observamos los hijos reactivos para redibujar si se añaden/quitan nodos remotos
                for (child in node.reactiveChildren) {
                    RenderNode(node = child, onAction)
                }
            }
        }
        NodeTypes.RECYCLER_VIEW -> {
            // Instanciación procedimental optimizada (Culling Lógico de Jetpack Compose)
            LazyColumn(modifier = modifier) {
                items(node.reactiveChildren) { child ->
                    RenderNode(node = child, onAction)
                }
            }
        }
        NodeTypes.IMAGE -> {
            val url = aesthetics["url"] as? String ?: ""
            val height = aesthetics["height"] as? Double

            var imgModifier = modifier
            if (height != null) {
                imgModifier = imgModifier.height(height.dp)
            }

            // Descarga asíncrona de imágenes
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imgModifier
            )
        }
        NodeTypes.TEXT -> {
            val textStr = aesthetics["text"] as? String ?: ""
            val fontSizeStr = aesthetics["fontSize"] as? Double ?: 14.0
            val textColorStr = aesthetics["textColor"] as? String ?: "#000000"

            var textColor = Color.Black
            try {
                textColor = Color(android.graphics.Color.parseColor(textColorStr))
            } catch (e: Exception) { }

            Text(
                text = textStr,
                fontSize = fontSizeStr.sp,
                color = textColor,
                modifier = modifier
            )
        }
        NodeTypes.CHECKBOX -> {
            val labelStr = aesthetics["label"] as? String ?: ""
            var checked by remember { mutableStateOf(node.memoryCamera["value"] as? Boolean ?: false) }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        node.memoryCamera["value"] = it
                    }
                )
                Text(text = labelStr)
            }
        }
        NodeTypes.WEB_VIEW -> {
            val urlStr = aesthetics["url"] as? String ?: "https://google.com"
            val height = aesthetics["height"] as? Double

            var webModifier = modifier
            if (height != null) {
                webModifier = webModifier.height(height.dp)
            }

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        loadUrl(urlStr)
                    }
                },
                update = { webView ->
                    if (webView.url != urlStr) {
                        webView.loadUrl(urlStr)
                    }
                },
                modifier = webModifier
            )
        }
        NodeTypes.INPUT -> {
            val placeholder = aesthetics["placeholder"] as? String ?: ""
            val isPassword = node.logicDirectives["isPassword"] as? Boolean == true
            val bindKey = node.logicDirectives["bindKey"] as? String ?: "value"

            // Memory Camera Simulation via Local State
            var text by remember { mutableStateOf(node.memoryCamera[bindKey] as? String ?: "") }

            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    // Save to Engine Memory Camera using a specific key if requested
                    node.memoryCamera[bindKey] = it
                },
                label = { Text(placeholder) },
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = modifier
            )
        }
        NodeTypes.ACTION_EMITTER -> {
            val labelStr = aesthetics["label"] as? String ?: "Button"

            var buttonColor = Color.Blue
            val btnColorStr = aesthetics["backgroundColor"] as? String
            if (btnColorStr != null) {
                 try {
                     buttonColor = Color(android.graphics.Color.parseColor(btnColorStr))
                 } catch (e: Exception) { }
            }

            Button(
                onClick = {
                    // Ejecuta las directivas V4 en Kotlin Nativo
                    onAction(node, "click", node.logicDirectives["action"])
                    println("Valores locales en memoria del botón: ${node.memoryCamera}")
                },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = modifier
            ) {
                Text(text = labelStr, color = Color.White)
            }
        }
        else -> {
            // Fallback validation: Empty space cube
            Box(modifier = Modifier)
        }
    }
}

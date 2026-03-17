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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.engine.core.Node
import com.engine.core.NodeTypes

@Composable
fun RenderNode(node: Node) {
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
                    RenderNode(node = child)
                }
            }
        }
        NodeTypes.RECYCLER_VIEW -> {
            // Instanciación procedimental optimizada (Culling Lógico de Jetpack Compose)
            LazyColumn(modifier = modifier) {
                items(node.reactiveChildren) { child ->
                    RenderNode(node = child)
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
        NodeTypes.INPUT -> {
            val placeholder = aesthetics["placeholder"] as? String ?: ""
            val isPassword = node.logicDirectives["isPassword"] as? Boolean == true

            // Memory Camera Simulation via Local State
            var text by remember { mutableStateOf(node.memoryCamera["value"] as? String ?: "") }

            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    // Save to Engine Memory Camera
                    node.memoryCamera["value"] = it
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
                    // Logic Directive Action Emitter simulation
                    println("Boton Presionado: " + node.logicDirectives["action"])
                    println("Valores en memoria: ${node.memoryCamera}") // Esto imprimiría en logcat
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

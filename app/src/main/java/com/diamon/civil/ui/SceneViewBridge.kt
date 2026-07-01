package com.diamon.civil.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberModelInstance

fun setSceneViewContent(composeView: ComposeView, modelPath: String) {
    composeView.setContent {
        SceneViewWrapper(modelPath)
    }
}

@Composable
fun SceneViewWrapper(modelPath: String) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, modelPath)
    
    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader
    ) {
        modelInstance?.let {
            ModelNode(modelInstance = it)
        }
    }
}

# Guía de Implementación: SceneView v0.10.0 (Android Java)

Esta guía resume los conceptos clave para implementar SceneView v0.10.0 en proyectos Android utilizando Java, basada en la estructura del motor de renderizado Filament subyacente.

## 1. Conceptos Fundamentales

SceneView utiliza un grafo de escena donde todo objeto es un `Node`.

*   **Scene:** Contenedor raíz de todos los nodos.
*   **ModelNode:** Representa un modelo 3D (glTF/GLB).
*   **LightNode:** Representa una fuente de luz (necesaria para materiales PBR).
*   **CameraNode:** Define la perspectiva del renderizado.

## 2. Configuración Básica (MainActivity)

Para inicializar la escena en un `MainActivity` (con ViewBinding):

```java
// Obtener el engine de Filament desde la vista
Engine engine = binding.sceneView.getEngine();

// 1. Configurar la Cámara
if (binding.sceneView.getCameraNode() != null) {
    binding.sceneView.getCameraNode().setNearClipPlane(0.1f);
    binding.sceneView.getCameraNode().setFarClipPlane(1000.0f);
    
    // Posicionar la cámara
    binding.sceneView.getCameraNode().setPosition(new Float3(0.0f, 0.0f, 5.0f));
    binding.sceneView.getCameraNode().lookAt(new Float3(0.0f, 0.0f, 0.0f), new Float3(0.0f, 1.0f, 0.0f), true);
}

// 2. Configurar Iluminación (Crítico para modelos PBR)
// Filament requiere al menos una luz para materiales PBR
LightNode lightNode = new LightNode(engine);
binding.sceneView.addChild(lightNode);
```

## 3. Carga de Modelos

Los modelos deben ser `.glb` o `.gltf`. Se cargan mediante `ModelNode`:

```java
ModelNode modelNode = new ModelNode(
    binding.sceneView.getEngine(),
    "models/test_beam.glb", // Ruta en assets
    true,                   // autoScale
    1.0f,                   // scale
    new Float3(0.0f, 0.0f, 0.0f), // posición
    null,                   // material
    null                    // placeholder
);

binding.sceneView.addChild(modelNode);
modelNode.centerModel(new Float3(0.0f, 0.0f, 0.0f));
```

## 4. Temas Avanzados y Rendimiento

### 4.1. Iluminación Basada en Imágenes (IBL)
Para que los modelos PBR se vean realistas, Filament requiere un mapa de entorno (IBL). Sin IBL, los materiales metálicos o brillantes no mostrarán reflejos, aunque tengan luz directa.
*   Se debe cargar un entorno `.hdr` o `.ktx` y configurarlo en el `Engine` de Filament.
*   `SceneView` permite añadir entornos complejos (`EnvironmentNode`) para mejorar el realismo.

### 4.2. Threading y Asincronía
*   La carga de modelos (especialmente modelos pesados) **debe** realizarse fuera del hilo principal para evitar bloqueos en la interfaz (`Application Not Responding`).
*   Utilizar `ExecutorService` o Coroutines para la carga y manipular la escena (añadir nodos) únicamente en el hilo principal (`runOnUiThread`).

## 5. Notas Técnicas Importantes

*   **Iluminación:** Sin un `LightNode` añadido a la escena, los modelos con materiales PBR (Physically Based Rendering) se renderizarán completamente negros.
*   **Compatibilidad:** Requiere `minSdkVersion 24` (Android 7.0+).
*   **Filament Engine:** La mayor parte de la configuración avanzada (luces, entornos) requiere interactuar con el `Engine` de Filament obtenido a través de `binding.sceneView.getEngine()`.
*   **Depuración:** Si el modelo no aparece, verificar:
    1. Que el archivo `.glb` exista en `app/src/main/assets/`.
    2. Que la cámara esté mirando hacia el origen `(0,0,0)` y el modelo esté en esa posición.
    3. Que al menos haya un `LightNode` en la escena.
    4. Revisar Logcat buscando errores provenientes de `Filament` o `SceneView`.

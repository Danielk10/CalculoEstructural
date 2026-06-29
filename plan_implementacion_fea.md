# Plan de implementación — Structural FEA Advanced

**STRUCTURAL FEA ADVANCED · ANDROID NDK**
Motor CalculiX compilado · CalculiX 2.23 + SPOOLES + ARPACK + OpenBLAS + Gmsh + OCCT

---

## Progreso del Proyecto
- **Progreso total estimado:** 55% (basado en items completados vs pendientes)
- **Completados:** 18 ítems
- **En progreso:** 1 ítem
- **Pendientes:** 12 ítems

---

## Fase 0 + Fase 1 — Completados

- Navigation Drawer + tabs MODEL/TERMINAL/VIEWER
- SceneView v0.10.0 integrado (Filament)
- FRD → GLB converter en C++/tinygltf (TET4 + TRIA3)
- Heatmap Von Mises (azul a rojo) en vértices
- NativeFeaCore JNI wrapper completo
- CalculixRunner (ccx via JNI y ProcessBuilder)
- ProjectStore: serialización JSON del estado
- InpEnricher: inyección de propiedades en mallas Gmsh
- Unit tests NDK en Linux: test_analysis_model, test_calculix_runner, test_project_store
- InpEnricherTest JUnit validado
- CalculixRunner.cpp: fix buffer PATH_MAX compilado
- Alineación 16 KB verificada en todos los .so (Android 15 listo)
- Dependencias dinámicas mapeadas en jniLibs/arm64-v8a
- Symlink libz.so.1 → /system/lib64/libz.so implementado
- **A1. Pipeline CAD completo:** GmshRunner, MshToInpConverter y flujo en MainActivity integrados.
- **A2. Structural Result Mapping:** InpGenerator emite *SECTION PRINT y DatParser extrae fuerzas N, V, M.

---

## Etapa A: Cerrar Fase 1 — Motor completo
**ETA:** Inmediato | **Progreso:** 100%

### A3. Integration Testing en dispositivo ARM64
- **Estado:** Próximo paso (CRÍTICO)
- **Archivos:** Ninguno
- **Descripción:** Todo fue verificado en Linux local y mediante compilación exitosa. Hay que confirmar que la cadena completa funciona en hardware real ARM64.
- **Tareas:**
  - [ ] Generar APK Release firmado
  - [ ] Caso A — Modo Solid: cargar STL de cubo, mallar con Gmsh, resolver con CalculiX, ver heatmap Von Mises en SceneView
  - [ ] Caso B — Modo Frame: dibujar viga biapoyada con carga puntual central, resolver, verificar valores de M y V en el .dat
  - [ ] Validar el symlink libz.so.1 → /system/lib64/libz.so en AssetHelper (verificar crash Tcl)
  - [ ] Si CalculiX crashea por ruta Termux hardcodeada: ejecutar patchelf --replace-needed en el binario
- **Nota:** CRÍTICO: revisar ALINEACION_Y_DEPENDENCIAS.md — la ruta absoluta de Termux en libCalculiX.so puede causar crash al arranque.

---

## Etapa B: Editor Estructural — Modo SAP2000
**ETA:** 4-6 semanas | **Progreso:** 0%

### B1. Custom OpenGL ES Renderer — Lienzo de pórticos
- **Estado:** Pendiente
- **Archivos:** `FrameRenderer.java`, `FrameGLSurfaceView.java`, `GridShader.glsl`
- **Descripción:** El núcleo gráfico del Modo Civil. El usuario dibuja su estructura directamente en pantalla.
- **Tareas:**
  - [ ] Crear FrameGLSurfaceView extends GLSurfaceView con contexto OpenGL ES 3.0
  - [ ] Renderizar cuadrícula (grid) con líneas finas — coordenadas del mundo en metros
  - [ ] Gestos: tap en vacío = crear Nodo, tap+drag Nodo→Nodo = crear Elemento de barra
  - [ ] Renderizar nodos como círculos (shaders), barras como líneas coloreadas (viga=azul, columna=rojo)
  - [ ] Renderizar apoyos: triángulo para articulado, rectángulo para empotrado
  - [ ] Renderizar cargas: flecha con magnitud flotante sobre el nodo
- **Nota:** No usar LibGDX — secuestra la Activity. GLSurfaceView convive perfectamente con botones nativos de Android alrededor.

### B2. Biblioteca de Secciones Transversales
- **Estado:** Pendiente
- **Archivos:** `assets/sections.json`, `SectionLibrary.java`, `SectionPickerDialog.java`
- **Descripción:** Base de datos de perfiles comerciales para alimentar la tarjeta *BEAM SECTION de CalculiX.
- **Tareas:**
  - [ ] Crear sections.json con perfiles: W8×31, W12×50, IPE 200, IPE 300, IPE 400, HSS 100×100×6, Tubo circular Ø200×10, Rectangular 300×400
  - [ ] Cada perfil: nombre, tipo (I/rectangular/circular/tubo), h, b, tf, tw, A, Iy, Iz, J
  - [ ] SectionLibrary.java: cargar y consultar sections.json
  - [ ] SectionPickerDialog: diálogo RecyclerView para seleccionar perfil al crear elemento
  - [ ] Almacenar la sección elegida en el modelo de datos FrameElement
- **Nota:** Los valores de Iy, Iz, J son necesarios para BEAM SECTION GENERAL. Calcularlos de las dimensiones si no están en el JSON.

### B3. Generador .inp para Pórticos (Elementos B32)
- **Estado:** Pendiente
- **Archivos:** `StructuralInpGenerator.java`
- **Descripción:** Traducir el modelo gráfico del lienzo al formato CalculiX para vigas cuadráticas B32.
- **Tareas:**
  - [ ] Para cada barra del modelo: calcular nodo intermedio automáticamente = ( (x1+x2)/2, (y1+y2)/2, (z1+z2)/2 )
  - [ ] Emitir *ELEMENT, TYPE=B32, ELSET=BEAMS con los 3 nodos (extremo1, intermedio, extremo2)
  - [ ] Por cada sección diferente: emitir *BEAM SECTION, ELSET=E_n, SECTION=GENERAL con los 7 valores de sección
  - [ ] Emitir *MATERIAL, NAME=... y *ELASTIC con E y ν
  - [ ] Emitir apoyos como *BOUNDARY (nodo, DoF_inicio, DoF_fin, valor=0)
  - [ ] Emitir cargas como *CLOAD (nodo, DoF, magnitud)
  - [ ] Al final del *STEP: *SECTION PRINT, ELSET=BEAMS y *NODE PRINT, NSET=ALL
- **Nota:** B32 requiere obligatoriamente el nodo intermedio en la posición 2 del elemento. Sin él, CalculiX abortará.

### B4. Diagram Engine — BMD / SFD / AFD
- **Estado:** Pendiente
- **Archivos:** `DiagramRenderer.java`, `DatParser.java`
- **Descripción:** Visualización clásica de ingeniería civil: diagramas de Momento Flector, Cortante y Fuerza Axial.
- **Tareas:**
  - [ ] Completar DatParser.java (iniciado en A2) para extraer todos los valores por elemento
  - [ ] Calcular escala automática: valor_máximo_absoluto → tamaño visual del pico del diagrama
  - [ ] Para BMD: dibujar polígono perpendicular a cada barra en Canvas de Android (en overlay sobre el GLSurfaceView)
  - [ ] Para SFD y AFD: ídem con colores distintos — SFD=azul, BMD=rojo, AFD=verde
  - [ ] Mostrar etiquetas de valores en los picos de los diagramas
  - [ ] Toggle UI para alternar entre diagrama deformado / BMD / SFD / AFD
- **Nota:** Los diagramas se dibujan en Android Canvas 2D, no en OpenGL. Superponer un Canvas transparente sobre el GLSurfaceView.

---

## Etapa C: Editor 3D Sólidos — Modo Abaqus
**ETA:** 3-4 semanas | **Progreso:** 0%

### C1. CAD Primitivas — Box, Cylinder, Sphere vía OCCT
- **Estado:** Pendiente
- **Archivos:** `OcctPrimitivesJNI.cpp`, `OcctPrimitivesJNI.java`, `SolidEditorFragment.xml`
- **Descripción:** OpenCASCADE ya está embebida en las libTK*.so. Exponer creación de sólidos básicos vía JNI.
- **Tareas:**
  - [ ] OcctPrimitivesJNI.cpp: implementar createBox(l,w,h), createCylinder(r,h), createSphere(r) usando BRepPrimAPI_Make*
  - [ ] Exportar cada sólido como BREP a archivo temporal, luego pasarlo a Gmsh para mallar
  - [ ] SolidEditorFragment: botones flotantes '+Box', '+Cylinder', '+Sphere' con diálogo de dimensiones
  - [ ] Mostrar la malla resultante en SceneView (ya funciona — reutilizar el pipeline GLB)
- **Nota:** No implementar el visualizador OCCT nativo — usar siempre Gmsh→FRD→GLB→SceneView. Es el camino ya probado.

### C2. Operaciones Booleanas vía OCCT
- **Estado:** Pendiente
- **Archivos:** `OcctBooleanJNI.cpp`
- **Descripción:** Combinar sólidos mediante unión, corte e intersección.
- **Tareas:**
  - [ ] OcctBooleanJNI.cpp: implementar fuseShapes(a.brep, b.brep)→out.brep usando BRepAlgoAPI_Fuse
  - [ ] Implementar cutShapes (BRepAlgoAPI_Cut) e intersectShapes (BRepAlgoAPI_Common)
  - [ ] UI: modo 'selección de 2 sólidos' + menú contextual Union/Cut/Intersect
  - [ ] Re-mallar el resultado con Gmsh y actualizar la vista SceneView
- **Nota:** Si OCCT no está disponible como JNI en esta fase, postergar y priorizar C3 y C4.

### C3. Ray-Casting — Selección táctil de caras
- **Estado:** Pendiente
- **Archivos:** `FaceSelector.java`, `SolidEditorFragment.java`
- **Descripción:** Permitir al usuario tocar el modelo 3D para seleccionar una cara y aplicarle condiciones.
- **Tareas:**
  - [ ] Capturar MotionEvent.ACTION_DOWN en la vista de SceneView
  - [ ] Calcular rayo desde la posición de cámara a través del píxel tocado
  - [ ] Intersectar el rayo con las caras del modelo (bounding boxes de los triángulos del GLB)
  - [ ] Resaltar la cara seleccionada (cambiar color del material en SceneView)
  - [ ] Mostrar BottomSheet con opciones: 'Aplicar presión', 'Empotramiento', 'Refinar malla aquí'
  - [ ] Almacenar la selección en el modelo de datos como FaceCondition
- **Nota:** SceneView no expone ray-casting directamente. Implementar contra los triángulos del GLB parseado.

### C4. Material Library UI
- **Estado:** Pendiente
- **Archivos:** `MaterialDatabase.java`, `assets/materials.json`
- **Descripción:** Base de datos de materiales para el Modo Abaqus. InpEnricher ya inyecta propiedades — solo conectar la UI.
- **Tareas:**
  - [ ] Crear materials.json: Acero A36 (E=200GPa, ν=0.3, ρ=7850, σy=250MPa), Aluminio 6061-T6, Concreto 25MPa, Titanio Ti-6Al-4V, Madera (ortótropo)
  - [ ] MaterialDatabase.java: cargar y consultar materials.json
  - [ ] MaterialPickerDialog: diálogo con tarjetas de material mostrando propiedades clave
  - [ ] Conectar la selección de material con InpEnricher.java (ya implementado)
- **Nota:** InpEnricher.java ya está unit-tested. Esta tarea es puro Frontend.

### C5. Mesh Controls — Densidad y refinamiento local
- **Estado:** Pendiente
- **Archivos:** `GmshRunner.java`
- **Descripción:** Dar al usuario control sobre la calidad de la malla sin exponerle la CLI de Gmsh.
- **Tareas:**
  - [ ] Agregar parámetro meshDensity (1-5) a GmshRunner
  - [ ] Mapear el slider a: density=1 → -clmax 50, density=3 → -clmax 20, density=5 → -clmax 5
  - [ ] UI: slider 'Coarse ←→ Fine' con preview del número estimado de elementos
  - [ ] Refinamiento local: si una cara fue marcada en C3, emitir Mesh.Field.setNumber para esa zona en el script de Gmsh
- **Nota:** Valores de -clmax en mm. Advertir al usuario si estima más de 10k elementos en un teléfono de gama baja.

---

## Etapa D: Publicación — Play Store
**ETA:** 3-4 semanas | **Progreso:** 0%

### D1. INP Importer — Compatibilidad Abaqus
- **Estado:** Pendiente
- **Archivos:** `AbaqusInpImporter.java`
- **Descripción:** Permitir importar modelos .inp hechos en Abaqus o CalculiX directamente en la app.
- **Tareas:**
  - [ ] AbaqusInpImporter.java: parser de tarjetas *NODE, *ELEMENT, *MATERIAL, *ELASTIC, *BOUNDARY, *CLOAD, *STEP
  - [ ] Reconstruir el AnalysisModel C++ desde el .inp importado
  - [ ] Detectar automáticamente si el modelo es Frame (B31/B32) o Solid (C3D4/C3D10) y abrir el editor correspondiente
  - [ ] File-picker para archivos .inp desde almacenamiento externo
- **Nota:** Dado que CalculiX usa la misma sintaxis que Abaqus, el mismo parser funciona para ambos. Ignorar tarjetas desconocidas con un warning.

### D2. PDF Reporting
- **Estado:** Pendiente
- **Archivos:** `ReportGenerator.java`, `build.gradle`
- **Descripción:** Generar un reporte profesional descargable con el modelo, resultados y screenshots.
- **Tareas:**
  - [ ] Añadir dependencia iText7 Community (GPL-compatible) al build.gradle
  - [ ] Capturar screenshot del modelo 3D: sceneView.draw(canvas) o PixelCopy
  - [ ] Generar tabla de resultados: nodos con máx desplazamiento, elementos con máx Von Mises o máx Momento
  - [ ] ReportGenerator.java: componer PDF con logo de la app, datos del análisis, imagen del modelo, tabla de resultados
  - [ ] Compartir PDF mediante FileProvider + Intent.ACTION_SEND
- **Nota:** iText7 Community usa la licencia AGPL, compatible con GPL. Verificar que no rompe las obligaciones de distribución.

### D3. Performance — Threading y feedback al usuario
- **Estado:** Pendiente
- **Archivos:** `GmshRunner.java`, `CalculixRunner.java`, `DatParser.java`
- **Descripción:** Mover todo el trabajo pesado fuera del hilo principal para no congelar la UI.
- **Tareas:**
  - [ ] Migrar GmshRunner a Kotlin Coroutine (Dispatchers.IO) o AsyncTask deprecado → ExecutorService
  - [ ] Mostrar ProgressBar indeterminada + texto 'Mallando...' / 'Calculando...' / 'Parseando resultados...' durante cada etapa
  - [ ] Migrar DatParser a background thread
  - [ ] Para mallas > 10k elementos: mostrar estimación de tiempo basada en benchmark del dispositivo
  - [ ] Botón 'Cancelar' que envíe SIGTERM al proceso ccx/gmsh si el usuario aborta
- **Nota:** CalculixRunner ya usa JNI — revisar si la ejecución nativa bloquea el hilo JNI. Si es así, lanzar en nuevo Thread antes del JNI call.

### D4. Play Store — Publicación
- **Estado:** Pendiente
- **Archivos:** `app/build.gradle`, `fastlane/`
- **Descripción:** Preparar la app para distribución pública cumpliendo con GPL y políticas de Google.
- **Tareas:**
  - [ ] Generar APK/AAB Release firmado con clave de producción
  - [ ] Preparar listing: screenshots Modo Frame + Modo Solid, icono final, descripción ES/EN
  - [ ] Declarar que el código fuente está disponible en GitHub (obligación GPL)
  - [ ] Configurar monetización: app base gratis (Frame 2D), compra interna para Frame 3D + Solid Mode + PDF
  - [ ] Subir a Play Console — track Pruebas Internas → Alpha → Beta → Producción
- **Nota:** La GPL permite monetización con anuncios y compras internas. Solo exige que el APK y el código fuente estén disponibles.

---

## Puntos críticos antes de publicar
1. La **ruta absoluta de Termux** hardcodeada en `libCalculiX.so` puede causar crash al arranque en dispositivos reales. Verificar con `patchelf` si ocurre.
2. SceneView v0.10.0 requiere `minSdkVersion=24` (Android 7). Confirmar que el `build.gradle` lo tiene.
3. La GPL exige publicar el código fuente completo junto con el APK. GitHub ya lo cumple — incluir la URL en el listing de Play Store.

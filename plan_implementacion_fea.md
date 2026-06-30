# Plan de implementación — Structural FEA Advanced

**STRUCTURAL FEA ADVANCED · ANDROID NDK**
Motor CalculiX compilado · CalculiX 2.23 + SPOOLES + ARPACK + OpenBLAS + Gmsh + OCCT

---

## Progreso del Proyecto
- **Progreso total estimado:** 85% (basado en items completados vs pendientes)
- **Completados:** 27 ítems
- **En progreso:** 1 ítem
- **Pendientes:** 3 ítems

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
- **A3. Integration Testing en dispositivo ARM64:** Verificado en hardware real. App estable.

---

## Etapa A: Cerrar Fase 1 — Motor completo
**ETA:** Finalizado | **Progreso:** 100%

---

## Etapa B: Editor Estructural — Modo SAP2000
**ETA:** 1-2 semanas | **Progreso:** 80%

### B1. Custom OpenGL ES Renderer — Lienzo de pórticos
- **Estado:** COMPLETADO (Base)
- **Archivos:** `FrameRenderer.java`, `FrameGLSurfaceView.java`
- **Descripción:** Implementado renderizado de cuadrícula 3D base y visibilidad integrada en MainActivity.

### B2. Biblioteca de Secciones Transversales
- **Estado:** COMPLETADO
- **Archivos:** `assets/sections.json`, `SectionLibrary.java`
- **Descripción:** Base de datos de perfiles comerciales (W, IPE, HSS, etc.) implementada.

### B3. Generador .inp para Pórticos (Elementos B32)
- **Estado:** COMPLETADO
- **Archivos:** `StructuralInpGenerator.java`
- **Descripción:** Traductor de modelo estructural a CalculiX con elementos viga cuadráticos B32.

### B4. Diagram Engine — BMD / SFD / AFD
- **Estado:** COMPLETADO
- **Archivos:** `DiagramView.java`, `DatParser.java`
- **Descripción:** Visualización de diagramas de Momento Flector, Cortante y Axial sobre el modelo.
- **Tareas:**
  - [x] Completar DatParser.java para extraer valores por elemento.
  - [x] Implementar DiagramView usando Android Canvas 2D.
  - [x] Integrar botones BMD/SFD/AFD/OFF en la interfaz estructural.
  - [x] Fix error `*SECTION PRINT` (missing NAME parameter) en el generador de INP.

---

## Etapa C: Editor 3D Sólidos — Modo Abaqus
**ETA:** 1-2 semanas | **Progreso:** 60%

### C1. CAD Primitivas — Box, Cylinder, Sphere vía OCCT
- **Estado:** COMPLETADO
- **Archivos:** `OcctPrimitivesJNI.cpp`, `OcctPrimitivesJNI.java`, `MainActivity.java`
- **Descripción:** Creación de sólidos básicos (Caja, Cilindro, Esfera) integrada vía JNI con OpenCASCADE.

### C2. Operaciones Booleanas vía OCCT
- **Estado:** COMPLETADO
- **Archivos:** `OcctBooleanJNI.cpp`, `OcctBooleanJNI.java`
- **Descripción:** Combinar sólidos mediante unión (FUSE) y corte (CUT) implementado vía JNI.
- **Tareas:**
  - [x] OcctBooleanJNI.cpp: implementar fuse e intersect usando BRepAlgoAPI.
  - [x] UI: botones 'FUSE' y 'CUT' integrados en el módulo 3D.
  - [x] Re-mallar el resultado con Gmsh y actualizar la vista SceneView automáticamente.

### C3. Ray-Casting — Selección táctil de caras
- **Estado:** Pendiente
- **Archivos:** `FaceSelector.java`, `SolidEditorFragment.java`
- **Descripción:** Permitir al usuario tocar el modelo 3D para seleccionar una cara y aplicarle condiciones.

### C4. Material Library UI
- **Estado:** COMPLETADO
- **Archivos:** `MaterialDatabase.java`, `assets/materials.json`
- **Descripción:** Biblioteca estándar de materiales (Acero, Aluminio, Concreto, etc.) implementada.

---

## Etapa D: Publicación — Play Store
**ETA:** 2-3 semanas | **Progreso:** 0%
...

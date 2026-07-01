# Master Implementation Plan: Structural FEA Suite

This document outlines the strategic roadmap for developing a dual-pipeline engineering platform. The app converges into **CalculiX** as the unified solver while maintaining distinct pre-processing and post-processing flows for **3D Solid Analysis** and **Structural Analysis**.

---

## 🏛️ Architecture Overview

The system is built on a shared C++ NDK core that handles heavy computation, parsing, and geometry logic, with a Kotlin-based UI layer.

### Two Pipelines, One Solver
1.  **3D Solid Analysis Pipeline**: CAD (STEP/IGES/BREP) ➔ OpenCASCADE ➔ Gmsh ➔ CalculiX (.inp) ➔ glTF ➔ SceneView.
2.  **Structural Analysis Pipeline**: Structural Model (Nodes/Beams/Shells) ➔ Internal Model ➔ CalculiX (.inp) ➔ Results ➔ OpenGL ES Renderer (Diagrams).

---

## 📊 Phase 0: UI Infrastructure & Core Navigation (COMPLETED)
- [x] **Modular Navigation**: Implementation of Navigation Drawer to switch between modules.
- [x] **Layout Separation**: Dedicated UI for **3D Solid Analysis** and **Structural Analysis**.
- [x] **Shared Terminal**: Unified command-line interface for direct binary execution.
- [x] **Hybrid 3D View**: Sub-tab system in **3D Solid Analysis** for Parameters and SceneView.
- [x] **Functional Prototypes**: Initial input areas for both modules implemented.

---

## ⚙️ Phase 1: Shared NDK Core & Solver Pipeline
*Goal: Establish the end-to-end flow from input to results.*

### 1.1 Common Core (JNI/C++)
- [x] **`AnalysisModel`**: Define unified C++ data structures for nodes, elements, and materials.
- [x] **`toInpString()`**: Logic to export internal model to CalculiX `.inp` format.
- [x] **`CalculixRunner`**: Robust JNI wrapper for `ccx` execution and job management (Implemented in C++ and exposed via NativeFeaCore JNI).
- [x] **`FrdConverter`**: C++ logic using **tinygltf** to convert `.frd` to colored glTF/GLB (Already implemented and tested).
- [x] **`ProjectStore`**: JSON/Binary serialization of the project state.

### 1.2 3D Solid Analysis Pipeline
- [x] **`CAD Pipeline`**: CAD (STEP/IGES/BREP) processing via Gmsh binary execution (Java). (Implemented and integrated in MainActivity)
- [x] **`InpEnricher`**: Logic to inject material properties and BCs into Gmsh-generated meshes (Java) (Implemented and unit tested).
- [x] **`Visual Conversion`**: C++ logic using **tinygltf** to convert `.frd` to colored glTF/GLB (Already implemented and tested).

### 1.3 Structural Analysis Pipeline
- [x] **Structural Inp Export**: Initial implementation for nodal and elemental data.
- [x] **Structural Result Mapping**: Converting nodal results into member forces (N, V, M) for diagrams. (Implemented via DatParser)

---

## 🧊 Phase 2: 3D Solid Analysis - CAD & Mesh Editor (COMPLETED)
*Goal: Allow geometry creation and manipulation within the app.*

- [x] **CAD Primitives**: Create Box, Cylinder, Sphere via OCCT (Implemented via OcctPrimitivesJNI).
- [x] **Boolean Engine**: Union (Fuse), Cut, and Intersection operations (Implemented via OcctBooleanJNI).
- [x] **Mesh Controls**: Local refinement and global mesh density parameters (Integrated in MainActivity via GmshRunner).
- [x] **Material Library**: Predefined material property management (MaterialDatabase.java + materials.json).

---

## 📐 Phase 3: Structural Analysis - Structural Editor (SAP-style) (COMPLETED)
*Goal: Implement a dedicated high-performance 2D/3D structural editor.*

- [x] **Custom OpenGL ES Renderer**: High-performance drawing of wireframes, nodes, and beams (FrameRenderer.java).
- [x] **Editing Tools**: Grid rendering and interactive node/beam creation (Integrated in FrameRenderer).
- [x] **Entity Management**: Node coordinates and element connectivity (StructuralModel.java).
- [x] **Diagram Engine**: Visualizing Bending Moment, Shear Force, and Axial diagrams (DiagramView.java + DatParser.java).

---

## 🚀 Phase 4: Advanced Integration & Optimization (COMPLETED)
*Goal: Polish, cross-module support, and performance.*

- [x] **Performance**: Multithreading for meshing and result parsing (ExecutorService in MainActivity).
- [x] **Abaqus .inp Importer**: High-fidelity Abaqus format compatibility (AbaqusInpImporter.java).
- [x] **Reporting**: Automated PDF generation with simulation data and result tables (ReportGenerator.java).
- [x] **Mixed Modeling**: Support for models containing both Solids and Structural elements (Implemented in AnalysisModel.cpp).
- [x] **Advanced Picking**: High-fidelity ray-casting for face/node selection (Implemented via SceneViewBridge + OnHitListener).


---

## 🧪 Testing & Validation Strategy

### Local-First C++ Verification
To ensure the robustness of the FEA core, all new C++ logic (Parsers, Writers, Models) must be verified in the local Linux environment before NDK integration:
1.  **Unit Testing**: Create standalone `main.cpp` drivers for NDK components.
2.  **Solver Validation**: Execute the local `ccx` binary with generated `.inp` files.
3.  **Result Verification**: Compare local outputs with expected engineering results.

---

## 🛠️ Technological Stack Summary
| Category | Tools | Status |
|---|---|---|
| **CAD/Geometry** | OpenCASCADE (OCCT) | Integrated (JNI) |
| **Meshing** | Gmsh | Integrated (Binary + Runner) |
| **Solver** | CalculiX (ccx) | Integrated (via Java/NDK) |
| **Visualization (3D)** | SceneView (Filament/Compose) | Integrated |
| **Visualization (Struct)** | OpenGL ES 3.0+ | Integrated |
| **Formats** | glTF/GLB, STEP, IGES, BREP, INP, FRD | FULLY SUPPORTED |

---
*Last Updated: July 1, 2026*

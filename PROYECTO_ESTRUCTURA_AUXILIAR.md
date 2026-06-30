# Informe: Estructura de Componentes Auxiliares del Proyecto

Este documento detalla los archivos, scripts y herramientas que no forman parte directa del código fuente de la aplicación (`app/`), pero que son esenciales para las fases de compilación, validación, pruebas y documentación técnica del proyecto.

---

## 1. Clasificación por Fase de Desarrollo

| Nombre | Tipo | Descripción | Fase |
| :--- | :--- | :--- | :--- |
| `build_*.sh` | Script | Scripts de compilación para librerías nativas (CalculiX, Gmsh, OCCT, SPOOLES). | Compilación / Infraestructura |
| `cbuild_occt_android.sh` | Script | Script especializado de compilación cruzada para OCCT en Android. | Compilación |
| `setup-sdk.sh` | Script | Script de inicialización y configuración del entorno de desarrollo SDK. | Preparación |
| `test_*/` | Ejecutable | Binarios/Proyectos de prueba para componentes individuales. | Pruebas |
| `tests/` | Código fuente | Suite de pruebas unitarias en C++. | Pruebas |
| `validation/` | Datos | Conjunto de datos de validación (archivos .frd, .inp). | Validación |
| `converter_prototype/`| Proyecto | Prototipo de convertidor de formatos (FRD a GLB). | Prototipado / R&D |
| `*.md` | Doc | Informes de auditoría, compilación, planes y guías de desarrollo. | Documentación |

---

## 2. Detalle de Scripts de Compilación (Fase: Compilación)
Estos scripts automatizan la compilación de dependencias nativas complejas:
- **`build_calculix_fixed.sh`**: Compila el motor de elementos finitos CalculiX con parches aplicados.
- **`build_gmsh.sh`**: Compila la herramienta de mallado Gmsh.
- **`build_opencascade.sh`**: Compila OpenCASCADE para modelado geométrico.
- **`build_spooles_spooles.sh`**: Compila la librería SPOOLES para álgebra lineal.
- **`cbuild_occt_android.sh`**: Script crítico para la integración de OCCT en Android mediante compilación cruzada.

## 3. Detalle de Pruebas y Validación (Fase: Pruebas/Validación)
Herramientas utilizadas para verificar la integridad del motor de cálculo:
- **`tests/`**: Contiene implementaciones de pruebas (`test_analysis_model.cpp`, etc.) para verificar la lógica nativa.
- **`validation/`**: Contiene archivos de ejemplo (`beam.frd`, `beam.inp`) utilizados para ejecutar simulaciones de validación rápida.

## 4. Documentación Técnica (Fase: Documentación)
- **Informes de compilación y auditoría**: `INFORME_COMPILACION_CALCULIX.md`, `REPORTE_AUDITORIA.md`, etc.
- **Planes de implementación**: `IMPLEMENTATION_PLAN.md`, `plan_implementacion_fea.md`.
- **Guías**: `DEVELOPER_GUIDE.md`, `guia_desarrollo_calculoestructural.md`, `guia_uso_sdk.md`.

---
*Nota: Este informe es informativo para el mantenimiento y escalabilidad del proyecto.*

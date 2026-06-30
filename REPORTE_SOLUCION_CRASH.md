# Reporte de Solución: Crash al Iniciar la App

## Problema
La aplicación se cerraba inmediatamente tras el inicio. A pesar de aplicar parches de seguridad (null-safety) y cambiar el tema base en XML, la aplicación persistía en cerrarse con un `IllegalStateException` indicando que ya existía una `ActionBar` proporcionada por el sistema.

## Investigación
Se utilizó **Firebase Test Lab** (Robo Test) para obtener el stack trace real del crash en un dispositivo remoto.

### Cómo acceder a los logs en Firebase Test Lab:
1.  **Ejecutar:** `gcloud firebase test android run --type robo --app <ruta_apk> --device model=<modelo>,version=<sdk> --project <ID_PROYECTO>`
2.  **Acceder:** El comando devuelve una URL. Al abrirla, ir a la pestaña "Test Results".
3.  **Descargar Logs:** En la interfaz web, o mediante comandos `gcloud storage cp`, se puede acceder a `data_app_crash_*.txt` para ver el stack trace.

El log reveló que, inexplicablemente, el sistema seguía aplicando el tema antiguo, lo que sugiere un problema de **caché de recursos persistente** en el sistema de compilación de Android.

## Solución
Para forzar la invalidación de la caché de recursos del tema, se realizó un cambio de nombre (renaming) al estilo del tema.

1.  **Renombramiento:**
    -   Estilo cambiado de `Theme.CalculoEstructural` a `Theme.CalculoEstructural.NoActionBar` en `themes.xml` (normal y night).
    -   Actualización de la referencia en `AndroidManifest.xml` (`android:theme="@style/Theme.CalculoEstructural.NoActionBar"`).
2.  **Gestión de UI:**
    -   Se eliminó `setSupportActionBar()` en `MainActivity` para evitar conflictos con el decorado de la ventana.
    -   El `Toolbar` ahora se gestiona como una vista independiente.

## Estado Final
Tras renombrar el tema y realizar un `clean build` completo, la aplicación se inicia correctamente sin conflictos de `ActionBar`.

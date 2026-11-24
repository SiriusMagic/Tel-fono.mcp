# Teléfono (teléfono MCP): reglas para agentes

- Lee `ARQUITECTURA.md` (y el contrato común `FAMILIA.md` del repo de Inicio) y abre solo los archivos que necesites.
- Pruebas: `cd android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` (debe pasar todo).
  Nunca desactives ni saltes una prueba. Lint sin avisos nuevos.
- Cada versión: sube `versionCode` y `versionName`, copia el APK a `android/Telefono-<versión>.apk` (borra el anterior)
  y anótalo en `CHANGELOG.md`.
- Reglas puras (grupos de llamadas, perdidas, T9, duración, códigos, números de Ecuador) en `data/`, con pruebas. Las
  pantallas solo pintan y avisan. El registro y los contactos pasan por `PhoneBook` (`AndroidBook` / `MemoryBook`).
- La IA **nunca** marca, contesta ni cuelga, ni envía códigos *…#. Solo puede pedir una llamada con una notificación que
  el usuario toca (`Requests`), y eso solo abre el marcador.
- Contactos vive dentro de Teléfono (`com.zirakusa.contactos`, recursos con prefijo `c_`, su propio icono). Sus reglas
  siguen: consultas siempre con `?`, la IA puede leer, crear y marcar favoritos; **nunca eliminar**.
- Privacidad: el registro de llamadas y los contactos se leen, no se copian. Nada sale salvo por el Modo MCP que el usuario
  active; internet es solo para eso.
- Cada herramienta MCP nueva lleva su prueba en `McpTest`: lista de herramientas, llamada correcta, argumentos malos y
  «no encontrado». Las que cambian algo exigen token de lectura y escritura (`READ_ONLY` si no).
- Prohibido: `eval`, motores de scripts, SQL concatenado.
- Firma: la de depuración del entorno, igual que el resto de la familia y Gateway Hub.

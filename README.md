# Teléfono (teléfono MCP)

Marcador y llamadas recientes para Android en negro con acento verde. Parte de la familia de Stone, Luma, Calculadora,
Notas, Filem, Brújula, Nivel, Reloj, Calendario, Inicio, QR, Clima, Redes, Contactos y Cámara. Por MCP tu IA ve tus
llamadas y puede **pedirte** que llames a alguien; **nunca marca, contesta ni cuelga**.

## Estado (0.1.0)

- **Recientes:** Todas · Perdidas hoy; llamadas seguidas del mismo número en una sola línea («Ana Pérez (2)»; 099… y +593…
  son el mismo); entrante, saliente, perdida (en rojo), rechazada, bloqueada; hora y duración. Tocar abre Mensaje, Copiar
  y Añadir contacto; el botón verde llama.
- **Teclado:** 12 teclas con letras (Ñ en el 6), búsqueda de contactos mientras marcas (por letras T9 o por dígitos),
  mantener 0 = «+», borrar (mantener borra todo), tonos y códigos como *611# o *#06#.
- **Favoritos:** los contactos con estrella (de Contactos), grandes; un toque llama.
- Abre los números de otras apps (enlaces `tel:`) en el teclado, sin llamar solo.
- **Llamar directamente** (o abrir el marcador del teléfono). La llamada en curso la muestra la pantalla de llamadas del
  propio Android: Teléfono aún no es la app de llamadas predeterminada (eso llega en la 0.2.0, con pantalla de llamada propia).
- **Ajustes:** llamar directamente, tonos del teclado, **Modo MCP**, Temas, privacidad y versión.
- **Modo MCP:** `phone_calls`, `phone_missed`, `phone_call` y, con token de lectura y escritura, `phone_call_request`: una
  notificación «Claude quiere que llames a Ana Pérez» que abre el número; tú decides. La IA no puede pedir códigos (*…#).
  Tokens `tel_…`; módulo `phone_` para Gateway Hub.
- **Resumen para Inicio:** «Llamadas perdidas» de hoy en la pantalla «Hoy».
- Sin anuncios ni estadísticas; internet solo para el Modo MCP (apagado de fábrica).

APK: [`android/Telefono-0.1.0.apk`](android/Telefono-0.1.0.apk) (`com.zirakusa.telefono`, firmado como el resto de la
familia y Gateway Hub).

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

## Siguiente

Ver [`ROADMAP.md`](ROADMAP.md).

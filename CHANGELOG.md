# Teléfono 0.2.0

- Recientes con pestañas «Todo» y «Llamadas perdidas» (con un punto si hoy hubo perdidas), título grande y, en cada
  llamada, la SIM con la que fue (1 o 2), «Móvil» o «Ecuador» y la hora a la manera del teléfono: «Justo ahora»,
  «hace 5 min», «08:12 a. m.», «Ayer», «22/11».
- Botón ⓘ: página del número o contacto con su inicial grande, «Llamar con SIM 2 (por defecto)», llamar, mensaje y
  WhatsApp, y su registro de llamadas (entrante, saliente, perdida; «No conectado» o «(1 m 38 s)»), con «Ver más».
- Doble SIM: el teclado muestra dos botones de llamar, «1» y «2»; en Ajustes, «SIM por defecto» (Preguntar, SIM 1 o
  SIM 2). Si eliges «Preguntar», Teléfono te pregunta con qué SIM llamar.
- Botón de WhatsApp en el teclado y en la página del número (solo si WhatsApp está instalado; Teléfono no usa internet
  para eso).
- Botón «Contactos» en la barra de abajo: abre la app Contactos (o la del teléfono). Editar o crear un contacto se hace
  allí.
- Nuevo permiso «Teléfono» (estado del teléfono): solo para saber qué SIM hay y con cuál fue cada llamada.
- Modo MCP: `phone_calls` y `phone_call` dicen la SIM (`sim`: 1 o 2) cuando se sabe. La IA sigue sin poder marcar.
- 14 pruebas.

# Teléfono 0.1.0

Primera versión: marcador y llamadas recientes en negro con acento verde.
- Recientes agrupadas, perdidas de hoy, acciones por línea.
- Teclado con letras (Ñ), búsqueda T9 de contactos, «+», tonos y códigos.
- Favoritos de Contactos; abrir enlaces `tel:`; llamar directamente o con el marcador del teléfono.
- Modo MCP (`tel_`, módulo `phone_`): `phone_calls`, `phone_missed`, `phone_call`, `phone_call_request` (notificación; la IA
  nunca marca).
- Resumen «Llamadas perdidas» para la pantalla «Hoy» de Inicio.
- 10 pruebas.

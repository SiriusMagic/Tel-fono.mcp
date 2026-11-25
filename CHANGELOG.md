# Teléfono 0.5.0: ya se pueden añadir contactos, widget de Contactos e iconos nuevos

- **Arreglado: no dejaba añadir contactos.** Teléfono solo pedía permiso para *leer* contactos, así que al tocar «Guardar»
  Android lo rechazaba y no pasaba nada. Ahora Contactos pide también *modificarlos* al abrirse (Android lo da sin otra
  pregunta si ya podía leerlos) y, si aun así falta, el editor lo dice con un botón «Permitir» y guarda en cuanto lo das,
  sin perder lo escrito. Si una cuenta no acepta contactos nuevos, lo dice («Prueba otra en “Guardar en”») en vez de
  cerrarse.
- **«Añadir contacto» y «Editar contacto» de Teléfono abren Contactos** (dentro de esta misma app), con el número ya
  escrito. Antes abrían la app de contactos del sistema, que puede no existir. Otras apps que piden «Añadir a contactos»
  también pueden usar Contactos.
- **Widget «Contactos»** (mantén pulsado un espacio vacío › Widgets, o Contactos › Ajustes › «Añadir widget»): tus
  favoritos (★) con sus iniciales, el botón verde para abrir **Teléfono**, «+» para un contacto nuevo y el título para ir
  directo a **Contactos**. Tocar un favorito abre Teléfono con su número listo: la llamada empieza solo cuando tú la
  haces.
- **Iconos nuevos de Teléfono**: 3D (el de fábrica), Píxel, Óleo, Cristal y Clásico. Se quitan los anteriores; si tenías
  uno elegido, pasa solo al nuevo que ocupa su lugar.
- **Icono de Contactos a elegir** (Contactos › Ajustes › Icono de Contactos): Píxel (el de fábrica), Dibujo, Óleo, 3D y
  Cristal. El widget usa el mismo. Tras esta actualización quizá tengas que volver a poner el icono de Contactos en tu
  pantalla de inicio (una sola vez).
- 44 pruebas.

# Teléfono 0.4.0: filtro de llamadas, anuncio de voz y marcación rápida

- **Filtro de llamadas no deseadas** (Ajustes › Llamadas): Teléfono puede ser el «filtro de llamadas» de Android (Android 10
  o más nuevo; Android te pregunta y puedes quitárselo cuando quieras). Opciones: bloquear números ocultos y privados, y
  qué hacer con los números que no están en tus contactos (Sonar, Silenciar o Bloquear; de fábrica suenan). Lista
  **Bloqueados**: añade números desde Recientes, desde la página de un número o a mano, y quítalos con ✕. 099… y
  +593 99… cuentan como el mismo número. Las llamadas bloqueadas se rechazan sin sonar ni avisar y aparecen en Recientes
  como «Bloqueada». Todo se decide en el teléfono, sin internet.
- **Anuncio de voz de llamada entrante**: Nunca, Siempre o Solo con auriculares. Al entrar una llamada, la voz del propio
  teléfono dice «Llamada de Ana Pérez» o el número en grupos («0 9 9, 1 2 3, 4 5 6 7»). Suena al volumen del tono de
  llamada (en silencio no se oye). Necesita que Teléfono sea el filtro de llamadas.
- **Marcación rápida**: asigna un contacto o un número a las teclas 2–9; mantén pulsada la tecla en el teclado (sin nada
  escrito) y llama igual que el botón «Llamar» (con tu SIM por defecto o preguntando). La tecla 1 llama al buzón de voz si
  tu operador lo tiene. Una tecla sin asignar abre la página para asignarla.
- **Características del operador**: «Realizar llamadas con» (Preguntar, SIM 1 o SIM 2: la antigua «SIM por defecto») y
  cada SIM con su nombre (Claro EC, Movistar…) y su ranura.
- **Ajustes de Android**: accesos a «Configuración de SIM y red», «Sonidos y vibración» (tono de llamada) y
  «Configuración de llamadas del sistema» (buzón, desvío, llamada en espera), cada uno con una nota.
- **Grabación automática de llamadas: no es posible.** Desde Android 10, Android no deja que las apps graben llamadas;
  solo la app de teléfono que viene de fábrica puede hacerlo, si el fabricante lo permite. Teléfono no es esa app (no es
  la pantalla de llamada), así que no ofrece ningún interruptor falso: lo explica en Ajustes.
- La IA sigue sin poder marcar, contestar ni colgar; el filtro y la marcación rápida no tienen herramientas MCP.
- 39 pruebas.

# Teléfono 0.3.1

- **Conectar todas** (Gateway Hub 0.0.21): con un toque en el Hub se activa aquí «Redireccionar a Hub» y la app queda
  vinculada, sin entrar en ella. Solo lo puede pedir una app firmada por el mismo desarrollador.
- **Lucía** (la voz del Hub) usa las herramientas de esta app en el teléfono, **sin internet**, con las mismas reglas que
  por MCP. Solo mientras «Redireccionar a Hub» esté activado.
- **Icono de la app**: 5 iconos para elegir (Original, Claro, Dorado, Azul y Verde) en Ajustes › Icono de la app. Tras esta
  actualización quizá tengas que volver a poner el icono en tu pantalla de inicio (una sola vez).

# Teléfono 0.3.0: Contactos dentro de Teléfono

- **Una sola app**: Contactos completo vive ahora dentro de Teléfono (lista con A–Z, ficha, editar, «Guardar en», combinar
  repetidos, cumpleaños, tarjeta con QR, importar/exportar .vcf, cuentas y sus ajustes). Al instalar Teléfono aparecen dos
  iconos: **Teléfono** y **Contactos**; el de Contactos abre directo la lista, sin pasar por el teclado.
- El botón «Contactos» de Teléfono abre esa misma pantalla, al instante.
- **Modo MCP**: Teléfono sirve también las herramientas de contactos (`contacts_search`, `contacts_get`,
  `contacts_favorites`, `contacts_birthdays`; con lectura y escritura `contacts_create` y `contacts_favorite`). Un solo
  token `tel_` para las dos. La IA sigue sin poder borrar contactos ni llamar.
- **Resumen para Inicio**: llamadas perdidas de hoy y, después, los cumpleaños de la semana.
- La app Contactos aparte ya no hace falta: se puede desinstalar (los contactos son los de Android, no se pierde nada; sus
  ajustes de orden y formato se eligen de nuevo).
- 32 pruebas.

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

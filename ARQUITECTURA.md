# Arquitectura de Teléfono

Teléfono incluye **Contactos** (desde 0.3.0): su código vive en `com.zirakusa.contactos` dentro de este mismo módulo, con su
propia actividad y su propio icono de inicio (`com.zirakusa.contactos.MainActivity`). Sus recursos llevan el prefijo `c_`
(`res/values*/contactos.xml`, `c_ic_launcher`). Sus herramientas MCP (`contactos/mcp/ContactTools.kt`) las sirve el
`McpHandler` de Teléfono; `ContactLiveData` les da los contactos reales. Sus pruebas están en `src/test/.../contactos/`.

Carpeta = package (`android/app/src/main/java/com/zirakusa/telefono/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | Lógica pura, con pruebas. `Calls` (`CallType`, `Call`, `Person`, `Group`; `Dial`: agrupar, perdidas, letras T9 con Ñ, búsqueda del teclado, duración, códigos, `tel:` con `%23`; `Numbers`: clave 0…/+593…, formato, nombre de un número); `Store` (`PhoneBook` con `sims()`, `NoAccess`, `MemoryBook`, `Settings` con «SIM por defecto», filtro, bloqueados, anuncio y marcación rápida, `KeyValues`; `voicemail()`); `Sims` (`Sim`, `Handle`, `SubInfo`, `SimChoice`: qué cuenta es SIM 1/2, con qué SIM fue cada llamada, con cuál llamar o si preguntar); `Filter` (`Verdict`, `Unknown`, `FilterSettings`: si una llamada entrante suena, se silencia o se rechaza; lista «Bloqueados» con la clave de `Numbers`); `Announce` (`AnnounceMode`: cuándo hablar y el texto «Llamada de …» con el número en grupos); `SpeedDial` (`Speed`, `LongPress`, `Choice`: teclas 2–9, buzón en la 1, qué hace una pulsación larga y a quién elegir); `When` (`Ago`: «Justo ahora», «hace 5 min», «08:12 a. m.», «Ayer», «22/11» y la hora del registro). `Dial` también: duración «1 m 38 s», «No conectado», historial de un número, inicial; `Numbers.wa` (dígitos para WhatsApp) y `personOf`. |
| `ui/` | `TelefonoState` (único dueño del estado: llamadas, contactos, lo marcado, la línea abierta, ajustes, el papel de filtro `Role`, bloqueados y marcación rápida) y `TelefonoApp` (pestañas Favoritos · Recientes · Teclado y botón Contactos; `AppActions`; `dial`: con la SIM por defecto o preguntando; `longPress`: marcación rápida por el mismo camino; `SystemPage`: páginas de Android). |
| `ui/screens/` | `RecentsScreen` (Todo · Llamadas perdidas, SIM, ⓘ), `KeypadScreen`, `DetailScreen` y `DetailParts` (página de un número: llamar, mensaje, WhatsApp, registro con «Ver más»), `SimParts` (insignia de SIM, «¿Con qué SIM llamas?», botón 1 | 2), `Words` (tiempos y tipos en palabras), `FavoritesScreen`, `SettingsScreen` (con «Llamadas», «Ajustes de Android» y la fila informativa de grabación), `FilterScreen` (`FilterScreen`, `BlockedScreen`, `SubPage`), `SpeedScreen`, `CarrierScreen`, `McpScreen`, `Parts`, `Chrome` y `SettingsParts`. |
| `ui/theme/` | `Theme`: `ThemeMode` y `Accent` (verde de fábrica). |
| `mcp/` | Modo MCP: `Tokens` (`tel_…`), `PhoneTools` (`phone_calls`, `phone_missed`, `phone_call`; con escritura `phone_call_request`; a través de `PhoneData`), `McpHandler`, `RelayLink`, `McpMode` (`LiveData`) y `HubLink` (módulo `phone_`). |
| `AndroidBook.kt` | `CallLog` (con cuenta/SIM, tipo de número y lugar) y los números de `ContactsContract`, solo lectura; las SIM que pueden llamar (`TelecomManager` + `SubscriptionManager`, con `READ_PHONE_STATE`). |
| `Screening.kt` | `Screening` (`CallScreeningService`, Android 10+ con el papel `ROLE_CALL_SCREENING` que el usuario concede): solo llamadas entrantes; decide con `Filter.decide` (rechazo silencioso que queda en el registro como bloqueada, o silenciar) y, si suena, `Announcer` dice el anuncio con `TextToSpeech` por el canal del tono. Nunca contesta, cuelga ni graba. |
| `Requests.kt` | La notificación «… quiere que llames a …» que abre el marcador. |
| `Summary.kt` | `SummaryLines` (pura) y `SummaryProvider`: perdidas de hoy para Inicio. |
| `MainActivity.kt` | Crea el estado, pide permisos, abre `tel:` en el teclado, llama (con `EXTRA_PHONE_ACCOUNT_HANDLE` de la SIM elegida) o abre el marcador, WhatsApp (`wa.me` entregado a la app de WhatsApp), Contactos (la app de la familia o la del teléfono), pide el papel de filtro (`RoleManager`), abre páginas de Android (SIM y red, sonidos, llamadas del sistema), editar contacto (editor del sistema), tonos DTMF, copia e «Ir al Hub». |

## Lo que Android no deja hacer

Teléfono no es la pantalla de llamada (no es un `InCallService` ni la app de teléfono por defecto). Por eso no puede grabar
llamadas (desde Android 10 solo la app de teléfono del sistema, con permisos privilegiados), ni cambiar la pantalla de una
llamada en curso. El filtro de llamadas sí está abierto a cualquier app con el papel de filtro; en algunos teléfonos Android
no le pasa las llamadas de los contactos.

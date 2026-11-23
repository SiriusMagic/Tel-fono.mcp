# Arquitectura de Teléfono

Carpeta = package (`android/app/src/main/java/com/zirakusa/telefono/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | Lógica pura, con pruebas. `Calls` (`CallType`, `Call`, `Person`, `Group`; `Dial`: agrupar, perdidas, letras T9 con Ñ, búsqueda del teclado, duración, códigos, `tel:` con `%23`; `Numbers`: clave 0…/+593…, formato, nombre de un número); `Store` (`PhoneBook` con `sims()`, `NoAccess`, `MemoryBook`, `Settings` con «SIM por defecto», `KeyValues`); `Sims` (`Sim`, `Handle`, `SubInfo`, `SimChoice`: qué cuenta es SIM 1/2, con qué SIM fue cada llamada, con cuál llamar o si preguntar); `When` (`Ago`: «Justo ahora», «hace 5 min», «08:12 a. m.», «Ayer», «22/11» y la hora del registro). `Dial` también: duración «1 m 38 s», «No conectado», historial de un número, inicial; `Numbers.wa` (dígitos para WhatsApp) y `personOf`. |
| `ui/` | `TelefonoState` (único dueño del estado: llamadas, contactos, lo marcado, la línea abierta y ajustes) y `TelefonoApp` (pestañas Favoritos · Recientes · Teclado y botón Contactos; `AppActions`; `dial`: con la SIM por defecto o preguntando). |
| `ui/screens/` | `RecentsScreen` (Todo · Llamadas perdidas, SIM, ⓘ), `KeypadScreen`, `DetailScreen` y `DetailParts` (página de un número: llamar, mensaje, WhatsApp, registro con «Ver más»), `SimParts` (insignia de SIM, «¿Con qué SIM llamas?», botón 1 | 2), `Words` (tiempos y tipos en palabras), `FavoritesScreen`, `SettingsScreen`, `McpScreen`, `Parts`, `Chrome` y `SettingsParts`. |
| `ui/theme/` | `Theme`: `ThemeMode` y `Accent` (verde de fábrica). |
| `mcp/` | Modo MCP: `Tokens` (`tel_…`), `PhoneTools` (`phone_calls`, `phone_missed`, `phone_call`; con escritura `phone_call_request`; a través de `PhoneData`), `McpHandler`, `RelayLink`, `McpMode` (`LiveData`) y `HubLink` (módulo `phone_`). |
| `AndroidBook.kt` | `CallLog` (con cuenta/SIM, tipo de número y lugar) y los números de `ContactsContract`, solo lectura; las SIM que pueden llamar (`TelecomManager` + `SubscriptionManager`, con `READ_PHONE_STATE`). |
| `Requests.kt` | La notificación «… quiere que llames a …» que abre el marcador. |
| `Summary.kt` | `SummaryLines` (pura) y `SummaryProvider`: perdidas de hoy para Inicio. |
| `MainActivity.kt` | Crea el estado, pide permisos, abre `tel:` en el teclado, llama (con `EXTRA_PHONE_ACCOUNT_HANDLE` de la SIM elegida) o abre el marcador, WhatsApp (`wa.me` entregado a la app de WhatsApp), Contactos (la app de la familia o la del teléfono), editar contacto (editor del sistema), tonos DTMF, copia e «Ir al Hub». |

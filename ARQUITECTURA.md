# Arquitectura de Teléfono

Carpeta = package (`android/app/src/main/java/com/zirakusa/telefono/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | Lógica pura, con pruebas. `Calls` (`CallType`, `Call`, `Person`, `Group`; `Dial`: agrupar, perdidas, letras T9 con Ñ, búsqueda del teclado, duración, códigos, `tel:` con `%23`; `Numbers`: clave 0…/+593…, formato, nombre de un número); `Store` (`PhoneBook`, `NoAccess`, `MemoryBook`, `Settings`, `KeyValues`). |
| `ui/` | `TelefonoState` (único dueño del estado: llamadas, contactos, lo marcado, la línea abierta y ajustes) y `TelefonoApp` (pestañas Favoritos · Recientes · Teclado; `AppActions`). |
| `ui/screens/` | `RecentsScreen`, `KeypadScreen`, `FavoritesScreen`, `SettingsScreen`, `McpScreen`, `Parts`, `Chrome` y `SettingsParts`. |
| `ui/theme/` | `Theme`: `ThemeMode` y `Accent` (verde de fábrica). |
| `mcp/` | Modo MCP: `Tokens` (`tel_…`), `PhoneTools` (`phone_calls`, `phone_missed`, `phone_call`; con escritura `phone_call_request`; a través de `PhoneData`), `McpHandler`, `RelayLink`, `McpMode` (`LiveData`) y `HubLink` (módulo `phone_`). |
| `AndroidBook.kt` | `CallLog` y los números de `ContactsContract`, solo lectura. |
| `Requests.kt` | La notificación «… quiere que llames a …» que abre el marcador. |
| `Summary.kt` | `SummaryLines` (pura) y `SummaryProvider`: perdidas de hoy para Inicio. |
| `MainActivity.kt` | Crea el estado, pide permisos, abre `tel:` en el teclado, llama o abre el marcador, tonos DTMF, copia e «Ir al Hub». |

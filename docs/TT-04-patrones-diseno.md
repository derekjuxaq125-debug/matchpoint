# TT-04 · Patrones de diseño aplicados y su justificación

Responsable: Santiago (con apoyo de Julian) · Sprint 2 · 5 pts

El enunciado pide una plataforma **mantenible y preparada para nuevas modalidades de torneo, tipos de pronóstico y reglas de participación**. Cada patrón responde a una de esas necesidades.

## 1. Repository (acceso a datos)

| Dónde | `repositorio/*Repositorio` (interfaces) con versiones `*Memoria` y `*Archivo` |
|---|---|
| Problema | Las reglas de negocio no deben depender de cómo se guardan los datos (archivo, memoria para pruebas, base de datos) |
| Solución | Los servicios reciben la interfaz; `Aplicacion` decide la implementación |
| Beneficio | Las pruebas usan memoria y el programa usa archivos sin cambiar una línea de los servicios. `MovimientoRepositorio` además **no ofrece** borrar ni modificar: el patrón refuerza HU-09 |

## 2. Strategy (cobro de pronósticos y modalidades)

| Dónde | `CobroPronostico` con `NINGUNO` y `CobroConCreditos`; `ModalidadPronostico` |
|---|---|
| Problema | HU-10 se programó antes que HU-11, y en el futuro puede haber dinámicas gratuitas o con otro costo |
| Solución | `PronosticoServicio` llama `cobro.cobrar(...)` sin saber cómo se cobra. Cada modalidad sabe validar su número de opciones |
| Beneficio | HU-11 se integró **sin modificar** la regla de HU-10; una modalidad nueva es un valor más del enum. En la web, `_cobrar_pronostico` cumple el mismo papel |

## 3. Facade / Raíz de composición

| Dónde | `Aplicacion` (Java) y `api.js` (web) |
|---|---|
| Problema | Las pantallas no deben armar repositorios, libros y servicios por su cuenta |
| Solución | `Aplicacion` crea y conecta todo en un solo lugar; las pantallas piden `app.creditos`, `app.saldos`, etc. En la web, `rpc()` es la única puerta a Supabase y agrega la sesión |
| Beneficio | Cambiar archivos por base de datos (TT-01) solo toca `Aplicacion` |

## 4. Patrones de apoyo

- **Unit of Work (transacción)**: cada función RPC de Supabase confirma pronóstico + movimiento + saldo juntos o nada (HU-11).
- **Clave de idempotencia**: `clave_solicitud` / `clave_idempotencia` (HU-14).
- **Registro de vistas** (web): `vistas/index.js`; cada historia agrega su pantalla y el menú por rol se arma solo (HU-02).

## 5. Extensibilidad (revisión pedida en el plan)

| Cambio futuro | Qué se toca |
|---|---|
| Nueva modalidad de pronóstico | Un valor en `ModalidadPronostico` y en el `CHECK` de `eventos` |
| Nueva regla de costo | Otra implementación de `CobroPronostico` |
| Reglas de premio (Sprint 3) | Una estrategia `ReglaPremio` (multiplicador o pozo, ver `DEFINICIONES.md`) |
| Otra forma de guardar | Otra implementación de los repositorios |

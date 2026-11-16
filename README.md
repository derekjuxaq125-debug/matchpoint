# MatchPoint — Santiago: HU-01, HU-02, HU-04, HU-10 (hasta la mitad del Sprint 2)

Responsable: Santiago · Equipo 1 · Cliente: Nexo Digital Labs

Incluye también TT-02 (`docs/TT-02-git.md`, `.gitignore`, `.github/`), TT-03 (`docs/TT-03-kanban.md`, `docs/backlog-matchpoint.csv`) y las definiciones pendientes (`docs/DEFINICIONES.md`).

Proyecto Maven (Java 17+, Swing). Abrir en NetBeans con **File > Open Project**. Ejecutar la clase `com.matchpoint.cuentas.Main`.

## HU-01 · Registrar usuario

Como usuario nuevo, quiero registrarme con una identificación única para participar en los torneos.

| Criterio | Dónde |
|---|---|
| La cuenta se crea con una identificación única (rol Participante) | `UsuarioServicio.registrar`, `ui/DialogoRegistro` |
| Si la identificación ya existe, rechaza el registro con un mensaje claro ("La identificación ya está registrada. Use otra o inicie sesión.", sin distinguir mayúsculas) | `UsuarioServicio` |
| Campo faltante: indica cuál falta y lo resalta en rojo | `UsuarioServicio` + `DialogoRegistro` |
| Identificación de 5 a 15 caracteres (letras y números), contraseña de mínimo 8 y confirmación igual | `UsuarioServicio` |
| La contraseña nunca se guarda en claro (SHA-256 con sal) | `ClaveHash` |
| "Cancelar": no guarda nada y vuelve al inicio de sesión | `DialogoRegistro` |
| Error al guardar: notifica y no se crea ninguna cuenta | `RepositorioException` |

## HU-02 · Iniciar sesión con rol

Como usuario, quiero iniciar sesión y que el sistema reconozca mi rol para acceder solo a lo que me corresponde.

| Criterio | Dónde |
|---|---|
| Con credenciales válidas se inicia sesión y solo se muestran las opciones del rol | `AutenticacionServicio.iniciarSesion`, `ui/VentanaPrincipal`, `modelo/Rol` |
| Con credenciales inválidas se niega el acceso ("Identificación o contraseña incorrectas.", mismo mensaje si el usuario no existe o la clave falla) | `AutenticacionServicio` |
| Campo faltante: indica cuál falta y lo resalta en rojo | `AutenticacionServicio` + `VentanaLogin` |
| Cerrar sesión devuelve al inicio de sesión | `VentanaPrincipal` |

Roles y opciones: **Participante** (listar torneos y eventos, ver opciones de pronóstico, registrar pronóstico, consultar saldo), **Organizador** (crear torneo, registrar evento, asignar créditos iniciales) y **Auditor** (reporte consolidado, detectar inconsistencias). Las opciones son un menú (`modelo/Opcion`); cada una se habilita con su historia de usuario. Ya funcionan **Registrar evento y opciones** (HU-04) y **Registrar pronóstico** (HU-10).

## HU-04 · Registrar evento

Como organizador, quiero registrar un evento con su fecha, modalidad de pronóstico, opciones válidas y hora de cierre para que los participantes puedan pronosticar.

| Criterio | Dónde |
|---|---|
| Solo un organizador registra eventos | `EventoServicio.registrar` (`AccesoDenegadoException`) |
| Evento con torneo, nombre, fecha y hora, modalidad, opciones, hora de cierre y costo | `EventoServicio`, `ui/DialogoEvento` |
| Campo faltante: indica cuál falta y lo resalta en rojo | `EventoServicio` + `DialogoEvento` |
| Modalidad: 2 opciones (sin empate), 3 (local/empate/visitante) o múltiple (2 a 10); sin opciones repetidas | `modelo/ModalidadPronostico`, `EventoServicio` |
| Fecha del evento futura y dentro de las fechas del torneo; cierre futuro y no posterior al inicio | `EventoServicio` |
| Sin eventos duplicados (mismo torneo, nombre y fecha) | `EventoServicio` |
| Error al guardar: notifica y no se crea el evento | `RepositorioException` |

Fechas y horas se escriben como `2026-11-20 15:00`. El evento está **abierto** hasta su hora de cierre y **cerrado** desde ese instante (`Evento.estado`).

## HU-10 · Registrar pronóstico

Como participante, quiero registrar mi pronóstico en un evento para competir por premios.

| Criterio | Dónde |
|---|---|
| Solo un participante pronostica | `PronosticoServicio.registrar` (`AccesoDenegadoException`) |
| Solo en un evento habilitado (existente y antes del cierre); si no, mensaje claro (regla compartida con HU-12) | `PronosticoServicio`, `Evento.estaHabilitado` |
| Solo con una opción válida del evento (sin distinguir mayúsculas) | `PronosticoServicio` |
| Un pronóstico por participante y evento | `PronosticoServicio` |
| Campo faltante: indica cuál falta y lo resalta en rojo | `PronosticoServicio` + `ui/DialogoPronostico` |
| Error al guardar: notifica y no se registra nada | `RepositorioException` |

## Puntos de enlace con el resto del equipo

- **Julian (HU-11 / HU-09 / HU-14):** `CobroPronostico` es la interfaz que `PronosticoServicio` llama antes de guardar, con el id del pronóstico como referencia y `Evento.costoCreditos()`. Hoy es `CobroPronostico.NINGUNO`; se reemplaza en el constructor de `PronosticoServicio` (ver `Aplicacion`). Si lanza una excepción, el pronóstico no se guarda.
- **Julian (TT-01):** los repositorios son interfaces (`UsuarioRepositorio`, `TorneoRepositorio`, `EventoRepositorio`, `PronosticoRepositorio`) con versión en memoria y en archivo. Para pasar a base de datos se agrega una implementación y se cambia en `Aplicacion`.
- **Johan (HU-03):** `Torneo` y `TorneoRepositorio` son el contrato mínimo que HU-04 necesita. `Aplicacion` crea un "Torneo de demostración" si no hay ninguno; se borra cuando HU-03 esté integrada.
- **Johan (HU-05/06/08):** las opciones de menú ya existen en `modelo/Opcion`; falta cambiar el `default` de `VentanaPrincipal.abrir` por su pantalla.

## Cuentas de prueba

El registro público solo crea participantes. La primera vez que se ejecuta se crean estas dos cuentas:

| Rol | Identificación | Contraseña |
|---|---|---|
| Organizador | `ORG0001` | `organizador123` |
| Auditor | `AUD0001` | `auditor123` |

Los datos se guardan en `usuarios.txt`, `torneos.txt`, `eventos.txt` y `pronosticos.txt` (junto al programa) y persisten entre ejecuciones. Para empezar de cero se borran esos archivos.

## Pruebas

JUnit 5, `mvn test`: `UsuarioServicioTest` (HU-01), `AutenticacionServicioTest` (HU-02), `EventoServicioTest` (HU-04), `PronosticoServicioTest` (HU-10 y regla de cierre de HU-12) y `RepositoriosArchivoTest` (persistencia).

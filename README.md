# MatchPoint — Equipo 1 · Nexo Digital Labs

Plataforma de gestión de torneos, pronósticos y créditos virtuales. Los créditos **no** tienen valor monetario, no se retiran ni se convierten en dinero.

| Integrante | Rol |
|---|---|
| Santiago | Líder / Analista |
| Julian | Arquitecto / Backend |
| Johan (Camilo en la wiki) | Desarrollo / QA |

El proyecto tiene dos versiones con **las mismas reglas de negocio**:

| Versión | Tecnología | Cómo se usa |
|---|---|---|
| Escritorio | Java 17, Swing, Maven, JUnit 5 | NetBeans → *File > Open Project* → ejecutar `com.matchpoint.cuentas.Main` |
| Web | Vite + JavaScript, Supabase (PostgreSQL), Vercel | Ver [`docs/DESPLIEGUE.md`](docs/DESPLIEGUE.md) |

## Estado por sprint

| Sprint | Historias | Estado |
|---|---|---|
| 1 | HU-01, HU-03, HU-07, TT-01, TT-02, TT-03 | Terminado |
| 2 | HU-02, HU-04, HU-05, HU-06, HU-08, HU-09, HU-10, HU-11, HU-12, HU-14, HU-15, HU-26, TT-04, TT-05 | Terminado |
| 3 | HU-13, HU-16 a HU-25, TT-06 | Pendiente |

## Dónde está cada historia

| ID | Java (`src/main/java/com/matchpoint/cuentas`) | Web / Supabase |
|---|---|---|
| HU-01 / HU-02 | `UsuarioServicio`, `AutenticacionServicio`, `ui/VentanaLogin` | `vistas/acceso.js`, migración 003 |
| HU-03 | `TorneoServicio`, `ui/DialogoTorneo` | `vistas/torneos-organizador.js`, migración 005 |
| HU-04 | `EventoServicio`, `ui/DialogoEvento` | `vistas/eventos-organizador.js`, migración 006 |
| HU-05 / HU-06 | `ConsultaServicio`, `ui/VentanaTorneosEventos` | `vistas/torneos.js`, migración 009 |
| HU-07 | `CreditoServicio`, `ui/DialogoAsignarCreditos` | `vistas/creditos.js`, migración 004 |
| HU-08 | `SaldoServicio`, `ui/DialogoSaldo` | `vistas/saldo.js`, migración 010 |
| HU-09 | `LibroMovimientos`, `MovimientoRepositorio` | tabla `movimientos` + triggers, migración 002 |
| HU-10 | `PronosticoServicio`, `ui/DialogoPronostico` | `vistas/pronosticar.js`, migración 006 |
| HU-11 | `CobroConCreditos` | `_cobrar_pronostico`, migración 007 |
| HU-12 | `PronosticoServicio.cambiar`, `ui/DialogoMisPronosticos` | `vistas/mis-pronosticos.js`, migración 011 |
| HU-14 / HU-15 | Clave de idempotencia y candado por usuario en `LibroMovimientos` | `clave_solicitud`, `FOR UPDATE`, migración 008 |
| HU-26 | `AccesoDatos` | RLS + funciones por token, migración 012 |
| TT-01 | — | `supabase/migrations/…0100_tt01_esquema.sql`, [`docs/TT-01-modelo-datos.md`](docs/TT-01-modelo-datos.md) |
| TT-04 | Repository, Strategy, Facade | [`docs/TT-04-patrones-diseno.md`](docs/TT-04-patrones-diseno.md) |
| TT-05 | `src/test/.../TT05SaldoIdempotenciaConcurrenciaTest` | `supabase/tests/tt05_pruebas.sql`, `web/scripts/prueba-concurrencia.mjs`, [`docs/TT-05-pruebas.md`](docs/TT-05-pruebas.md) |

Otros documentos: [`docs/TT-02-git.md`](docs/TT-02-git.md) (ramas y revisión entre pares), [`docs/TT-03-kanban.md`](docs/TT-03-kanban.md), [`docs/DEFINICIONES.md`](docs/DEFINICIONES.md).

## Pruebas

```bash
mvn test                      # Java: 95 pruebas JUnit 5
cd web && npm run build       # la web compila
```

## Cuentas de prueba

| Rol | Identificación | Contraseña |
|---|---|---|
| Organizador | `ORG0001` | `organizador123` |
| Auditor | `AUD0001` | `auditor123` |
| Participante (solo web, con 100 créditos) | `DEMO00001` | `demo12345` |

En la versión de escritorio los participantes se registran desde la pantalla de inicio y el organizador les asigna créditos (HU-07). Los datos se guardan en `usuarios.txt`, `torneos.txt`, `eventos.txt`, `pronosticos.txt` y `movimientos.txt`.

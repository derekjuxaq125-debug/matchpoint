# TT-05 · Pruebas de saldo, idempotencia y concurrencia

Responsable: Johan · Sprint 2 · 8 pts · Meta: **cero descuentos duplicados y cero saldos negativos**.

## 1. Versión de escritorio (Java, JUnit 5)

`mvn test` ejecuta, entre otras, `TT05SaldoIdempotenciaConcurrenciaTest`:

| Caso | Escenario | Resultado esperado |
|---|---|---|
| TT05-01 | 100 débitos de 1 crédito a la vez sobre 50 créditos | 50 aceptados, saldo 0 |
| TT05-02 | La misma solicitud (misma clave) 50 veces a la vez | 1 movimiento |
| TT05-03 | 20 reintentos simultáneos del mismo pronóstico | 1 pronóstico, 1 descuento, 19 "repetida" |
| TT05-04 | 20 pronósticos simultáneos en 20 eventos de 10 con 100 créditos | 10 aceptados, saldo 0 |
| TT05-05 | 5 usuarios, 200 operaciones simultáneas | Saldos independientes, todos en 0 |
| TT05-06 | Concurrencia guardando en archivo | El archivo queda consistente |

En todos se verifica además: saldo = suma de movimientos, ningún `saldoResultante` negativo y ninguna clave repetida.

Pruebas por historia: `LibroMovimientosTest` (HU-09), `CreditoServicioTest` (HU-07), `CobroConCreditosTest` (HU-11), `IdempotenciaConcurrenciaTest` (HU-14/HU-15), `SaldoServicioTest` (HU-08), `CambioPronosticoTest` (HU-12), `AccesoDatosPropiosTest` (HU-26), `TorneoServicioTest` (HU-03), `ConsultaServicioTest` (HU-05/HU-06).

## 2. Versión web (Supabase)

- `supabase/tests/tt05_pruebas.sql`: 22 casos en una transacción que se deshace al final (no deja datos). Se pega completo en *SQL Editor* y debe devolver todas las filas con `ok = true`.
- `web/scripts/prueba-concurrencia.mjs` (`npm run prueba:concurrencia`): envía solicitudes simultáneas reales por HTTP (10 asignaciones iniciales, 15 envíos del mismo pronóstico y pronósticos en paralelo en varios eventos).

## 3. Por qué funciona

| Riesgo del enunciado | Control |
|---|---|
| Doble clic / reintento por mala conexión | Clave de solicitud única por intento (`pronosticos.clave_solicitud`, `movimientos.clave_idempotencia`); repetirla devuelve el resultado original |
| Dos operaciones sobre el mismo saldo | Java: candado por usuario en `LibroMovimientos`. Web: `SELECT ... FOR UPDATE` sobre `cuentas_credito` |
| Saldo negativo | Verificación antes del débito + `CHECK (saldo >= 0)` en la tabla |
| Falla a mitad de la operación | Web: todo en una transacción. Java: si el pronóstico no se guarda, se registra una REVERSION (no se borra el débito) |

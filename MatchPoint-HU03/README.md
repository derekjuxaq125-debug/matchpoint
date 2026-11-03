# MatchPoint — HU-03 Crear torneo (Sprint 1)

Responsable: Camilo · Equipo 1 · Cliente: Nexo Digital Labs

Proyecto Maven (Java 17+, Swing). Abrir en NetBeans con **File > Open Project**. Ejecutar la clase `com.matchpoint.torneos.Main`.

## Criterios de aceptación cubiertos

| Criterio | Dónde |
|---|---|
| Formulario con Nombre, Deporte (lista desplegable), Fecha de inicio y Fecha de fin, todos obligatorios, y botones "Crear" y "Cancelar" | `ui/DialogoCrearTorneo` |
| Datos válidos: guarda, confirma y el torneo aparece en la lista (nombre, deporte y fechas) | `TorneoServicio.crear`, `ui/VentanaTorneos` |
| Fecha de fin anterior a inicio: rechaza con "La fecha de fin no puede ser anterior a la de inicio" (fechas iguales permitidas) | `TorneoServicio` |
| Fecha de inicio anterior a la actual: rechaza con mensaje claro | `TorneoServicio` |
| Nombre repetido: no lo crea e informa que está en uso (sin distinguir mayúsculas) | `TorneoServicio` |
| Campo faltante: indica cuál falta y lo resalta en rojo | `TorneoServicio` + `DialogoCrearTorneo` |
| "Cancelar": no guarda nada y vuelve a la lista | `DialogoCrearTorneo` |
| Error al guardar: notifica y no se crea ningún torneo | `RepositorioException` |

Los torneos se guardan en `torneos.txt` (junto al programa) y persisten entre ejecuciones.

## Pruebas

`src/test/.../TorneoServicioTest.java` (JUnit 5): `mvn test`.

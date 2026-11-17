# TT-03 · Configurar el tablero Kanban

Responsable: Santiago · Sprint 1 · 1 pt

Recomendado: **GitHub Projects** (queda junto al repositorio y se enlaza solo con los Pull Requests). Si prefieren Trello, los pasos son equivalentes.

## 1. Columnas

| Columna | Qué significa | Límite sugerido |
|---|---|---|
| Backlog | Ítems del proyecto sin sprint asignado | — |
| Sprint (por hacer) | Ítems comprometidos en el sprint actual | — |
| En progreso | Alguien está trabajando en la rama | 2 por persona |
| En revisión | Pull Request abierto, espera al revisor del ciclo | — |
| Terminada | Cumple la definición de terminado (ver `TT-02-git.md`, sección 3) | — |

## 2. Campos / etiquetas
- **Sprint:** Sprint 1, Sprint 2, Sprint 3.
- **Responsable:** Santiago, Julian, Johan.
- **Puntos:** el número del backlog.
- **Tipo:** `HU` (historia) o `TT` (tarea técnica).
- **Bloqueada:** para ítems que esperan a otro (ver columna "Depende de" del CSV).

## 3. Cargar los 32 ítems

El archivo `backlog-matchpoint.csv` trae los 32 ítems con ID, título, puntos, sprint, responsable, dependencias y estado.

- **GitHub Projects:** crear el proyecto (vista *Board*), y en la vista *Table* pegar las filas del CSV (o crear los ítems con `gh project item-create`). Agregar los campos de la sección 2.
- **Trello:** *Menú → Importar* no acepta CSV directo; usar el Power-Up *CSV Importer*, o crear una tarjeta por ítem copiando la columna "Título".

## 4. Cierre de la tarea
1. Crear el tablero y dar acceso a Julian y Johan.
2. Pegar el enlace del tablero en la wiki del Sprint 1 (sección de herramientas).
3. Tablero creado en Trello: **MatchPoint · Equipo 1** (enlace en la wiki, página Sprint 2 Backlog). Cada tarjeta lleva criterios de aceptación, checklist de definición de terminado y la revisión entre pares.

## 5. Rutina semanal
- En la reunión semanal: revisar columna "En revisión" primero (desbloquea a los demás), luego "En progreso".
- Quien termina una historia actualiza el tablero **y** el backlog de la wiki en el mismo momento.

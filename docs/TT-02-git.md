# TT-02 · Configurar Git (rama por funcionalidad y revisión entre pares)

Responsable: Santiago · Sprint 1 · 2 pts

## 1. Ramas

| Rama | Uso |
|---|---|
| `main` | Siempre compila y pasa las pruebas. Nadie hace push directo. |
| `feature/HU-XX-descripcion-corta` | Una por historia de usuario. Ej.: `feature/HU-10-registrar-pronostico` |
| `tarea/TT-XX-descripcion-corta` | Una por tarea técnica. Ej.: `tarea/TT-01-esquema-bd` |
| `fix/HU-XX-descripcion-corta` | Correcciones encontradas en la revisión o en las pruebas |

El ID de la historia va siempre en el nombre de la rama.

## 2. Flujo de trabajo (cada historia)

```bash
git checkout main && git pull
git checkout -b feature/HU-10-registrar-pronostico
# ... programar, probar con `mvn test` ...
git add <solo tus archivos>
git commit -m "HU-10: registrar pronóstico solo en eventos habilitados"
git push -u origin feature/HU-10-registrar-pronostico
# abrir Pull Request hacia main (se llena la plantilla automáticamente)
```

Mensajes de commit: `HU-XX: qué se hizo` (en presente, en español, una idea por commit).

## 3. Revisión entre pares (ciclo fijo)

| Quien programa | Quien revisa |
|---|---|
| Johan | Santiago |
| Julian | Johan |
| Santiago | Julian |

Reglas:
- Nada se une a `main` sin la aprobación del revisor que toca.
- El revisor comprueba los criterios de aceptación de la wiki, que haya pruebas y que `mvn test` pase.
- Se une con **Squash and merge** o **Rebase and merge** para que el historial muestre quién hizo cada parte.
- Si el revisor pide cambios, se corrigen en la misma rama (nuevos commits) y se vuelve a pedir revisión.
- Un ítem está terminado cuando: el código está en `main`, cumple los criterios, tiene pruebas y el estado se actualizó en el tablero y en el backlog.

## 4. Configuración que hay que hacer una sola vez

**Cada integrante, en su computador** (para que los commits queden con su cuenta):

```bash
git config user.name  "Nombre Apellido"
git config user.email "correo-de-su-cuenta-de-github@ejemplo.com"
```

**Quien administra el repositorio (Settings → Branches → Add branch protection rule, patrón `main`):**
1. Marcar *Require a pull request before merging*.
2. Marcar *Require approvals* con 1 aprobación.
3. Marcar *Dismiss stale pull request approvals when new commits are pushed*.
4. Marcar *Do not allow bypassing the above settings*.
5. En *Settings → General → Pull Requests*: activar *Automatically delete head branches*.

**Archivos de este repositorio que ya dejan la convención lista:**
- `.gitignore`: evita subir `target/`, archivos del IDE y los datos que genera el programa.
- `.github/pull_request_template.md`: plantilla con la lista de verificación de "terminado".

## 5. Reglas del Canvas que se ven en Git
- Mínimo de 6 a 8 horas semanales por integrante y reunión semanal: el historial de commits y el tablero son la evidencia.
- Cada quien hace commit con su propia cuenta; no se suben archivos de otro compañero.

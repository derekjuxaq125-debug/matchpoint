# Definiciones pendientes del Plan de trabajo (propuestas)

Responsable: Santiago · Son **propuestas** para aprobar en la reunión semanal (o con el cliente, Nexo Digital Labs). No se pudo contrastar con el enunciado ni con la wiki; si el enunciado dice otra cosa, manda el enunciado.

Lo que ya quedó implementado con estas propuestas se marca **(en código)** y se cambia en una sola constante.

## 1. Dudas de las revisiones

| Duda | Propuesta | Dónde |
|---|---|---|
| Longitud máxima del nombre de usuario (HU-01) | 60 caracteres **(en código)** | `UsuarioServicio.NOMBRE_MAX` |
| Longitud máxima del nombre de torneo (HU-03, Johan) | 60 caracteres, igual que usuarios y eventos | HU-03 |
| Longitud máxima del nombre de evento (HU-04) | 60 caracteres **(en código)** | `EventoServicio.NOMBRE_MAX` |
| Cantidad máxima de créditos iniciales (HU-07, Julian) | Entero entre 1 y 10 000 | HU-07 |
| Contraseña (HU-01) | Mínimo 8 caracteres, guardada con SHA-256 + sal propia **(en código)**. Si el cliente exige un algoritmo de contraseñas (bcrypt/PBKDF2), se cambia solo `ClaveHash`. | `ClaveHash` |

## 2. Costo en créditos de cada pronóstico

- Cada **evento** define su costo al registrarse (entero mayor que cero); en el formulario viene precargado con 10 **(en código)**.
- Todos los pronósticos de ese evento cuestan lo mismo, así el costo queda fijo aunque cambie el saldo del usuario.
- HU-11 (Julian) lee `Evento.costoCreditos()` y descuenta ese valor.

## 3. Reglas de pronósticos (HU-10 / HU-12)

- Solo un participante puede pronosticar; solo un organizador registra eventos **(en código)**.
- Un participante tiene **un solo pronóstico por evento**; para cambiarlo se usa HU-12 (cambios antes del cierre) **(en código: se rechaza el segundo registro)**.
- Un evento admite pronósticos hasta su hora de cierre; en el instante de cierre ya está cerrado **(en código)**.
- La hora de cierre no puede ser posterior al inicio del evento **(en código)**.

## 4. Operaciones fallidas

Una operación que falla **no deja rastro parcial**:

| Caso | Qué pasa | Mensaje al usuario |
|---|---|---|
| Datos inválidos o campo faltante | No se guarda nada; se resalta el campo | Indica cuál campo y por qué |
| Evento cerrado / inexistente / opción inválida | No se guarda ni se descuenta nada | "El evento ya está cerrado. No se aceptan pronósticos." u otro específico |
| Saldo insuficiente (HU-11) | No se registra el pronóstico ni se descuenta | "Saldo insuficiente." |
| Error al guardar (archivo o BD) | No se crea el registro ni el movimiento | "No se pudo guardar … No se registró …" |
| Solicitud repetida (HU-14) | Se ignora el duplicado y se devuelve el resultado de la primera | "La operación ya fue procesada." |

Regla para HU-11: el descuento y el pronóstico se confirman **juntos** (una transacción en la BD). `PronosticoServicio` llama a `CobroPronostico.cobrar` antes de guardar; si falla, no se guarda el pronóstico.

## 5. Cálculo de premios (HU-20, HU-21)

Propuesta: **multiplicador fijo = número de opciones del evento** (2 opciones → x2; 3 opciones → x3; …).

- Premio = costo del pronóstico × número de opciones, solo para quienes acertaron.
- Un movimiento de tipo PREMIO por cada pronóstico ganador, con el id del pronóstico como origen.
- Si nadie acierta, no se genera ningún movimiento.
- La liquidación es idempotente porque un pronóstico ya premiado no se vuelve a premiar (HU-21).
- Es sencillo de explicar y de probar. Alternativa, si el cliente la prefiere: **pozo compartido** (se reparte entre los ganadores todo lo recaudado; hay que definir qué se hace con el residuo de la división entera).

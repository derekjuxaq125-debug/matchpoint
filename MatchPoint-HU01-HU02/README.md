# MatchPoint — HU-01 Registrar usuario y HU-02 Iniciar sesión con rol (Sprint 1)

Responsable: Santiago · Equipo 1 · Cliente: Nexo Digital Labs

Proyecto Maven (Java 17+, Swing). Abrir en NetBeans con **File > Open Project**. Ejecutar la clase `com.matchpoint.cuentas.Main`.

## HU-01 · Registrar usuario

Como usuario nuevo, quiero registrarme con una identificación única para participar en los torneos.

| Criterio | Dónde |
|---|---|
| La cuenta se crea con una identificación única (rol Participante) | `UsuarioServicio.registrar`, `ui/DialogoRegistro` |
| Si la identificación ya existe, rechaza el registro con un mensaje claro ("La identificación ya está registrada. Use otra o inicie sesión.", sin distinguir mayúsculas) | `UsuarioServicio` |
| Campo faltante: indica cuál falta y lo resalta en rojo | `UsuarioServicio` + `DialogoRegistro` |
| Identificación de 5 a 15 caracteres (letras y números), contraseña de mínimo 6 y confirmación igual | `UsuarioServicio` |
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

Roles y opciones: **Participante** (listar torneos y eventos, ver opciones de pronóstico, consultar saldo), **Organizador** (crear torneo, registrar evento, asignar créditos iniciales) y **Auditor** (reporte consolidado, detectar inconsistencias). Las opciones son un menú; cada una se habilita con su historia de usuario.

## Cuentas de prueba

El registro público solo crea participantes. La primera vez que se ejecuta se crean estas dos cuentas:

| Rol | Identificación | Contraseña |
|---|---|---|
| Organizador | `ORG0001` | `organizador123` |
| Auditor | `AUD0001` | `auditor123` |

Los usuarios se guardan en `usuarios.txt` (junto al programa) y persisten entre ejecuciones.

## Pruebas

`src/test/.../UsuarioServicioTest.java` y `AutenticacionServicioTest.java` (JUnit 5): `mvn test`.

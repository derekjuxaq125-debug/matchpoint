# HU-26 · Acceso solo a datos propios

Responsable: Santiago · Sprint 2 · 3 pts

> Como participante, quiero ver y gestionar únicamente mis participaciones y movimientos para proteger mi información.

| Capa | Versión de escritorio (Java) | Versión web (Supabase) |
|---|---|---|
| Quién es el usuario | `Usuario` de la sesión (HU-02) | Token de `sesiones`, validado por `_usuario_sesion` |
| Lectura | `AccesoDatos.exigirLectura`: el participante solo lo propio, el auditor todo (solo lectura), el organizador nada individual | `consultar_saldo`, `mis_pronosticos` toman el usuario del token; `consultar_participacion` aplica la misma regla |
| Gestión | `AccesoDatos.exigirGestion`: solo el dueño cambia su pronóstico | `cambiar_pronostico` rechaza pronósticos ajenos con el mismo mensaje que uno inexistente |
| Tablas | Archivos locales | RLS activo sin políticas + `REVOKE`: con la llave pública no se lee ninguna tabla |

Pruebas: `AccesoDatosPropiosTest` (Java) y la sección HU-26 de `supabase/tests/tt05_pruebas.sql`.

# Despliegue: Supabase (base de datos) + Vercel (página web)

Responsable: Johan · Sprint 2

## 1. Supabase (ya está creado)

Proyecto `matchpoint` (región São Paulo): `https://qvdlllzkpzvwpbpgiiss.supabase.co`.
Las migraciones de `supabase/migrations/` ya están aplicadas en ese proyecto, con datos de demostración.

Para crear la base en **otro** proyecto: *SQL Editor* → ejecutar los archivos de `supabase/migrations/` **en orden de nombre** (001 a 012) y poner su URL y llave en `web/.env.local`.

Comprobación: pegar `supabase/tests/tt05_pruebas.sql` en el *SQL Editor* → todas las filas `ok = true` (no deja datos).

## 2. Vercel

1. Entrar a https://vercel.com con la cuenta de GitHub → **Add New… → Project**.
2. Importar el repositorio `derekjuxaq125-debug/matchpoint`.
3. Elegir `web` como *Root Directory*. El archivo `web/vercel.json` indica el preset Vite, los comandos de instalación y compilación, y la carpeta que se publica (`dist`).
4. **Deploy**. Cada push a `main` vuelve a publicar.

La URL y la llave pública de Supabase van en `web/.env.production`. Si prefieren no tenerlas en el repo, bórrenlas de ahí y créelas en Vercel → *Settings → Environment Variables*: `VITE_SUPABASE_URL` y `VITE_SUPABASE_ANON_KEY`.

> La llave publicable/anon **es pública por diseño**: la seguridad está en RLS y en las funciones (ver `docs/HU-26-acceso-datos-propios.md`). Nunca suban la llave `service_role`.

## 3. Correr la web en el computador

```bash
cd web
npm install
npm run dev        # http://localhost:5173
```

## 4. Cuentas

| Rol | Identificación | Contraseña |
|---|---|---|
| Organizador | `ORG0001` | `organizador123` |
| Auditor | `AUD0001` | `auditor123` |
| Participante de demostración | `DEMO00001` | `demo12345` (100 créditos) |

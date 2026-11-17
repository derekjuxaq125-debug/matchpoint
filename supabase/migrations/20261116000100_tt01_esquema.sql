-- =====================================================================
-- TT-01 · Esquema de base de datos de MatchPoint (Supabase / PostgreSQL)
-- Responsable: Julian · Sprint 1 (cerrado en Sprint 2)
--
-- Tablas: usuarios, sesiones, torneos, eventos, pronosticos,
--         cuentas_credito (saldo vigente) y movimientos (libro contable).
-- Regla de seguridad: el navegador NUNCA lee ni escribe las tablas
-- directamente. RLS queda activo y sin políticas, y solo las funciones
-- RPC (security definer) validan la sesión y el rol antes de operar.
-- =====================================================================

create extension if not exists pgcrypto with schema extensions;

-- ---------- Usuarios (HU-01, HU-02) ----------
create table public.usuarios (
    identificacion  text primary key
                    check (identificacion ~ '^[A-Za-z0-9]{5,15}$'),
    nombre          text not null check (length(trim(nombre)) between 1 and 60),
    rol             text not null default 'PARTICIPANTE'
                    check (rol in ('PARTICIPANTE', 'ORGANIZADOR', 'AUDITOR')),
    clave_hash      text not null,               -- bcrypt (pgcrypto), nunca en claro
    creado          timestamptz not null default now()
);
-- La identificación es única sin distinguir mayúsculas.
create unique index usuarios_identificacion_ci on public.usuarios (lower(identificacion));

-- ---------- Sesiones (HU-02) ----------
create table public.sesiones (
    token       uuid primary key default gen_random_uuid(),
    usuario_id  text not null references public.usuarios (identificacion) on delete cascade,
    creada      timestamptz not null default now(),
    expira      timestamptz not null default now() + interval '8 hours'
);
create index sesiones_usuario on public.sesiones (usuario_id);

-- ---------- Torneos (HU-03) ----------
create table public.torneos (
    id            uuid primary key default gen_random_uuid(),
    nombre        text not null check (length(trim(nombre)) between 1 and 60),
    deporte       text not null,
    fecha_inicio  date not null,
    fecha_fin     date not null,
    creado_por    text not null references public.usuarios (identificacion),
    creado        timestamptz not null default now(),
    constraint torneo_fechas check (fecha_fin >= fecha_inicio)
);
create unique index torneos_nombre_ci on public.torneos (lower(trim(nombre)));

-- ---------- Eventos (HU-04, HU-05, HU-06) ----------
create table public.eventos (
    id              uuid primary key default gen_random_uuid(),
    torneo_id       uuid not null references public.torneos (id),
    nombre          text not null check (length(trim(nombre)) between 1 and 60),
    inicio          timestamptz not null,
    modalidad       text not null
                    check (modalidad in ('GANADOR_ENFRENTAMIENTO', 'GANADOR_O_EMPATE', 'OPCION_MULTIPLE')),
    opciones        text[] not null check (cardinality(opciones) between 2 and 10),
    cierre          timestamptz not null,
    costo_creditos  integer not null check (costo_creditos > 0),
    creado_por      text not null references public.usuarios (identificacion),
    creado          timestamptz not null default now(),
    constraint evento_cierre check (cierre <= inicio)
);
create unique index eventos_sin_duplicados on public.eventos (torneo_id, lower(trim(nombre)), inicio);

-- ---------- Pronósticos (HU-10, HU-12, HU-14) ----------
create table public.pronosticos (
    id               uuid primary key default gen_random_uuid(),
    usuario_id       text not null references public.usuarios (identificacion),
    evento_id        uuid not null references public.eventos (id),
    opcion           text not null,
    clave_solicitud  text not null unique,          -- llave de idempotencia (HU-14)
    registrado       timestamptz not null default now(),
    actualizado      timestamptz,
    constraint un_pronostico_por_evento unique (usuario_id, evento_id)
);

-- ---------- Saldo vigente (HU-07, HU-08, HU-11, HU-15) ----------
-- Una fila por usuario. El CHECK impide saldos negativos aunque falle la lógica.
create table public.cuentas_credito (
    usuario_id         text primary key references public.usuarios (identificacion),
    saldo              integer not null default 0 check (saldo >= 0),
    inicial_asignado   boolean not null default false,
    actualizado        timestamptz not null default now()
);

-- ---------- Libro de movimientos (HU-09) ----------
create table public.movimientos (
    id                  bigint generated always as identity primary key,
    operacion           uuid not null default gen_random_uuid(),   -- identifica la operación
    usuario_id          text not null references public.usuarios (identificacion),
    tipo                text not null
                        check (tipo in ('ASIGNACION_INICIAL', 'PARTICIPACION', 'PREMIO', 'AJUSTE', 'REVERSION')),
    cantidad            integer not null check (cantidad <> 0),    -- + crédito / - débito
    saldo_resultante    integer not null check (saldo_resultante >= 0),
    origen_tipo         text not null,                             -- USUARIO, PRONOSTICO, LIQUIDACION...
    origen_id           text not null,                             -- id de la operación que lo originó
    clave_idempotencia  text not null unique,                      -- evita movimientos duplicados
    justificacion       text,
    registrado_por      text not null references public.usuarios (identificacion),
    registrado          timestamptz not null default now()
);
create index movimientos_usuario on public.movimientos (usuario_id, id);

-- ---------- Seguridad ----------
alter table public.usuarios        enable row level security;
alter table public.sesiones        enable row level security;
alter table public.torneos         enable row level security;
alter table public.eventos         enable row level security;
alter table public.pronosticos     enable row level security;
alter table public.cuentas_credito enable row level security;
alter table public.movimientos     enable row level security;

revoke all on all tables in schema public from anon, authenticated;
revoke all on all sequences in schema public from anon, authenticated;

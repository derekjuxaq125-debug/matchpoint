-- =====================================================================
-- HU-01 · Registrar usuario   /   HU-02 · Iniciar sesión con rol
-- Versión web (Santiago). Mismas reglas que UsuarioServicio y
-- AutenticacionServicio de la versión Java.
-- Los errores llevan en HINT el campo que hay que resaltar en rojo.
-- =====================================================================

-- Toda cuenta nueva nace con su cuenta de créditos en 0 (HU-07 asigna el inicial).
create or replace function public._crear_cuenta_credito()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    insert into public.cuentas_credito (usuario_id) values (new.identificacion);
    return new;
end $$;

create trigger usuarios_crear_cuenta after insert on public.usuarios
    for each row execute function public._crear_cuenta_credito();

-- Valida el token y, si se indica, el rol. Error MP401 = sesión inválida.
create or replace function public._usuario_sesion(p_token uuid, p_roles text[] default null)
returns public.usuarios
language plpgsql security definer set search_path = public as $$
declare
    v_usuario public.usuarios;
begin
    select u.* into v_usuario
      from public.sesiones s join public.usuarios u on u.identificacion = s.usuario_id
     where s.token = p_token and s.expira > now();
    if not found then
        raise exception 'La sesión no es válida o expiró. Inicie sesión de nuevo.' using errcode = 'MP401';
    end if;
    if p_roles is not null and not (v_usuario.rol = any (p_roles)) then
        raise exception 'Su rol (%) no tiene permiso para esta operación.', initcap(v_usuario.rol)
            using errcode = 'MP403';
    end if;
    return v_usuario;
end $$;

-- HU-01
create or replace function public.registrar_usuario(
    p_identificacion text, p_nombre text, p_clave text, p_confirmacion text)
returns json
language plpgsql security definer set search_path = public, extensions as $$
declare
    v_id     text := trim(coalesce(p_identificacion, ''));
    v_nombre text := trim(coalesce(p_nombre, ''));
begin
    if v_id = '' then
        raise exception 'Falta el campo obligatorio: Identificación.' using hint = 'identificacion';
    end if;
    if v_nombre = '' then
        raise exception 'Falta el campo obligatorio: Nombre.' using hint = 'nombre';
    end if;
    if coalesce(p_clave, '') = '' then
        raise exception 'Falta el campo obligatorio: Contraseña.' using hint = 'clave';
    end if;
    if v_id !~ '^[A-Za-z0-9]{5,15}$' then
        raise exception 'La identificación debe tener de 5 a 15 caracteres, solo letras y números, sin espacios.'
            using hint = 'identificacion';
    end if;
    if length(v_nombre) > 60 then
        raise exception 'El nombre admite máximo 60 caracteres.' using hint = 'nombre';
    end if;
    if length(p_clave) < 8 then
        raise exception 'La contraseña debe tener mínimo 8 caracteres.' using hint = 'clave';
    end if;
    if p_clave is distinct from p_confirmacion then
        raise exception 'La confirmación no coincide con la contraseña.' using hint = 'confirmacion';
    end if;
    if exists (select 1 from public.usuarios where lower(identificacion) = lower(v_id)) then
        raise exception 'La identificación ya está registrada. Use otra o inicie sesión.' using hint = 'identificacion';
    end if;

    insert into public.usuarios (identificacion, nombre, rol, clave_hash)
    values (v_id, v_nombre, 'PARTICIPANTE', crypt(p_clave, gen_salt('bf')));

    return json_build_object('identificacion', v_id, 'nombre', v_nombre, 'rol', 'PARTICIPANTE');
exception
    when unique_violation then
        raise exception 'La identificación ya está registrada. Use otra o inicie sesión.' using hint = 'identificacion';
end $$;

-- HU-02
create or replace function public.iniciar_sesion(p_identificacion text, p_clave text)
returns json
language plpgsql security definer set search_path = public, extensions as $$
declare
    v_usuario public.usuarios;
    v_token   uuid;
begin
    if trim(coalesce(p_identificacion, '')) = '' then
        raise exception 'Falta el campo obligatorio: Identificación.' using hint = 'identificacion';
    end if;
    if coalesce(p_clave, '') = '' then
        raise exception 'Falta el campo obligatorio: Contraseña.' using hint = 'clave';
    end if;

    select * into v_usuario from public.usuarios where lower(identificacion) = lower(trim(p_identificacion));
    -- Mismo mensaje si no existe o si la contraseña falla (no revela qué cuentas existen).
    if not found or v_usuario.clave_hash <> crypt(p_clave, v_usuario.clave_hash) then
        raise exception 'Identificación o contraseña incorrectas.';
    end if;

    delete from public.sesiones where usuario_id = v_usuario.identificacion and expira <= now();
    insert into public.sesiones (usuario_id) values (v_usuario.identificacion) returning token into v_token;

    return json_build_object('token', v_token, 'identificacion', v_usuario.identificacion,
                             'nombre', v_usuario.nombre, 'rol', v_usuario.rol);
end $$;

create or replace function public.sesion_actual(p_token uuid)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_usuario public.usuarios := public._usuario_sesion(p_token);
begin
    return json_build_object('token', p_token, 'identificacion', v_usuario.identificacion,
                             'nombre', v_usuario.nombre, 'rol', v_usuario.rol);
end $$;

create or replace function public.cerrar_sesion(p_token uuid)
returns void
language sql security definer set search_path = public as $$
    delete from public.sesiones where token = p_token;
$$;

-- Cuentas precargadas (el registro público solo crea participantes).
insert into public.usuarios (identificacion, nombre, rol, clave_hash) values
    ('ORG0001', 'Organizador MatchPoint', 'ORGANIZADOR', extensions.crypt('organizador123', extensions.gen_salt('bf'))),
    ('AUD0001', 'Auditor MatchPoint',     'AUDITOR',     extensions.crypt('auditor123',     extensions.gen_salt('bf')))
on conflict do nothing;

-- Permisos: el navegador (anon) solo ejecuta las funciones públicas.
revoke all on function public._usuario_sesion(uuid, text[]) from public, anon, authenticated;
revoke all on function public._crear_cuenta_credito() from public, anon, authenticated;
grant execute on function public.registrar_usuario(text, text, text, text) to anon, authenticated;
grant execute on function public.iniciar_sesion(text, text) to anon, authenticated;
grant execute on function public.sesion_actual(uuid) to anon, authenticated;
grant execute on function public.cerrar_sesion(uuid) to anon, authenticated;

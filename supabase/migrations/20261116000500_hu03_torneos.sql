-- =====================================================================
-- HU-03 · Crear torneo (Johan)
-- Nombre único (máx. 60), deporte de la lista, inicio no anterior a hoy
-- (hora de Colombia) y fin no anterior al inicio. Solo organizador.
-- =====================================================================

create or replace function public.crear_torneo(
    p_token uuid, p_nombre text, p_deporte text, p_inicio text, p_fin text)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_actor   public.usuarios := public._usuario_sesion(p_token, array['ORGANIZADOR']);
    v_nombre  text := trim(coalesce(p_nombre, ''));
    v_deporte text := trim(coalesce(p_deporte, ''));
    v_inicio  date;
    v_fin     date;
    v_hoy     date := (now() at time zone 'America/Bogota')::date;
    v_torneo  public.torneos;
begin
    if v_nombre = '' then raise exception 'Falta el campo obligatorio: Nombre.' using hint = 'nombre'; end if;
    if v_deporte = '' then raise exception 'Falta el campo obligatorio: Deporte.' using hint = 'deporte'; end if;
    if coalesce(trim(p_inicio), '') = '' then raise exception 'Falta el campo obligatorio: Fecha de inicio.' using hint = 'inicio'; end if;
    if coalesce(trim(p_fin), '') = '' then raise exception 'Falta el campo obligatorio: Fecha de fin.' using hint = 'fin'; end if;
    if length(v_nombre) > 60 then raise exception 'El nombre admite máximo 60 caracteres.' using hint = 'nombre'; end if;
    if v_deporte not in ('Fútbol', 'Baloncesto', 'Tenis', 'Voleibol', 'Atletismo', 'Ciclismo', 'Automovilismo', 'Otro') then
        raise exception 'Seleccione un deporte de la lista.' using hint = 'deporte';
    end if;
    begin
        v_inicio := to_date(trim(p_inicio), 'YYYY-MM-DD');
        if to_char(v_inicio, 'YYYY-MM-DD') <> trim(p_inicio) then raise exception 'x'; end if;
    exception when others then
        raise exception 'La fecha de inicio no es válida (aaaa-mm-dd).' using hint = 'inicio';
    end;
    begin
        v_fin := to_date(trim(p_fin), 'YYYY-MM-DD');
        if to_char(v_fin, 'YYYY-MM-DD') <> trim(p_fin) then raise exception 'x'; end if;
    exception when others then
        raise exception 'La fecha de fin no es válida (aaaa-mm-dd).' using hint = 'fin';
    end;
    if v_inicio < v_hoy then
        raise exception 'La fecha de inicio no puede ser anterior a la fecha actual.' using hint = 'inicio';
    end if;
    if v_fin < v_inicio then
        raise exception 'La fecha de fin no puede ser anterior a la de inicio.' using hint = 'fin';
    end if;
    if exists (select 1 from public.torneos where lower(trim(nombre)) = lower(v_nombre)) then
        raise exception 'El nombre "%" ya está en uso por otro torneo.', v_nombre using hint = 'nombre';
    end if;

    insert into public.torneos (nombre, deporte, fecha_inicio, fecha_fin, creado_por)
    values (v_nombre, v_deporte, v_inicio, v_fin, v_actor.identificacion)
    returning * into v_torneo;

    return row_to_json(v_torneo);
exception
    when unique_violation then
        raise exception 'El nombre "%" ya está en uso por otro torneo.', v_nombre using hint = 'nombre';
end $$;

-- Lista simple de torneos (organizador: lista de HU-03 y selector de HU-04).
create or replace function public.listar_torneos(p_token uuid)
returns json
language plpgsql security definer set search_path = public as $$
begin
    perform public._usuario_sesion(p_token);
    return coalesce((
        select json_agg(json_build_object(
                   'id', t.id, 'nombre', t.nombre, 'deporte', t.deporte,
                   'fecha_inicio', t.fecha_inicio, 'fecha_fin', t.fecha_fin,
                   'eventos', (select count(*) from public.eventos e where e.torneo_id = t.id))
               order by t.fecha_inicio, t.nombre)
          from public.torneos t), '[]'::json);
end $$;

grant execute on function public.crear_torneo(uuid, text, text, text, text) to anon, authenticated;
grant execute on function public.listar_torneos(uuid) to anon, authenticated;

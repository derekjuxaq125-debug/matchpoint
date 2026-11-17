-- =====================================================================
-- HU-04 · Registrar evento y opciones   /   HU-10 · Registrar pronóstico
-- Versión web (Santiago). Mismas reglas que EventoServicio y
-- PronosticoServicio de la versión Java.
-- =====================================================================

-- HU-04 ---------------------------------------------------------------
create or replace function public.registrar_evento(
    p_token uuid, p_torneo text, p_nombre text, p_inicio text, p_modalidad text,
    p_opciones text[], p_cierre text, p_costo text)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_actor    public.usuarios := public._usuario_sesion(p_token, array['ORGANIZADOR']);
    v_torneo   public.torneos;
    v_nombre   text := trim(coalesce(p_nombre, ''));
    v_inicio   timestamptz;
    v_cierre   timestamptz;
    v_costo    integer;
    v_opciones text[];
    v_min      int;
    v_max      int;
    v_evento   public.eventos;
begin
    if coalesce(trim(p_torneo), '') = '' then raise exception 'Falta el campo obligatorio: Torneo.' using hint = 'torneo'; end if;
    if v_nombre = '' then raise exception 'Falta el campo obligatorio: Nombre del evento.' using hint = 'nombre'; end if;
    if coalesce(trim(p_inicio), '') = '' then raise exception 'Falta el campo obligatorio: Fecha y hora del evento.' using hint = 'inicio'; end if;
    if coalesce(trim(p_modalidad), '') = '' then raise exception 'Falta el campo obligatorio: Modalidad de pronóstico.' using hint = 'modalidad'; end if;
    if coalesce(trim(p_cierre), '') = '' then raise exception 'Falta el campo obligatorio: Hora de cierre.' using hint = 'cierre'; end if;
    if coalesce(trim(p_costo), '') = '' then raise exception 'Falta el campo obligatorio: Costo en créditos.' using hint = 'costo'; end if;

    select * into v_torneo from public.torneos where id::text = trim(p_torneo);
    if not found then raise exception 'El torneo seleccionado no existe.' using hint = 'torneo'; end if;
    if length(v_nombre) > 60 then
        raise exception 'El nombre del evento no puede superar los 60 caracteres.' using hint = 'nombre';
    end if;

    -- Opciones: sin vacías, sin repetidas (sin distinguir mayúsculas), máx. 40 caracteres.
    select coalesce(array_agg(trim(o) order by n), '{}') into v_opciones
      from unnest(coalesce(p_opciones, '{}')) with ordinality as x(o, n) where trim(o) <> '';
    if cardinality(v_opciones) = 0 then
        raise exception 'Falta el campo obligatorio: Opciones de pronóstico.' using hint = 'opciones';
    end if;
    if (select count(distinct lower(o)) from unnest(v_opciones) o) <> cardinality(v_opciones) then
        raise exception 'Las opciones no se pueden repetir.' using hint = 'opciones';
    end if;
    if exists (select 1 from unnest(v_opciones) o where length(o) > 40) then
        raise exception 'Cada opción puede tener máximo 40 caracteres.' using hint = 'opciones';
    end if;
    case p_modalidad
        when 'GANADOR_ENFRENTAMIENTO' then v_min := 2; v_max := 2;
        when 'GANADOR_O_EMPATE'       then v_min := 3; v_max := 3;
        when 'OPCION_MULTIPLE'        then v_min := 2; v_max := 10;
        else raise exception 'Seleccione una modalidad de la lista.' using hint = 'modalidad';
    end case;
    if cardinality(v_opciones) not between v_min and v_max then
        if v_min = v_max then
            raise exception 'La modalidad elegida requiere exactamente % opciones.', v_min using hint = 'opciones';
        end if;
        raise exception 'La modalidad elegida requiere entre % y % opciones.', v_min, v_max using hint = 'opciones';
    end if;

    begin v_inicio := p_inicio::timestamptz;
    exception when others then raise exception 'La fecha y hora del evento no son válidas.' using hint = 'inicio'; end;
    begin v_cierre := p_cierre::timestamptz;
    exception when others then raise exception 'La hora de cierre no es válida.' using hint = 'cierre'; end;
    if v_inicio <= now() then raise exception 'La fecha del evento debe ser futura.' using hint = 'inicio'; end if;
    if (v_inicio at time zone 'America/Bogota')::date not between v_torneo.fecha_inicio and v_torneo.fecha_fin then
        raise exception 'La fecha del evento debe estar dentro de las fechas del torneo (% a %).',
            v_torneo.fecha_inicio, v_torneo.fecha_fin using hint = 'inicio';
    end if;
    if v_cierre <= now() then raise exception 'La hora de cierre debe ser futura.' using hint = 'cierre'; end if;
    if v_cierre > v_inicio then
        raise exception 'La hora de cierre no puede ser posterior al inicio del evento.' using hint = 'cierre';
    end if;
    if trim(p_costo) !~ '^[0-9]{1,6}$' or trim(p_costo)::int <= 0 then
        raise exception 'El costo debe ser un número entero mayor que cero.' using hint = 'costo';
    end if;
    v_costo := trim(p_costo)::int;

    if exists (select 1 from public.eventos where torneo_id = v_torneo.id
                  and lower(trim(nombre)) = lower(v_nombre) and inicio = v_inicio) then
        raise exception 'Ya existe un evento con ese nombre y fecha en el torneo.' using hint = 'nombre';
    end if;

    insert into public.eventos (torneo_id, nombre, inicio, modalidad, opciones, cierre, costo_creditos, creado_por)
    values (v_torneo.id, v_nombre, v_inicio, p_modalidad, v_opciones, v_cierre, v_costo, v_actor.identificacion)
    returning * into v_evento;
    return row_to_json(v_evento);
end $$;

-- Lista de eventos con su estado (abierto hasta la hora de cierre).
create or replace function public.listar_eventos(p_token uuid, p_solo_abiertos boolean default false)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_actor public.usuarios := public._usuario_sesion(p_token);
begin
    return coalesce((
        select json_agg(json_build_object(
                   'id', e.id, 'torneo_id', e.torneo_id, 'torneo', t.nombre, 'deporte', t.deporte,
                   'nombre', e.nombre, 'inicio', e.inicio, 'cierre', e.cierre,
                   'modalidad', e.modalidad, 'opciones', e.opciones, 'costo', e.costo_creditos,
                   'estado', case when now() < e.cierre then 'ABIERTO' else 'CERRADO' end,
                   'mi_pronostico', (select p.opcion from public.pronosticos p
                                      where p.evento_id = e.id and p.usuario_id = v_actor.identificacion))
               order by e.inicio)
          from public.eventos e join public.torneos t on t.id = e.torneo_id
         where not p_solo_abiertos or now() < e.cierre), '[]'::json);
end $$;

-- HU-10 ---------------------------------------------------------------
-- Punto de enlace con HU-11 (igual que CobroPronostico.NINGUNO en Java):
-- se llama dentro de la misma transacción, antes de confirmar el pronóstico.
create or replace function public._cobrar_pronostico(p_usuario text, p_evento public.eventos, p_pronostico uuid)
returns void language plpgsql security definer set search_path = public as $$
begin
    null; -- HU-11 (Julian) reemplaza esta función por el descuento real.
end $$;

create or replace function public.registrar_pronostico(
    p_token uuid, p_evento text, p_opcion text, p_clave_solicitud text default null)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_actor  public.usuarios := public._usuario_sesion(p_token, array['PARTICIPANTE']);
    v_evento public.eventos;
    v_opcion text;
    v_pron   public.pronosticos;
begin
    if coalesce(trim(p_evento), '') = '' then raise exception 'Falta el campo obligatorio: Evento.' using hint = 'evento'; end if;
    if coalesce(trim(p_opcion), '') = '' then raise exception 'Falta el campo obligatorio: Opción de pronóstico.' using hint = 'opcion'; end if;

    select * into v_evento from public.eventos where id::text = trim(p_evento);
    if not found then raise exception 'El evento seleccionado no existe.' using hint = 'evento'; end if;
    if now() >= v_evento.cierre then
        raise exception 'El evento ya está cerrado. No se aceptan pronósticos.' using hint = 'evento';
    end if;
    select o into v_opcion from unnest(v_evento.opciones) o where lower(o) = lower(trim(p_opcion));
    if v_opcion is null then
        raise exception 'La opción elegida no es válida para este evento.' using hint = 'opcion';
    end if;
    if exists (select 1 from public.pronosticos where usuario_id = v_actor.identificacion and evento_id = v_evento.id) then
        raise exception 'Ya registró un pronóstico en este evento.' using hint = 'evento';
    end if;

    insert into public.pronosticos (usuario_id, evento_id, opcion, clave_solicitud)
    values (v_actor.identificacion, v_evento.id, v_opcion,
            coalesce(nullif(trim(p_clave_solicitud), ''), gen_random_uuid()::text))
    returning * into v_pron;

    perform public._cobrar_pronostico(v_actor.identificacion, v_evento, v_pron.id);

    return json_build_object('id', v_pron.id, 'evento', v_evento.nombre, 'opcion', v_pron.opcion,
                             'registrado', v_pron.registrado, 'costo', v_evento.costo_creditos,
                             'saldo', (select saldo from public.cuentas_credito where usuario_id = v_actor.identificacion),
                             'repetida', false);
end $$;

revoke all on function public._cobrar_pronostico(text, public.eventos, uuid) from public, anon, authenticated;
grant execute on function public.registrar_evento(uuid, text, text, text, text, text[], text, text) to anon, authenticated;
grant execute on function public.listar_eventos(uuid, boolean) to anon, authenticated;
grant execute on function public.registrar_pronostico(uuid, text, text, text) to anon, authenticated;

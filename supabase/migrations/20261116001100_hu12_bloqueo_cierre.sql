-- =====================================================================
-- HU-12 · Bloquear pronósticos al cierre (Santiago)
-- Antes del cierre el participante puede cambiar su opción (sin nuevo cobro).
-- Desde la hora de cierre se rechazan pronósticos nuevos y cambios.
-- La regla también vive en un trigger: ni una función nueva ni un acceso
-- administrativo pueden alterar un pronóstico de un evento cerrado.
-- =====================================================================

create or replace function public._proteger_pronostico()
returns trigger language plpgsql security definer set search_path = public as $$
declare
    v_cierre timestamptz;
begin
    select cierre into v_cierre from public.eventos where id = coalesce(new.evento_id, old.evento_id);
    if tg_op = 'DELETE' then
        raise exception 'Los pronósticos registrados no se eliminan.';
    end if;
    if now() >= v_cierre then
        if tg_op = 'INSERT' then
            raise exception 'El evento ya está cerrado. No se aceptan pronósticos.' using hint = 'evento';
        end if;
        raise exception 'El evento ya está cerrado. No se aceptan cambios en el pronóstico.' using hint = 'evento';
    end if;
    if tg_op = 'UPDATE' and (new.usuario_id <> old.usuario_id or new.evento_id <> old.evento_id
                             or new.clave_solicitud <> old.clave_solicitud or new.registrado <> old.registrado) then
        raise exception 'Solo se puede cambiar la opción del pronóstico.';
    end if;
    return new;
end $$;

create trigger pronosticos_proteger_cierre before insert or update or delete on public.pronosticos
    for each row execute function public._proteger_pronostico();

create or replace function public.cambiar_pronostico(p_token uuid, p_pronostico text, p_opcion text)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_actor  public.usuarios := public._usuario_sesion(p_token, array['PARTICIPANTE']);
    v_pron   public.pronosticos;
    v_evento public.eventos;
    v_opcion text;
begin
    select * into v_pron from public.pronosticos where id::text = trim(coalesce(p_pronostico, '')) for update;
    if not found or v_pron.usuario_id <> v_actor.identificacion then
        -- HU-26: un pronóstico ajeno se trata igual que uno inexistente.
        raise exception 'El pronóstico no existe o no le pertenece.' using hint = 'evento', errcode = 'MP403';
    end if;
    select * into v_evento from public.eventos where id = v_pron.evento_id;
    if now() >= v_evento.cierre then
        raise exception 'El evento ya está cerrado. No se aceptan cambios en el pronóstico.' using hint = 'evento';
    end if;
    if coalesce(trim(p_opcion), '') = '' then
        raise exception 'Falta el campo obligatorio: Opción de pronóstico.' using hint = 'opcion';
    end if;
    select o into v_opcion from unnest(v_evento.opciones) o where lower(o) = lower(trim(p_opcion));
    if v_opcion is null then
        raise exception 'La opción elegida no es válida para este evento.' using hint = 'opcion';
    end if;

    update public.pronosticos set opcion = v_opcion, actualizado = now() where id = v_pron.id
    returning * into v_pron;
    return json_build_object('id', v_pron.id, 'evento', v_evento.nombre, 'opcion', v_pron.opcion,
                             'actualizado', v_pron.actualizado);
end $$;

-- Pronósticos del propio participante (base de HU-13 en el Sprint 3).
create or replace function public.mis_pronosticos(p_token uuid)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_actor public.usuarios := public._usuario_sesion(p_token, array['PARTICIPANTE']);
begin
    return coalesce((
        select json_agg(json_build_object(
                   'id', p.id, 'opcion', p.opcion, 'registrado', p.registrado, 'actualizado', p.actualizado,
                   'evento', json_build_object('id', e.id, 'nombre', e.nombre, 'torneo', t.nombre,
                                               'opciones', e.opciones, 'cierre', e.cierre, 'inicio', e.inicio,
                                               'costo', e.costo_creditos,
                                               'estado', case when now() < e.cierre then 'ABIERTO' else 'CERRADO' end))
               order by e.cierre)
          from public.pronosticos p
          join public.eventos e on e.id = p.evento_id
          join public.torneos t on t.id = e.torneo_id
         where p.usuario_id = v_actor.identificacion), '[]'::json);
end $$;

revoke all on function public._proteger_pronostico() from public, anon, authenticated;
grant execute on function public.cambiar_pronostico(uuid, text, text) to anon, authenticated;
grant execute on function public.mis_pronosticos(uuid) to anon, authenticated;

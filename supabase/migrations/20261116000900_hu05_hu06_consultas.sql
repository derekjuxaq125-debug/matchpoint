-- =====================================================================
-- HU-05 · Listar torneos y eventos   /   HU-06 · Ver opciones de pronóstico
-- (Johan). Solo lectura; cualquier rol con sesión válida puede consultar.
-- El estado se calcula en el momento: ABIERTO hasta la hora de cierre.
-- =====================================================================

create or replace function public.listar_torneos_eventos(p_token uuid)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_actor public.usuarios := public._usuario_sesion(p_token);
begin
    return coalesce((
        select json_agg(json_build_object(
                   'id', t.id, 'nombre', t.nombre, 'deporte', t.deporte,
                   'fecha_inicio', t.fecha_inicio, 'fecha_fin', t.fecha_fin,
                   'eventos', coalesce((
                       select json_agg(json_build_object(
                                  'id', e.id, 'torneo', t.nombre, 'nombre', e.nombre,
                                  'inicio', e.inicio, 'cierre', e.cierre,
                                  'modalidad', e.modalidad, 'opciones', e.opciones,
                                  'costo', e.costo_creditos,
                                  'estado', case when now() < e.cierre then 'ABIERTO' else 'CERRADO' end,
                                  'mi_pronostico', (select p.opcion from public.pronosticos p
                                                     where p.evento_id = e.id
                                                       and p.usuario_id = v_actor.identificacion))
                              order by e.inicio)
                         from public.eventos e where e.torneo_id = t.id), '[]'::json))
               order by t.fecha_inicio, t.nombre)
          from public.torneos t), '[]'::json);
end $$;

-- HU-06: detalle de un evento (opciones, cierre, estado).
create or replace function public.opciones_evento(p_token uuid, p_evento text)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_evento public.eventos;
begin
    perform public._usuario_sesion(p_token);
    select * into v_evento from public.eventos where id::text = trim(coalesce(p_evento, ''));
    if not found then
        raise exception 'El evento seleccionado no existe.' using hint = 'evento';
    end if;
    return json_build_object('id', v_evento.id, 'nombre', v_evento.nombre, 'modalidad', v_evento.modalidad,
                             'opciones', v_evento.opciones, 'cierre', v_evento.cierre,
                             'costo', v_evento.costo_creditos,
                             'estado', case when now() < v_evento.cierre then 'ABIERTO' else 'CERRADO' end);
end $$;

grant execute on function public.listar_torneos_eventos(uuid) to anon, authenticated;
grant execute on function public.opciones_evento(uuid, text) to anon, authenticated;

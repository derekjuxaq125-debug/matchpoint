-- =====================================================================
-- HU-08 · Consultar saldo (Johan)
-- Devuelve el saldo vigente, la suma de los movimientos y si coinciden.
-- Solo el propio participante: el usuario sale del token, no de un parámetro.
-- =====================================================================

create or replace function public.consultar_saldo(p_token uuid)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_actor public.usuarios := public._usuario_sesion(p_token, array['PARTICIPANTE']);
    v_saldo integer;
    v_suma  bigint;
begin
    select saldo into v_saldo from public.cuentas_credito where usuario_id = v_actor.identificacion;
    select coalesce(sum(cantidad), 0) into v_suma from public.movimientos where usuario_id = v_actor.identificacion;
    return json_build_object(
        'usuario', v_actor.identificacion,
        'saldo', v_saldo,
        'suma_movimientos', v_suma,
        'coincide', v_saldo = v_suma,
        'movimientos', coalesce((
            select json_agg(json_build_object(
                       'id', m.id, 'operacion', m.operacion, 'tipo', m.tipo, 'cantidad', m.cantidad,
                       'saldo_resultante', m.saldo_resultante, 'origen_tipo', m.origen_tipo,
                       'origen_id', m.origen_id, 'justificacion', m.justificacion,
                       'registrado', m.registrado,
                       'detalle', case when m.origen_tipo = 'PRONOSTICO' then
                                     (select e.nombre || ' → ' || p.opcion from public.pronosticos p
                                        join public.eventos e on e.id = p.evento_id where p.id::text = m.origen_id)
                                  end)
                   order by m.id)
              from public.movimientos m where m.usuario_id = v_actor.identificacion), '[]'::json));
end $$;

grant execute on function public.consultar_saldo(uuid) to anon, authenticated;

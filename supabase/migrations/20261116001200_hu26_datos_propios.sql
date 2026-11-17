-- =====================================================================
-- HU-26 · Acceso solo a datos propios (Santiago)
-- Capas de protección:
--   1. RLS activo sin políticas + REVOKE: la llave pública no lee ninguna tabla.
--   2. Las funciones del participante toman el usuario del TOKEN, nunca de un
--      parámetro (consultar_saldo, mis_pronosticos, cambiar_pronostico...).
--   3. Esta función es la única que recibe un usuario: el participante solo
--      puede pedirse a sí mismo; el auditor puede consultar a cualquiera
--      (solo lectura) para atender reclamaciones.
-- =====================================================================

create or replace function public.consultar_participacion(p_token uuid, p_usuario text)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_actor   public.usuarios := public._usuario_sesion(p_token, array['PARTICIPANTE', 'AUDITOR']);
    v_usuario public.usuarios;
begin
    if v_actor.rol = 'PARTICIPANTE' and lower(trim(coalesce(p_usuario, ''))) <> lower(v_actor.identificacion) then
        raise exception 'No tiene permiso para ver ni gestionar datos de otro usuario.' using errcode = 'MP403';
    end if;
    select * into v_usuario from public.usuarios where lower(identificacion) = lower(trim(coalesce(p_usuario, '')));
    if not found then
        raise exception 'El usuario no existe.' using hint = 'usuario';
    end if;
    return json_build_object(
        'usuario', v_usuario.identificacion,
        'nombre', v_usuario.nombre,
        'saldo', (select saldo from public.cuentas_credito where usuario_id = v_usuario.identificacion),
        'suma_movimientos', (select coalesce(sum(cantidad), 0) from public.movimientos where usuario_id = v_usuario.identificacion),
        'pronosticos', coalesce((select json_agg(json_build_object('evento', e.nombre, 'opcion', p.opcion,
                                                                   'registrado', p.registrado) order by p.registrado)
                                   from public.pronosticos p join public.eventos e on e.id = p.evento_id
                                  where p.usuario_id = v_usuario.identificacion), '[]'::json),
        'movimientos', coalesce((select json_agg(json_build_object('tipo', m.tipo, 'cantidad', m.cantidad,
                                                                   'saldo_resultante', m.saldo_resultante,
                                                                   'registrado', m.registrado) order by m.id)
                                   from public.movimientos m where m.usuario_id = v_usuario.identificacion), '[]'::json));
end $$;

grant execute on function public.consultar_participacion(uuid, text) to anon, authenticated;

-- Refuerzo: ninguna tabla queda expuesta aunque se creen tablas nuevas después.
revoke all on all tables in schema public from anon, authenticated;
alter default privileges in schema public revoke all on tables from anon, authenticated;

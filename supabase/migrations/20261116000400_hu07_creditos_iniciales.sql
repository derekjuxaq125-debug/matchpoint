-- =====================================================================
-- HU-07 · Asignar créditos iniciales (Julian)
-- Entero de 1 a 10 000, una sola vez por usuario, registrado como el primer
-- movimiento del libro (HU-09).
-- =====================================================================

create or replace function public.listar_usuarios_creditos(p_token uuid)
returns json
language plpgsql security definer set search_path = public as $$
begin
    perform public._usuario_sesion(p_token, array['ORGANIZADOR']);
    return coalesce((
        select json_agg(json_build_object(
                   'identificacion', u.identificacion,
                   'nombre', u.nombre,
                   'rol', u.rol,
                   'saldo', c.saldo,
                   'saldo_inicial', (select m.cantidad from public.movimientos m
                                      where m.usuario_id = u.identificacion and m.tipo = 'ASIGNACION_INICIAL'
                                      order by m.id limit 1))
               order by u.creado)
          from public.usuarios u join public.cuentas_credito c on c.usuario_id = u.identificacion
         where u.rol = 'PARTICIPANTE'), '[]'::json);
end $$;

create or replace function public.asignar_creditos_iniciales(p_token uuid, p_usuario text, p_cantidad text)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_actor    public.usuarios := public._usuario_sesion(p_token, array['ORGANIZADOR']);
    v_cuenta   public.cuentas_credito;
    v_cantidad integer;
    v_mov      public.movimientos;
begin
    if coalesce(trim(p_usuario), '') = '' then
        raise exception 'Seleccione el usuario al que se asignan los créditos.' using hint = 'usuario';
    end if;
    if coalesce(trim(p_cantidad), '') = '' then
        raise exception 'Falta el campo obligatorio: Cantidad de créditos.' using hint = 'cantidad';
    end if;
    if trim(p_cantidad) !~ '^-?[0-9]+$' then
        raise exception 'La cantidad debe ser un número entero.' using hint = 'cantidad';
    end if;
    if length(trim(p_cantidad)) > 9 or trim(p_cantidad)::bigint > 10000 then
        raise exception 'La cantidad máxima de créditos iniciales es 10000.' using hint = 'cantidad';
    end if;
    v_cantidad := trim(p_cantidad)::integer;
    if v_cantidad <= 0 then
        raise exception 'La cantidad debe ser un entero mayor que cero.' using hint = 'cantidad';
    end if;

    -- Bloquea la cuenta: dos asignaciones simultáneas se atienden una tras otra.
    select c.* into v_cuenta
      from public.cuentas_credito c join public.usuarios u on u.identificacion = c.usuario_id
     where lower(c.usuario_id) = lower(trim(p_usuario))
       for update of c;
    if not found then
        raise exception 'El usuario seleccionado no está registrado.' using hint = 'usuario';
    end if;
    if v_cuenta.inicial_asignado then
        raise exception 'El usuario ya tiene saldo inicial asignado; no se puede asignar de nuevo y su saldo no cambia.'
            using hint = 'usuario';
    end if;

    v_mov := public._registrar_movimiento(v_cuenta.usuario_id, 'ASIGNACION_INICIAL', v_cantidad,
                 'USUARIO', v_cuenta.usuario_id, 'INICIAL-' || lower(v_cuenta.usuario_id), v_actor.identificacion);
    update public.cuentas_credito set inicial_asignado = true where usuario_id = v_cuenta.usuario_id;

    return json_build_object('usuario', v_cuenta.usuario_id, 'cantidad', v_mov.cantidad,
                             'saldo', v_mov.saldo_resultante, 'operacion', v_mov.operacion,
                             'registrado', v_mov.registrado);
end $$;

grant execute on function public.listar_usuarios_creditos(uuid) to anon, authenticated;
grant execute on function public.asignar_creditos_iniciales(uuid, text, text) to anon, authenticated;

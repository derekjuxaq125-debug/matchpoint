-- =====================================================================
-- HU-11 · Descontar créditos al participar (Julian)
-- Reemplaza el punto de enlace _cobrar_pronostico de HU-10. Corre dentro de
-- la transacción de registrar_pronostico: si el saldo no alcanza se lanza el
-- error y PostgreSQL deshace también el pronóstico (todo o nada).
-- =====================================================================

create or replace function public._cobrar_pronostico(p_usuario text, p_evento public.eventos, p_pronostico uuid)
returns void language plpgsql security definer set search_path = public as $$
declare
    v_saldo integer;
begin
    -- FOR UPDATE: bloquea el saldo del usuario hasta que termine la transacción (base de HU-15).
    select saldo into v_saldo from public.cuentas_credito where usuario_id = p_usuario for update;
    if v_saldo < p_evento.costo_creditos then
        raise exception 'Saldo insuficiente: tiene % créditos y el pronóstico cuesta %. No se registró nada.',
            v_saldo, p_evento.costo_creditos using hint = 'saldo', errcode = 'MP402';
    end if;
    perform public._registrar_movimiento(p_usuario, 'PARTICIPACION', -p_evento.costo_creditos,
        'PRONOSTICO', p_pronostico::text, 'COBRO-' || p_pronostico::text, p_usuario);
end $$;

revoke all on function public._cobrar_pronostico(text, public.eventos, uuid) from public, anon, authenticated;

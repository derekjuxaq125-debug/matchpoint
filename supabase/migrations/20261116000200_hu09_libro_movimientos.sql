-- =====================================================================
-- HU-09 · Registrar movimientos trazables (Julian)
-- Todo movimiento confirmado guarda operación, usuario, tipo, cantidad,
-- fecha y origen, y no se puede modificar ni eliminar.
-- =====================================================================

-- El libro es de solo agregado: se bloquea UPDATE y DELETE incluso para
-- funciones internas. Las correcciones se hacen con AJUSTE o REVERSION.
create or replace function public._movimientos_inmutables()
returns trigger language plpgsql as $$
begin
    raise exception 'Los movimientos confirmados no se pueden modificar ni eliminar. Use un ajuste o una reversión.';
end $$;

create trigger movimientos_no_update before update on public.movimientos
    for each row execute function public._movimientos_inmutables();
create trigger movimientos_no_delete before delete on public.movimientos
    for each row execute function public._movimientos_inmutables();
create trigger movimientos_no_truncate before truncate on public.movimientos
    for each statement execute function public._movimientos_inmutables();

-- Único punto que cambia saldos. Se ejecuta dentro de la transacción de quien
-- lo llama: si algo falla después, el movimiento y el saldo se deshacen juntos.
create or replace function public._registrar_movimiento(
    p_usuario        text,
    p_tipo           text,
    p_cantidad       integer,
    p_origen_tipo    text,
    p_origen_id      text,
    p_clave          text,
    p_actor          text,
    p_justificacion  text default null)
returns public.movimientos
language plpgsql security definer set search_path = public as $$
declare
    v_saldo integer;
    v_mov   public.movimientos;
begin
    if p_cantidad is null or p_cantidad = 0 then
        raise exception 'La cantidad del movimiento no puede ser cero.';
    end if;
    if p_tipo in ('AJUSTE', 'REVERSION') and coalesce(trim(p_justificacion), '') = '' then
        raise exception 'Los ajustes y reversiones exigen una justificación.';
    end if;

    select saldo into v_saldo from public.cuentas_credito where usuario_id = p_usuario;
    if not found then
        raise exception 'El usuario % no tiene cuenta de créditos.', p_usuario;
    end if;

    insert into public.movimientos (usuario_id, tipo, cantidad, saldo_resultante, origen_tipo,
                                    origen_id, clave_idempotencia, justificacion, registrado_por)
    values (p_usuario, p_tipo, p_cantidad, v_saldo + p_cantidad, p_origen_tipo,
            p_origen_id, p_clave, p_justificacion, p_actor)
    returning * into v_mov;

    update public.cuentas_credito
       set saldo = v_mov.saldo_resultante, actualizado = now()
     where usuario_id = p_usuario;

    return v_mov;
end $$;

revoke all on function public._registrar_movimiento(text, text, integer, text, text, text, text, text) from public, anon, authenticated;
revoke all on function public._movimientos_inmutables() from public, anon, authenticated;

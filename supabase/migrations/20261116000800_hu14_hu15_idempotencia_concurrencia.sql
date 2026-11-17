-- =====================================================================
-- HU-14 · Evitar operaciones duplicadas   /   HU-15 · Controlar concurrencia
-- (Julian)
--
-- HU-14: el navegador genera una clave_solicitud por intento de pronóstico y
--        la reutiliza en los reintentos. Si ya existe, se devuelve el resultado
--        original con repetida = true: ni segundo pronóstico ni segundo cobro.
--        Los movimientos también son idempotentes por clave_idempotencia.
-- HU-15: lo primero que hace la operación es bloquear la fila del saldo del
--        usuario (SELECT ... FOR UPDATE). Dos solicitudes simultáneas del mismo
--        usuario se atienden en fila; la segunda ve el saldo ya descontado.
--        Además el CHECK (saldo >= 0) es una última barrera en la tabla.
-- =====================================================================

-- Movimientos idempotentes: la misma clave devuelve el movimiento ya registrado.
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

    -- HU-15: bloqueo de la fila del saldo durante toda la transacción.
    select saldo into v_saldo from public.cuentas_credito where usuario_id = p_usuario for update;
    if not found then
        raise exception 'El usuario % no tiene cuenta de créditos.', p_usuario;
    end if;

    -- HU-14: ¿ya se registró esta operación?
    select * into v_mov from public.movimientos where clave_idempotencia = p_clave;
    if found then
        return v_mov;
    end if;

    if v_saldo + p_cantidad < 0 then
        raise exception 'Saldo insuficiente: tiene % créditos y la operación requiere %.', v_saldo, -p_cantidad
            using errcode = 'MP402', hint = 'saldo';
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

create or replace function public.registrar_pronostico(
    p_token uuid, p_evento text, p_opcion text, p_clave_solicitud text default null)
returns json
language plpgsql security definer set search_path = public as $$
declare
    v_actor  public.usuarios := public._usuario_sesion(p_token, array['PARTICIPANTE']);
    v_clave  text := coalesce(nullif(trim(p_clave_solicitud), ''), gen_random_uuid()::text);
    v_evento public.eventos;
    v_opcion text;
    v_pron   public.pronosticos;
    v_saldo  integer;
begin
    -- HU-15: primero se toma el candado del saldo del usuario.
    select saldo into v_saldo from public.cuentas_credito where usuario_id = v_actor.identificacion for update;

    -- HU-14: solicitud repetida → resultado original, sin volver a cobrar.
    select * into v_pron from public.pronosticos where clave_solicitud = v_clave;
    if found then
        if v_pron.usuario_id <> v_actor.identificacion then
            raise exception 'La clave de solicitud no es válida.';
        end if;
        select * into v_evento from public.eventos where id = v_pron.evento_id;
        return json_build_object('id', v_pron.id, 'evento', v_evento.nombre, 'opcion', v_pron.opcion,
                                 'registrado', v_pron.registrado, 'costo', v_evento.costo_creditos,
                                 'saldo', v_saldo, 'repetida', true);
    end if;

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
    values (v_actor.identificacion, v_evento.id, v_opcion, v_clave)
    returning * into v_pron;

    perform public._cobrar_pronostico(v_actor.identificacion, v_evento, v_pron.id);

    return json_build_object('id', v_pron.id, 'evento', v_evento.nombre, 'opcion', v_pron.opcion,
                             'registrado', v_pron.registrado, 'costo', v_evento.costo_creditos,
                             'saldo', (select saldo from public.cuentas_credito where usuario_id = v_actor.identificacion),
                             'repetida', false);
exception
    when unique_violation then
        raise exception 'Ya registró un pronóstico en este evento.' using hint = 'evento';
end $$;

revoke all on function public._registrar_movimiento(text, text, integer, text, text, text, text, text) from public, anon, authenticated;
grant execute on function public.registrar_pronostico(uuid, text, text, text) to anon, authenticated;

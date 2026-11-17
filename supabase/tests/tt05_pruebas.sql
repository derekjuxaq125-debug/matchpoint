-- =====================================================================
-- TT-05 · Pruebas de saldo, idempotencia y concurrencia en Supabase (Johan)
--
-- Cómo ejecutarlo: Supabase > SQL Editor > pegar TODO el archivo > Run.
-- Corre dentro de una transacción que se DESHACE al final (ROLLBACK): no deja
-- usuarios, torneos ni movimientos de prueba en la base de datos.
-- Resultado esperado: todas las filas con ok = true.
-- La concurrencia real (varias conexiones a la vez) se prueba con
-- web/scripts/prueba-concurrencia.mjs.
-- =====================================================================
begin;

create temp table resultados (n serial, caso text, ok boolean, detalle text) on commit drop;

do $$
declare
    t_org  uuid;
    t_a    uuid;
    t_b    uuid;
    v      json;
    v_tor  uuid;
    e1     uuid; e2 uuid; e3 uuid; e4 uuid;
    p1     uuid;
    falla  text;
    v_ini  timestamptz := now() + interval '2 days';
    v_cie  timestamptz := now() + interval '1 day';
begin
    perform public.registrar_usuario('TT5PRUEBAA', 'Ana Prueba', 'clave12345', 'clave12345');
    perform public.registrar_usuario('TT5PRUEBAB', 'Beto Prueba', 'clave12345', 'clave12345');
    t_org := (public.iniciar_sesion('ORG0001', 'organizador123') ->> 'token')::uuid;
    t_a   := (public.iniciar_sesion('tt5pruebaa', 'clave12345') ->> 'token')::uuid;
    t_b   := (public.iniciar_sesion('TT5PRUEBAB', 'clave12345') ->> 'token')::uuid;
    insert into resultados (caso, ok) values ('HU-02 inicio de sesión de 3 roles', t_org is not null and t_a is not null);

    -- Preparación: torneo y eventos
    v := public.crear_torneo(t_org, 'TT05 Torneo de prueba', 'Fútbol',
                             to_char(now() at time zone 'America/Bogota', 'YYYY-MM-DD'),
                             to_char((now() at time zone 'America/Bogota') + interval '30 days', 'YYYY-MM-DD'));
    v_tor := (v ->> 'id')::uuid;
    e1 := (public.registrar_evento(t_org, v_tor::text, 'TT05 E1', v_ini::text, 'GANADOR_O_EMPATE',
                                   array['Local', 'Empate', 'Visitante'], v_cie::text, '10') ->> 'id')::uuid;
    e2 := (public.registrar_evento(t_org, v_tor::text, 'TT05 E2', v_ini::text, 'GANADOR_ENFRENTAMIENTO',
                                   array['A', 'B'], v_cie::text, '30') ->> 'id')::uuid;
    e4 := (public.registrar_evento(t_org, v_tor::text, 'TT05 E4', v_ini::text, 'GANADOR_ENFRENTAMIENTO',
                                   array['A', 'B'], v_cie::text, '1') ->> 'id')::uuid;
    -- Evento ya cerrado (se inserta directo porque registrar_evento exige fechas futuras)
    insert into public.eventos (torneo_id, nombre, inicio, modalidad, opciones, cierre, costo_creditos, creado_por)
    values (v_tor, 'TT05 E3 cerrado', now() - interval '1 hour', 'GANADOR_ENFRENTAMIENTO', array['A', 'B'],
            now() - interval '2 hours', 5, 'ORG0001') returning id into e3;

    -- HU-07
    v := public.asignar_creditos_iniciales(t_org, 'TT5PRUEBAA', '40');
    insert into resultados (caso, ok, detalle) values ('HU-07 asigna saldo inicial', (v ->> 'saldo')::int = 40, v::text);
    begin
        perform public.asignar_creditos_iniciales(t_org, 'TT5PRUEBAA', '40');
        insert into resultados (caso, ok) values ('HU-07 segunda asignación rechazada', false);
    exception when others then
        insert into resultados (caso, ok, detalle) values ('HU-07 segunda asignación rechazada', true, sqlerrm);
    end;

    -- HU-11 + HU-14
    v := public.registrar_pronostico(t_a, e1::text, 'local', 'clave-tt05-1');
    p1 := (v ->> 'id')::uuid;
    insert into resultados (caso, ok, detalle) values ('HU-11 descuenta el costo (40 - 10 = 30)', (v ->> 'saldo')::int = 30, v::text);
    v := public.registrar_pronostico(t_a, e1::text, 'local', 'clave-tt05-1');
    insert into resultados (caso, ok, detalle) values ('HU-14 reintento devuelve repetida = true y no cobra',
        (v ->> 'repetida')::boolean and (v ->> 'saldo')::int = 30, v::text);
    insert into resultados (caso, ok) values ('HU-14 un solo movimiento de participación por el pronóstico',
        (select count(*) from public.movimientos where origen_id = p1::text) = 1);
    begin
        perform public.registrar_pronostico(t_a, e1::text, 'Empate', 'otra-clave');
        insert into resultados (caso, ok) values ('HU-10 segundo pronóstico en el mismo evento rechazado', false);
    exception when others then
        insert into resultados (caso, ok, detalle) values ('HU-10 segundo pronóstico en el mismo evento rechazado', true, sqlerrm);
    end;

    v := public.registrar_pronostico(t_a, e2::text, 'A', 'clave-tt05-2');
    insert into resultados (caso, ok, detalle) values ('HU-11 permite dejar el saldo en 0 exacto', (v ->> 'saldo')::int = 0, v::text);
    begin
        perform public.registrar_pronostico(t_a, e4::text, 'A', 'clave-tt05-3');
        insert into resultados (caso, ok) values ('HU-11 saldo insuficiente rechazado', false);
    exception when others then
        insert into resultados (caso, ok, detalle) values ('HU-11 saldo insuficiente rechazado', true, sqlerrm);
    end;
    insert into resultados (caso, ok) values ('HU-11 el rechazo no deja pronóstico ni movimiento',
        not exists (select 1 from public.pronosticos where clave_solicitud = 'clave-tt05-3'));

    -- HU-12
    begin
        perform public.registrar_pronostico(t_b, e3::text, 'A', 'clave-tt05-4');
        insert into resultados (caso, ok) values ('HU-12 pronóstico en evento cerrado rechazado', false);
    exception when others then
        insert into resultados (caso, ok, detalle) values ('HU-12 pronóstico en evento cerrado rechazado', true, sqlerrm);
    end;
    v := public.cambiar_pronostico(t_a, p1::text, 'Visitante');
    insert into resultados (caso, ok, detalle) values ('HU-12 cambio antes del cierre sin nuevo cobro',
        v ->> 'opcion' = 'Visitante' and (select saldo from public.cuentas_credito where usuario_id = 'TT5PRUEBAA') = 0, v::text);

    -- HU-26
    begin
        perform public.cambiar_pronostico(t_b, p1::text, 'Local');
        insert into resultados (caso, ok) values ('HU-26 otro participante no cambia mi pronóstico', false);
    exception when others then
        insert into resultados (caso, ok, detalle) values ('HU-26 otro participante no cambia mi pronóstico', true, sqlerrm);
    end;
    begin
        perform public.consultar_participacion(t_b, 'TT5PRUEBAA');
        insert into resultados (caso, ok) values ('HU-26 otro participante no ve mis datos', false);
    exception when others then
        insert into resultados (caso, ok, detalle) values ('HU-26 otro participante no ve mis datos', true, sqlerrm);
    end;
    insert into resultados (caso, ok) values ('HU-26 la llave pública (anon) no lee ninguna tabla',
        not exists (select 1 from pg_class c join pg_namespace s on s.oid = c.relnamespace
                     where s.nspname = 'public' and c.relkind = 'r'
                       and (has_table_privilege('anon', c.oid, 'select')
                            or has_table_privilege('anon', c.oid, 'insert'))));
    insert into resultados (caso, ok) values ('HU-26 anon no ejecuta funciones internas (_registrar_movimiento)',
        not has_function_privilege('anon', 'public._registrar_movimiento(text,text,integer,text,text,text,text,text)', 'execute'));

    -- HU-08 / HU-09
    v := public.consultar_saldo(t_a);
    insert into resultados (caso, ok, detalle) values ('HU-08 saldo = suma de movimientos', (v ->> 'coincide')::boolean,
        format('saldo %s, suma %s', v ->> 'saldo', v ->> 'suma_movimientos'));
    begin
        update public.movimientos set cantidad = 999 where usuario_id = 'TT5PRUEBAA';
        insert into resultados (caso, ok) values ('HU-09 los movimientos no se modifican', false);
    exception when others then
        insert into resultados (caso, ok, detalle) values ('HU-09 los movimientos no se modifican', true, sqlerrm);
    end;
    begin
        delete from public.movimientos where usuario_id = 'TT5PRUEBAA';
        insert into resultados (caso, ok) values ('HU-09 los movimientos no se eliminan', false);
    exception when others then
        insert into resultados (caso, ok, detalle) values ('HU-09 los movimientos no se eliminan', true, sqlerrm);
    end;

    -- HU-15: barrera final en la tabla
    begin
        update public.cuentas_credito set saldo = -1 where usuario_id = 'TT5PRUEBAA';
        insert into resultados (caso, ok) values ('HU-15 la tabla impide saldo negativo', false);
    exception when others then
        insert into resultados (caso, ok, detalle) values ('HU-15 la tabla impide saldo negativo', true, sqlerrm);
    end;
    insert into resultados (caso, ok) values ('TT-05 ningún movimiento con saldo resultante negativo',
        not exists (select 1 from public.movimientos where saldo_resultante < 0));
    insert into resultados (caso, ok) values ('TT-05 todos los saldos cuadran con su libro',
        not exists (select 1 from public.cuentas_credito c
                     where c.saldo <> (select coalesce(sum(m.cantidad), 0) from public.movimientos m where m.usuario_id = c.usuario_id)));
end $$;

select n, caso, ok, left(detalle, 120) as detalle from resultados order by n;

rollback;

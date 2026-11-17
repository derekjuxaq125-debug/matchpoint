// TT-05 · Prueba de concurrencia REAL contra Supabase (Johan).
// Lanza solicitudes simultáneas desde varias conexiones HTTP, como pasaría con
// un doble clic, un reintento por mala conexión o dos pestañas abiertas.
//
// Uso (desde la carpeta web):  npm run prueba:concurrencia
// Requiere eventos abiertos (cree al menos 2 con el organizador) y deja en la
// base un participante de prueba "TTC" + número, que sirve como evidencia.
import { readFileSync } from 'node:fs';
import { createClient } from '@supabase/supabase-js';

function leerEnv() {
  const env = { ...process.env };
  for (const archivo of ['.env.local', '.env.development']) {
    try {
      for (const linea of readFileSync(new URL(`../${archivo}`, import.meta.url), 'utf8').split('\n')) {
        const m = linea.match(/^\s*(VITE_[A-Z_]+)\s*=\s*(.+)\s*$/);
        if (m && !env[m[1]]) env[m[1]] = m[2];
      }
    } catch { /* archivo opcional */ }
  }
  return env;
}

const env = leerEnv();
const supabase = createClient(env.VITE_SUPABASE_URL, env.VITE_SUPABASE_ANON_KEY, { auth: { persistSession: false } });

async function rpc(nombre, params) {
  const { data, error } = await supabase.rpc(nombre, params);
  if (error) throw new Error(error.message);
  return data;
}

let fallas = 0;
function comprobar(caso, ok, detalle = '') {
  console.log(`${ok ? 'OK   ' : 'FALLA'} ${caso}${detalle ? ` — ${detalle}` : ''}`);
  if (!ok) fallas += 1;
}

const id = `TTC${Date.now().toString().slice(-9)}`;
const SALDO = 30;
await rpc('registrar_usuario', { p_identificacion: id, p_nombre: 'Prueba concurrencia TT-05', p_clave: 'prueba12345', p_confirmacion: 'prueba12345' });
const org = await rpc('iniciar_sesion', { p_identificacion: 'ORG0001', p_clave: 'organizador123' });
const yo = await rpc('iniciar_sesion', { p_identificacion: id, p_clave: 'prueba12345' });

// 1) Diez asignaciones iniciales simultáneas: solo una debe aplicarse (HU-07 + HU-15).
const asignaciones = await Promise.allSettled(Array.from({ length: 10 }, () =>
  rpc('asignar_creditos_iniciales', { p_token: org.token, p_usuario: id, p_cantidad: String(SALDO) })));
comprobar('10 asignaciones simultáneas → 1 aplicada', asignaciones.filter((r) => r.status === 'fulfilled').length === 1);

const abiertos = await rpc('listar_eventos', { p_token: yo.token, p_solo_abiertos: true });
if (abiertos.length < 2) {
  console.log('Se necesitan al menos 2 eventos abiertos. Créelos con ORG0001 y vuelva a ejecutar.');
  process.exit(1);
}

// 2) La MISMA solicitud enviada 15 veces a la vez (HU-14).
const [primero, ...resto] = abiertos;
const clave = `doble-clic-${id}`;
const iguales = await Promise.allSettled(Array.from({ length: 15 }, () =>
  rpc('registrar_pronostico', { p_token: yo.token, p_evento: primero.id, p_opcion: primero.opciones[0], p_clave_solicitud: clave })));
const ok = iguales.filter((r) => r.status === 'fulfilled').map((r) => r.value);
comprobar('15 envíos de la misma solicitud → 1 nuevo y el resto "repetida"',
  ok.filter((r) => !r.repetida).length === 1 && ok.filter((r) => r.repetida).length === ok.length - 1,
  `${ok.length} respuestas`);

// 3) Pronósticos simultáneos en todos los demás eventos (HU-11 + HU-15).
await Promise.allSettled(resto.map((e) =>
  rpc('registrar_pronostico', { p_token: yo.token, p_evento: e.id, p_opcion: e.opciones[0], p_clave_solicitud: `${id}-${e.id}` })));

const saldo = await rpc('consultar_saldo', { p_token: yo.token });
const cobrado = -saldo.movimientos.filter((m) => m.tipo === 'PARTICIPACION').reduce((s, m) => s + m.cantidad, 0);
comprobar('El saldo nunca queda negativo', saldo.saldo >= 0 && saldo.movimientos.every((m) => m.saldo_resultante >= 0), `saldo final ${saldo.saldo}`);
comprobar('Lo cobrado no supera el saldo inicial', cobrado <= SALDO, `cobrado ${cobrado} de ${SALDO}`);
comprobar('Saldo = suma de movimientos (HU-08)', saldo.coincide, `saldo ${saldo.saldo}, suma ${saldo.suma_movimientos}`);
const pron = await rpc('mis_pronosticos', { p_token: yo.token });
comprobar('Un movimiento de participación por cada pronóstico', pron.length === saldo.movimientos.filter((m) => m.tipo === 'PARTICIPACION').length,
  `${pron.length} pronósticos`);

console.log(fallas === 0 ? '\nTT-05: todas las comprobaciones pasaron.' : `\nTT-05: ${fallas} comprobación(es) fallaron.`);
process.exit(fallas === 0 ? 0 : 1);

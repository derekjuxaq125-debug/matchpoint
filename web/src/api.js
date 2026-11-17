// Cliente de Supabase. El navegador nunca toca las tablas: solo llama funciones
// RPC que validan la sesión y el rol en la base de datos (HU-02, HU-26).
import { createClient } from '@supabase/supabase-js';

const url = import.meta.env.VITE_SUPABASE_URL;
const llave = import.meta.env.VITE_SUPABASE_ANON_KEY;

export const configurado = Boolean(url && llave);
export const supabase = configurado
  ? createClient(url, llave, { auth: { persistSession: false } })
  : null;

const CLAVE_SESION = 'matchpoint.sesion';

export const sesion = {
  leer() {
    try { return JSON.parse(localStorage.getItem(CLAVE_SESION)); } catch { return null; }
  },
  guardar(datos) {
    try { localStorage.setItem(CLAVE_SESION, JSON.stringify(datos)); } catch { /* sin almacenamiento */ }
  },
  borrar() {
    try { localStorage.removeItem(CLAVE_SESION); } catch { /* sin almacenamiento */ }
  },
};

/** Error de negocio devuelto por la base de datos. `campo` indica qué resaltar. */
export class ErrorApi extends Error {
  constructor({ message, hint, code }) {
    super(message || 'No se pudo completar la operación.');
    this.campo = hint || null;
    this.codigo = code || null;
  }
}

/**
 * Llama una función RPC. Por defecto agrega el token de la sesión (p_token).
 * Si la sesión expiró (MP401) avisa a la aplicación para volver al inicio de sesión.
 */
export async function rpc(nombre, parametros = {}, { conSesion = true } = {}) {
  if (!supabase) throw new ErrorApi({ message: 'Falta configurar Supabase (ver web/.env.example).' });
  const params = conSesion ? { p_token: sesion.leer()?.token ?? null, ...parametros } : parametros;
  let respuesta;
  try {
    respuesta = await supabase.rpc(nombre, params);
  } catch {
    throw new ErrorApi({ message: 'No hay conexión con el servidor. Intente de nuevo.' });
  }
  const { data, error } = respuesta;
  if (error) {
    const err = new ErrorApi(error);
    if (err.codigo === 'MP401') {
      sesion.borrar();
      window.dispatchEvent(new CustomEvent('matchpoint:sesion-expirada', { detail: err.message }));
    }
    throw err;
  }
  return data;
}

/** Llave única para una solicitud; se reutiliza en los reintentos (HU-14). */
export function nuevaClaveSolicitud() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

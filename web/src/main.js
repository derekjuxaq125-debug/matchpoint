// Punto de entrada: decide entre la pantalla de acceso y la aplicación según
// la sesión, y muestra solo las opciones del rol del usuario (HU-02).
import './styles.css';
import { configurado, rpc, sesion } from './api.js';
import { h, aviso } from './ui.js';
import { pantallaAcceso } from './vistas/acceso.js';
import { VISTAS, PENDIENTES } from './vistas/index.js';

const app = document.getElementById('app');
export const ROLES = { PARTICIPANTE: 'Participante', ORGANIZADOR: 'Organizador', AUDITOR: 'Auditor' };

let usuarioActual = null;

const vistasDe = (rol) => VISTAS.filter((v) => v.roles.includes(rol));

function mostrarAcceso(mensaje) {
  usuarioActual = null;
  app.replaceChildren(pantallaAcceso({
    mensaje,
    alEntrar(usuario) {
      sesion.guardar(usuario);
      location.hash = '#/inicio';
      mostrarApp(usuario);
    },
  }));
}

async function cerrarSesion() {
  try { await rpc('cerrar_sesion'); } catch { /* la sesión ya no existía */ }
  sesion.borrar();
  location.hash = '';
  mostrarAcceso();
}

function mostrarApp(usuario) {
  usuarioActual = usuario;
  const vistas = vistasDe(usuario.rol);
  const contenido = h('main', { class: 'contenido', id: 'contenido', tabindex: '-1' });

  const enlaces = vistas.map((v) => h('a', { href: `#/${v.id}`, 'data-vista': v.id, class: 'menu-item' },
    h('span', {}, v.titulo), v.hu ? h('small', {}, v.hu) : null));
  const pendientes = (PENDIENTES[usuario.rol] || []).map((p) => h('span', { class: 'menu-item pendiente', title: `Se habilita con ${p.hu}` },
    h('span', {}, p.titulo), h('small', {}, p.sprint || p.hu)));

  const barra = h('aside', { class: 'barra' },
    h('div', { class: 'marca' }, h('span', { class: 'marca-icono', 'aria-hidden': 'true' }), h('span', {}, 'MatchPoint')),
    h('div', { class: 'perfil' },
      h('strong', {}, usuario.nombre),
      h('span', {}, `${usuario.identificacion} · ${ROLES[usuario.rol]}`)),
    h('nav', { class: 'menu', 'aria-label': 'Opciones de su rol' }, enlaces, pendientes),
    h('button', { class: 'btn btn-fantasma salir', onclick: cerrarSesion }, 'Cerrar sesión'),
    h('p', { class: 'pie' }, 'Créditos 100 % virtuales: sin valor monetario, retiro ni conversión.'));

  app.replaceChildren(h('div', { class: 'marco' }, barra, contenido));
  pintarVista();
}

function pintarVista() {
  if (!usuarioActual) return;
  const contenido = document.getElementById('contenido');
  const vistas = vistasDe(usuarioActual.rol);
  const id = location.hash.replace(/^#\/?/, '') || 'inicio';
  const vista = vistas.find((v) => v.id === id) || vistas[0];
  document.querySelectorAll('.menu-item[data-vista]').forEach((a) =>
    a.classList.toggle('activo', a.dataset.vista === vista.id));
  contenido.replaceChildren();
  const ctx = {
    usuario: usuarioActual,
    ir: (destino) => { location.hash = `#/${destino}`; },
    recargar: pintarVista,
  };
  Promise.resolve(vista.render(contenido, ctx)).catch((e) => aviso(e.message, 'error'));
  contenido.focus({ preventScroll: true });
}

window.addEventListener('hashchange', pintarVista);
window.addEventListener('matchpoint:sesion-expirada', (e) => mostrarAcceso(e.detail));

async function arrancar() {
  if (!configurado) {
    app.replaceChildren(h('div', { class: 'config-falta' },
      h('h1', {}, 'Falta la configuración de Supabase'),
      h('p', {}, 'Cree web/.env.local a partir de web/.env.example o configure VITE_SUPABASE_URL y VITE_SUPABASE_ANON_KEY en Vercel.')));
    return;
  }
  if (!sesion.leer()) {
    mostrarAcceso();
    return;
  }
  try {
    const usuario = await rpc('sesion_actual');
    sesion.guardar(usuario);
    mostrarApp(usuario);
  } catch {
    mostrarAcceso();
  }
}

arrancar();

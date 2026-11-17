// HU-03 · Crear torneo (organizador): formulario + lista de torneos.
import { rpc } from '../api.js';
import { h, campo, enviar, aviso, encabezado, formato, vacio, limpiarErrores } from '../ui.js';

export const DEPORTES = ['Fútbol', 'Baloncesto', 'Tenis', 'Voleibol', 'Atletismo', 'Ciclismo', 'Automovilismo', 'Otro'];

function hoyLocal() {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

export default {
  id: 'torneos-admin',
  titulo: 'Torneos',
  hu: 'HU-03',
  resumen: 'Cree torneos con nombre único, deporte y fechas.',
  roles: ['ORGANIZADOR'],
  async render(contenedor) {
    const lista = h('div', {}, vacio('Cargando torneos…'));

    async function cargar() {
      const torneos = await rpc('listar_torneos');
      lista.replaceChildren(torneos.length === 0
        ? vacio('Aún no hay torneos. Cree el primero con el formulario.')
        : h('table', { class: 'tabla' },
          h('thead', {}, h('tr', {}, h('th', {}, 'Nombre'), h('th', {}, 'Deporte'), h('th', {}, 'Inicio'),
            h('th', {}, 'Fin'), h('th', { class: 'num' }, 'Eventos'))),
          h('tbody', {}, torneos.map((t) => h('tr', {},
            h('td', {}, h('strong', {}, t.nombre)), h('td', {}, t.deporte),
            h('td', {}, formato.fecha(t.fecha_inicio)), h('td', {}, formato.fecha(t.fecha_fin)),
            h('td', { class: 'num' }, t.eventos))))));
    }

    const form = h('form', { class: 'formulario', novalidate: true },
      campo('Nombre *', h('input', { name: 'nombre', maxlength: '60' })),
      campo('Deporte *', h('select', { name: 'deporte' },
        h('option', { value: '' }, 'Seleccione…'), DEPORTES.map((d) => h('option', { value: d }, d)))),
      h('div', { class: 'fila' },
        campo('Fecha de inicio *', h('input', { name: 'inicio', type: 'date', min: hoyLocal() })),
        campo('Fecha de fin *', h('input', { name: 'fin', type: 'date', min: hoyLocal() }))),
      h('div', { class: 'acciones' },
        h('button', { class: 'btn btn-primario', type: 'submit' }, 'Crear'),
        h('button', { class: 'btn btn-fantasma', type: 'reset' }, 'Cancelar')));

    form.addEventListener('reset', () => limpiarErrores(form));
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const d = new FormData(form);
      enviar(form, async () => {
        const t = await rpc('crear_torneo', {
          p_nombre: d.get('nombre'), p_deporte: d.get('deporte'), p_inicio: d.get('inicio'), p_fin: d.get('fin'),
        });
        aviso(`Torneo "${t.nombre}" creado.`);
        form.reset();
        await cargar();
      });
    });

    contenedor.append(
      encabezado('Torneos', 'HU-03', 'La fecha de inicio no puede ser anterior a hoy y la de fin no puede ser anterior a la de inicio (pueden ser iguales).'),
      h('div', { class: 'rejilla' },
        h('section', { class: 'panel' }, h('h2', {}, 'Crear torneo'), form),
        h('section', { class: 'panel' }, h('h2', {}, 'Lista de torneos'), lista)));
    await cargar();
  },
};

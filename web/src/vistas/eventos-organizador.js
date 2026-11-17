// HU-04 · Registrar evento y opciones (organizador).
import { rpc } from '../api.js';
import { h, campo, enviar, aviso, encabezado, formato, vacio, localAIso, estadoChip, limpiarErrores } from '../ui.js';

export const MODALIDADES = {
  GANADOR_ENFRENTAMIENTO: { texto: 'Ganador (2 opciones, sin empate)', base: ['', ''] },
  GANADOR_O_EMPATE: { texto: 'Local / Empate / Visitante (3 opciones)', base: ['Local', 'Empate', 'Visitante'] },
  OPCION_MULTIPLE: { texto: 'Opción múltiple (2 a 10 opciones)', base: ['', '', ''] },
};

export default {
  id: 'eventos-admin',
  titulo: 'Eventos',
  hu: 'HU-04',
  resumen: 'Registre eventos con modalidad, opciones, hora de cierre y costo.',
  roles: ['ORGANIZADOR'],
  async render(contenedor) {
    const [torneos, eventos] = await Promise.all([rpc('listar_torneos'), rpc('listar_eventos')]);
    const lista = h('div', {});
    const opciones = h('div', { class: 'formulario' });

    function pintarLista(items) {
      lista.replaceChildren(items.length === 0 ? vacio('Todavía no hay eventos registrados.')
        : h('table', { class: 'tabla' },
          h('thead', {}, h('tr', {}, h('th', {}, 'Evento'), h('th', {}, 'Inicio'), h('th', {}, 'Cierre'),
            h('th', {}, 'Opciones'), h('th', { class: 'num' }, 'Costo'), h('th', {}, 'Estado'))),
          h('tbody', {}, items.map((e) => h('tr', {},
            h('td', {}, h('strong', {}, e.nombre), h('br'), h('small', {}, e.torneo)),
            h('td', {}, formato.fechaHora(e.inicio)), h('td', {}, formato.fechaHora(e.cierre)),
            h('td', {}, h('div', { class: 'opciones' }, e.opciones.map((o) => h('span', { class: 'opcion' }, o)))),
            h('td', { class: 'num' }, e.costo), h('td', {}, estadoChip(e.estado)))))));
    }

    function pintarOpciones(modalidad) {
      const base = MODALIDADES[modalidad]?.base || [];
      opciones.replaceChildren(...base.map((valor, i) => h('input', { name: 'opcion', value: valor, placeholder: `Opción ${i + 1}`, maxlength: '40' })),
        modalidad === 'OPCION_MULTIPLE'
          ? h('button', { type: 'button', class: 'btn btn-fantasma btn-chico', onclick: () => {
            if (opciones.querySelectorAll('input').length < 10) {
              opciones.insertBefore(h('input', { name: 'opcion', placeholder: 'Otra opción', maxlength: '40' }), opciones.lastChild);
            }
          } }, '+ Agregar opción') : null);
    }

    const selModalidad = h('select', { name: 'modalidad', onchange: (e) => pintarOpciones(e.target.value) },
      h('option', { value: '' }, 'Seleccione…'),
      Object.entries(MODALIDADES).map(([k, m]) => h('option', { value: k }, m.texto)));
    const cajaOpciones = campo('Opciones de pronóstico *', h('div', { name: 'opciones' }, opciones));
    cajaOpciones.dataset.campo = 'opciones';

    const form = h('form', { class: 'formulario', novalidate: true },
      campo('Torneo *', h('select', { name: 'torneo' }, h('option', { value: '' }, 'Seleccione…'),
        torneos.map((t) => h('option', { value: t.id }, `${t.nombre} (${formato.fecha(t.fecha_inicio)} – ${formato.fecha(t.fecha_fin)})`)))),
      campo('Nombre del evento *', h('input', { name: 'nombre', maxlength: '60', placeholder: 'Ej.: Millonarios vs Santa Fe' })),
      h('div', { class: 'fila' },
        campo('Fecha y hora del evento *', h('input', { name: 'inicio', type: 'datetime-local' })),
        campo('Hora de cierre *', h('input', { name: 'cierre', type: 'datetime-local' }), 'No puede ser posterior al inicio.')),
      campo('Modalidad *', selModalidad),
      cajaOpciones,
      campo('Costo en créditos *', h('input', { name: 'costo', type: 'number', min: '1', step: '1', value: '10' })),
      h('div', { class: 'acciones' },
        h('button', { class: 'btn btn-primario', type: 'submit' }, 'Registrar evento'),
        h('button', { class: 'btn btn-fantasma', type: 'reset' }, 'Cancelar')));

    form.addEventListener('reset', () => { limpiarErrores(form); opciones.replaceChildren(); });
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const d = new FormData(form);
      enviar(form, async () => {
        const evento = await rpc('registrar_evento', {
          p_torneo: d.get('torneo'), p_nombre: d.get('nombre'),
          p_inicio: localAIso(d.get('inicio')), p_modalidad: d.get('modalidad'),
          p_opciones: d.getAll('opcion'), p_cierre: localAIso(d.get('cierre')), p_costo: d.get('costo'),
        });
        aviso(`Evento "${evento.nombre}" registrado.`);
        form.reset();
        pintarLista(await rpc('listar_eventos'));
      });
    });

    contenedor.append(
      encabezado('Eventos', 'HU-04', 'El evento queda abierto para pronósticos hasta su hora de cierre y cerrado desde ese instante.'),
      torneos.length === 0 ? vacio('Primero cree un torneo (HU-03).')
        : h('div', { class: 'rejilla' },
          h('section', { class: 'panel' }, h('h2', {}, 'Registrar evento'), form),
          h('section', { class: 'panel' }, h('h2', {}, 'Eventos registrados'), lista)));
    pintarLista(eventos);
  },
};

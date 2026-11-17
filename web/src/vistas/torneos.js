// HU-05 · Listar torneos y eventos con su estado.
// HU-06 · Ver las opciones de pronóstico y la fecha y hora de cierre de cada evento.
import { rpc } from '../api.js';
import { h, encabezado, formato, vacio, estadoChip } from '../ui.js';
import { abrirPronostico } from './pronosticar.js';

const MODALIDAD = {
  GANADOR_ENFRENTAMIENTO: 'Ganador (sin empate)',
  GANADOR_O_EMPATE: 'Local / Empate / Visitante',
  OPCION_MULTIPLE: 'Opción múltiple',
};

function filaEvento(e, puedePronosticar, recargar) {
  return h('tr', {},
    h('td', {}, h('strong', {}, e.nombre), h('br'), h('small', {}, MODALIDAD[e.modalidad] || e.modalidad)),
    h('td', {}, formato.fechaHora(e.inicio)),
    h('td', {}, h('div', { class: 'opciones' },
      e.opciones.map((o) => h('span', { class: `opcion${o === e.mi_pronostico ? ' elegida' : ''}` }, o)))),
    h('td', {}, h('strong', {}, formato.fechaHora(e.cierre))),
    h('td', { class: 'num' }, e.costo),
    h('td', {}, estadoChip(e.estado)),
    h('td', {}, e.mi_pronostico ? h('small', {}, 'Ya pronosticó')
      : puedePronosticar && e.estado === 'ABIERTO'
        ? h('button', { class: 'btn btn-primario btn-chico', onclick: () => abrirPronostico(e, recargar) }, 'Pronosticar')
        : null));
}

export default {
  id: 'torneos',
  titulo: 'Torneos y eventos',
  hu: 'HU-05 / HU-06',
  resumen: 'Consulte los torneos, sus eventos, opciones y hora de cierre.',
  roles: ['PARTICIPANTE', 'AUDITOR'],
  async render(contenedor, { usuario, recargar }) {
    const torneos = await rpc('listar_torneos_eventos');
    const filtro = h('select', { class: 'btn', 'aria-label': 'Filtrar eventos' },
      h('option', { value: 'todos' }, 'Todos los eventos'),
      h('option', { value: 'ABIERTO' }, 'Solo abiertos'),
      h('option', { value: 'CERRADO' }, 'Solo cerrados'));
    const lista = h('div', { class: 'formulario' });

    function pintar() {
      const f = filtro.value;
      lista.replaceChildren(...(torneos.length === 0 ? [vacio('Todavía no hay torneos disponibles.')]
        : torneos.map((t) => {
          const eventos = t.eventos.filter((e) => f === 'todos' || e.estado === f);
          return h('section', { class: 'panel' },
            h('div', { class: 'vista-titulo' }, h('h2', {}, t.nombre), h('span', { class: 'chip-hu' }, t.deporte),
              h('small', {}, `${formato.fecha(t.fecha_inicio)} – ${formato.fecha(t.fecha_fin)}`)),
            eventos.length === 0 ? vacio('Sin eventos para mostrar.')
              : h('table', { class: 'tabla' },
                h('thead', {}, h('tr', {}, h('th', {}, 'Evento'), h('th', {}, 'Fecha y hora'), h('th', {}, 'Opciones'),
                  h('th', {}, 'Cierre'), h('th', { class: 'num' }, 'Costo'), h('th', {}, 'Estado'), h('th', {}, ''))),
                h('tbody', {}, eventos.map((e) => filaEvento(e, usuario.rol === 'PARTICIPANTE', recargar)))));
        })));
    }
    filtro.addEventListener('change', pintar);
    contenedor.append(
      encabezado('Torneos y eventos', 'HU-05 / HU-06', 'Cada evento muestra sus opciones de pronóstico y su fecha y hora de cierre. Abierto = aún admite pronósticos.'),
      h('div', { class: 'acciones', style: 'margin-bottom:1rem' }, filtro),
      lista);
    pintar();
  },
};

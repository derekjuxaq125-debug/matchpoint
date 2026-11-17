// HU-12 · Bloquear pronósticos al cierre: antes del cierre se puede cambiar la
// opción (sin nuevo cobro); después, la base de datos rechaza el cambio.
import { rpc } from '../api.js';
import { h, aviso, encabezado, formato, vacio, estadoChip } from '../ui.js';

export default {
  id: 'mis-pronosticos',
  titulo: 'Mis pronósticos',
  hu: 'HU-12',
  resumen: 'Revise sus pronósticos y cámbielos antes del cierre.',
  roles: ['PARTICIPANTE'],
  async render(contenedor, { recargar }) {
    const mios = await rpc('mis_pronosticos');

    function fila(p) {
      const error = h('small', { class: 'error-campo', role: 'alert' });
      const botones = p.evento.opciones.map((o) => h('button', {
        type: 'button',
        class: `opcion${o === p.opcion ? ' elegida' : ''}`,
        // Se deja intentar también en eventos cerrados: la regla la aplica el servidor (HU-12).
        onclick: async () => {
          if (o === p.opcion) return;
          error.textContent = '';
          try {
            const r = await rpc('cambiar_pronostico', { p_pronostico: p.id, p_opcion: o });
            aviso(`Pronóstico cambiado a "${r.opcion}". No se cobró de nuevo.`);
            recargar();
          } catch (e) {
            error.textContent = e.message;
          }
        },
      }, o));
      return h('tr', {},
        h('td', {}, h('strong', {}, p.evento.nombre), h('br'), h('small', {}, p.evento.torneo)),
        h('td', {}, h('div', { class: 'opciones' }, botones), error),
        h('td', {}, formato.fechaHora(p.evento.cierre)),
        h('td', {}, estadoChip(p.evento.estado)),
        h('td', {}, h('small', {}, formato.fechaHora(p.registrado)),
          p.actualizado ? h('small', {}, h('br'), `cambiado ${formato.fechaHora(p.actualizado)}`) : null));
    }

    contenedor.append(
      encabezado('Mis pronósticos', 'HU-12', 'Toque otra opción para cambiar su pronóstico mientras el evento esté abierto. Desde la hora de cierre no se aceptan cambios.'),
      mios.length === 0 ? vacio('Aún no ha registrado pronósticos.')
        : h('section', { class: 'panel' }, h('table', { class: 'tabla' },
          h('thead', {}, h('tr', {}, h('th', {}, 'Evento'), h('th', {}, 'Mi pronóstico'), h('th', {}, 'Cierre'),
            h('th', {}, 'Estado'), h('th', {}, 'Registrado'))),
          h('tbody', {}, mios.map(fila)))));
  },
};

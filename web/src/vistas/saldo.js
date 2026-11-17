// HU-08 · Consultar saldo: el saldo actual debe coincidir con la suma de los movimientos.
import { rpc } from '../api.js';
import { h, encabezado, formato, vacio } from '../ui.js';

const TIPOS = {
  ASIGNACION_INICIAL: 'Asignación inicial', PARTICIPACION: 'Participación', PREMIO: 'Premio',
  AJUSTE: 'Ajuste', REVERSION: 'Reversión',
};

export default {
  id: 'saldo',
  titulo: 'Mi saldo',
  hu: 'HU-08',
  resumen: 'Vea cuántos créditos tiene y verifique que cuadren con sus movimientos.',
  roles: ['PARTICIPANTE'],
  async render(contenedor) {
    const s = await rpc('consultar_saldo');
    contenedor.append(
      encabezado('Mi saldo', 'HU-08', 'Créditos 100 % virtuales: no tienen valor monetario, no se retiran ni se convierten en dinero.'),
      h('section', { class: 'tarjetas' },
        h('div', { class: 'panel saldo-grande' },
          h('small', {}, 'Saldo disponible'),
          h('strong', { class: 'cifra' }, formato.creditos(s.saldo)),
          h('span', {}, 'créditos')),
        h('div', { class: `panel verificacion ${s.coincide ? 'ok' : 'mal'}` },
          h('small', {}, 'Verificación'),
          h('strong', {}, s.coincide ? 'Cuadra' : 'No cuadra'),
          h('span', {}, `Suma de ${s.movimientos.length} movimientos: ${formato.creditos(s.suma_movimientos)}`))),
      h('section', { class: 'panel', style: 'margin-top:1.2rem' },
        h('h2', {}, 'De dónde sale su saldo'),
        s.movimientos.length === 0 ? vacio('Aún no tiene movimientos. El organizador le asignará su saldo inicial.')
          : h('table', { class: 'tabla' },
            h('thead', {}, h('tr', {}, h('th', {}, 'Fecha'), h('th', {}, 'Tipo'), h('th', {}, 'Detalle'),
              h('th', { class: 'num' }, 'Cantidad'), h('th', { class: 'num' }, 'Saldo después'))),
            h('tbody', {}, s.movimientos.map((m) => h('tr', {},
              h('td', {}, formato.fechaHora(m.registrado)), h('td', {}, TIPOS[m.tipo] || m.tipo),
              h('td', {}, m.detalle || m.justificacion || m.origen_tipo),
              h('td', { class: `num ${m.cantidad < 0 ? 'neg' : 'pos'}` }, `${m.cantidad > 0 ? '+' : ''}${m.cantidad}`),
              h('td', { class: 'num' }, formato.creditos(m.saldo_resultante))))))));
  },
};

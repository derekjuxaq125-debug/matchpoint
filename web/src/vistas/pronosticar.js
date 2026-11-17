// HU-10 · Registrar pronóstico: solo en eventos habilitados y con una opción válida.
import { rpc, nuevaClaveSolicitud } from '../api.js';
import { h, aviso, encabezado, formato, vacio, mostrarError } from '../ui.js';

/**
 * Abre el diálogo para pronosticar un evento. La clave de solicitud se crea una
 * vez por diálogo y se reutiliza si el usuario reintenta (HU-14).
 */
export function abrirPronostico(evento, alTerminar) {
  const clave = nuevaClaveSolicitud();
  let elegida = null;
  const botones = evento.opciones.map((o) => h('button', {
    type: 'button', class: 'opcion', onclick: (e) => {
      elegida = o;
      botones.forEach((b) => b.classList.toggle('elegida', b === e.currentTarget));
    },
  }, o));
  const form = h('form', { class: 'dialogo', method: 'dialog', novalidate: true },
    h('h2', {}, evento.nombre),
    h('p', {}, `${evento.torneo} · cierra ${formato.fechaHora(evento.cierre)} · cuesta ${evento.costo} créditos`),
    h('div', { class: 'campo', 'data-campo': 'opcion' },
      h('label', {}, 'Su pronóstico *'), h('div', { class: 'opciones' }, botones), h('small', { class: 'error-campo' })),
    h('div', { class: 'campo', 'data-campo': 'evento' }, h('small', { class: 'error-campo' })),
    h('div', { class: 'acciones' },
      h('button', { class: 'btn btn-primario', type: 'submit' }, 'Confirmar pronóstico'),
      h('button', { class: 'btn btn-fantasma', type: 'button', onclick: () => dialogo.close() }, 'Cancelar')));
  const dialogo = h('dialog', {}, form);
  dialogo.addEventListener('close', () => dialogo.remove());

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const boton = form.querySelector('[type=submit]');
    boton.setAttribute('disabled', '');
    try {
      const r = await rpc('registrar_pronostico', { p_evento: evento.id, p_opcion: elegida, p_clave_solicitud: clave });
      aviso(r.repetida ? 'La operación ya fue procesada; no se cobró de nuevo.'
        : `Pronóstico "${r.opcion}" registrado. Saldo: ${formato.creditos(r.saldo)} créditos.`);
      dialogo.close();
      alTerminar?.(r);
    } catch (err) {
      mostrarError(form, err);
    } finally {
      boton.removeAttribute('disabled');
    }
  });
  document.body.append(dialogo);
  dialogo.showModal();
}

export default {
  id: 'pronosticar',
  titulo: 'Pronosticar',
  hu: 'HU-10',
  resumen: 'Elija un evento abierto y registre su pronóstico.',
  roles: ['PARTICIPANTE'],
  async render(contenedor, { recargar }) {
    const eventos = await rpc('listar_eventos', { p_solo_abiertos: true });
    const tarjetas = eventos.map((e) => h('article', { class: 'tarjeta-opcion' },
      h('span', { class: 'chip-hu' }, `${e.costo} créditos`),
      h('strong', {}, e.nombre),
      h('span', {}, `${e.torneo} · cierra ${formato.fechaHora(e.cierre)}`),
      h('div', { class: 'opciones' }, e.opciones.map((o) => h('span', { class: `opcion${o === e.mi_pronostico ? ' elegida' : ''}` }, o))),
      e.mi_pronostico
        ? h('small', {}, `Ya pronosticó: ${e.mi_pronostico}`)
        : h('button', { class: 'btn btn-primario btn-chico', onclick: () => abrirPronostico(e, recargar) }, 'Pronosticar')));
    contenedor.append(
      encabezado('Pronosticar', 'HU-10', 'Solo se muestran los eventos abiertos. Un pronóstico por evento.'),
      eventos.length === 0 ? vacio('No hay eventos abiertos en este momento.') : h('section', { class: 'tarjetas' }, tarjetas));
  },
};

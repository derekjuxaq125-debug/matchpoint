// Inicio después de iniciar sesión: resume lo que puede hacer cada rol (HU-02).
import { h, encabezado } from '../ui.js';
import { VISTAS, PENDIENTES } from './index.js';

const QUE_HACE = {
  PARTICIPANTE: 'Consulte los torneos, pronostique antes del cierre de cada evento y revise su saldo de créditos.',
  ORGANIZADOR: 'Cree torneos, registre eventos con sus opciones y hora de cierre, y asigne los créditos iniciales.',
  AUDITOR: 'Consulte la información consolidada y verifique que saldos, movimientos y liquidaciones coincidan.',
};

export default {
  id: 'inicio',
  titulo: 'Inicio',
  roles: ['PARTICIPANTE', 'ORGANIZADOR', 'AUDITOR'],
  render(contenedor, { usuario, ir }) {
    const disponibles = VISTAS.filter((v) => v.id !== 'inicio' && v.roles.includes(usuario.rol));
    const pendientes = PENDIENTES[usuario.rol] || [];
    contenedor.append(
      encabezado(`Hola, ${usuario.nombre.split(' ')[0]}`, null, QUE_HACE[usuario.rol]),
      h('section', { class: 'tarjetas' },
        disponibles.map((v) => h('button', { class: 'tarjeta-opcion', onclick: () => ir(v.id) },
          h('span', { class: 'chip-hu' }, v.hu), h('strong', {}, v.titulo), h('span', {}, v.resumen || ''))),
        pendientes.map((p) => h('div', { class: 'tarjeta-opcion pendiente' },
          h('span', { class: 'chip-hu' }, p.hu), h('strong', {}, p.titulo),
          h('span', {}, p.sprint ? `Llega en el ${p.sprint}` : 'En construcción')))));
  },
};

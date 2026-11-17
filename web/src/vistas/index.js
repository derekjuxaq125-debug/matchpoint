// Registro de vistas: cada historia de usuario agrega aquí su pantalla.
// roles = quién la ve en el menú (HU-02). Las que aún no existen se listan en
// PENDIENTES y se muestran deshabilitadas, como en la versión de escritorio.
import inicio from './inicio.js';
import torneos from './torneos.js';
import torneosAdmin from './torneos-organizador.js';
import eventosAdmin from './eventos-organizador.js';
import pronosticar from './pronosticar.js';
import misPronosticos from './mis-pronosticos.js';
import creditos from './creditos.js';
import saldo from './saldo.js';

export const VISTAS = [
  inicio,
  torneos,
  torneosAdmin,
  eventosAdmin,
  pronosticar,
  misPronosticos,
  creditos,
  saldo,
];

export const PENDIENTES = {
  PARTICIPANTE: [
  ],
  ORGANIZADOR: [
  ],
  AUDITOR: [
    { titulo: 'Reporte consolidado', hu: 'HU-24', sprint: 'Sprint 3' },
    { titulo: 'Detectar inconsistencias', hu: 'HU-25', sprint: 'Sprint 3' },
  ],
};

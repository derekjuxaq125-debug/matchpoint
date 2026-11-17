// Registro de vistas: cada historia de usuario agrega aquí su pantalla.
// roles = quién la ve en el menú (HU-02). Las que aún no existen se listan en
// PENDIENTES y se muestran deshabilitadas, como en la versión de escritorio.
import inicio from './inicio.js';
import creditos from './creditos.js';

export const VISTAS = [
  inicio,
  creditos,
];

export const PENDIENTES = {
  PARTICIPANTE: [
    { titulo: 'Torneos y eventos', hu: 'HU-05 / HU-06' },
    { titulo: 'Pronosticar', hu: 'HU-10' },
    { titulo: 'Mis pronósticos', hu: 'HU-12' },
    { titulo: 'Mi saldo', hu: 'HU-08' },
  ],
  ORGANIZADOR: [
    { titulo: 'Torneos', hu: 'HU-03' },
    { titulo: 'Eventos', hu: 'HU-04' },
  ],
  AUDITOR: [
    { titulo: 'Reporte consolidado', hu: 'HU-24', sprint: 'Sprint 3' },
    { titulo: 'Detectar inconsistencias', hu: 'HU-25', sprint: 'Sprint 3' },
  ],
};

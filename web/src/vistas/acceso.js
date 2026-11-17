// HU-01 (registrarse) y HU-02 (iniciar sesión con rol).
import { rpc } from '../api.js';
import { h, campo, enviar, aviso } from '../ui.js';

function formularioIngreso(alEntrar) {
  const form = h('form', { class: 'formulario', novalidate: true },
    campo('Identificación', h('input', { name: 'identificacion', autocomplete: 'username', maxlength: '15' })),
    campo('Contraseña', h('input', { name: 'clave', type: 'password', autocomplete: 'current-password' })),
    h('button', { class: 'btn btn-primario', type: 'submit' }, 'Iniciar sesión'));
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const d = new FormData(form);
    enviar(form, async () => {
      const usuario = await rpc('iniciar_sesion',
        { p_identificacion: d.get('identificacion'), p_clave: d.get('clave') }, { conSesion: false });
      alEntrar(usuario);
    });
  });
  return form;
}

function formularioRegistro(alTerminar) {
  const form = h('form', { class: 'formulario', novalidate: true },
    campo('Identificación', h('input', { name: 'identificacion', maxlength: '15', autocomplete: 'username' }),
      'De 5 a 15 letras o números, sin espacios.'),
    campo('Nombre', h('input', { name: 'nombre', maxlength: '60', autocomplete: 'name' })),
    campo('Contraseña', h('input', { name: 'clave', type: 'password', autocomplete: 'new-password' }), 'Mínimo 8 caracteres.'),
    campo('Confirmar contraseña', h('input', { name: 'confirmacion', type: 'password', autocomplete: 'new-password' })),
    h('div', { class: 'acciones' },
      h('button', { class: 'btn btn-primario', type: 'submit' }, 'Registrarse'),
      h('button', { class: 'btn btn-fantasma', type: 'button', onclick: () => alTerminar(null) }, 'Cancelar')));
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const d = new FormData(form);
    enviar(form, async () => {
      const cuenta = await rpc('registrar_usuario', {
        p_identificacion: d.get('identificacion'), p_nombre: d.get('nombre'),
        p_clave: d.get('clave'), p_confirmacion: d.get('confirmacion'),
      }, { conSesion: false });
      aviso(`Cuenta ${cuenta.identificacion} creada. Ya puede iniciar sesión.`);
      alTerminar(cuenta.identificacion);
    });
  });
  return form;
}

export function pantallaAcceso({ alEntrar, mensaje }) {
  const panel = h('div', { class: 'acceso-panel' });
  const titulo = h('h2', {});

  function verIngreso(identificacion) {
    titulo.textContent = 'Iniciar sesión';
    const form = formularioIngreso(alEntrar);
    if (identificacion) form.querySelector('[name=identificacion]').value = identificacion;
    panel.replaceChildren(titulo,
      mensaje ? h('p', { class: 'error-general' }, mensaje) : null,
      form,
      h('p', { class: 'cambio' }, '¿No tiene cuenta? ',
        h('button', { class: 'enlace', type: 'button', onclick: verRegistro }, 'Registrarse')));
    mensaje = null;
  }
  function verRegistro() {
    titulo.textContent = 'Crear cuenta de participante';
    panel.replaceChildren(titulo, formularioRegistro(verIngreso));
  }
  verIngreso();

  return h('div', { class: 'acceso' },
    h('section', { class: 'acceso-portada' },
      h('div', { class: 'marca grande' }, h('span', { class: 'marca-icono', 'aria-hidden': 'true' }), h('span', {}, 'MatchPoint')),
      h('p', { class: 'lema' }, 'Torneos, pronósticos y créditos virtuales con cada movimiento verificable.'),
      h('div', { class: 'marcador', 'aria-hidden': 'true' },
        h('span', {}, 'LOCAL'), h('b', {}, '2'), h('i', {}, ':'), h('b', {}, '1'), h('span', {}, 'VISITANTE')),
      h('dl', { class: 'demo' },
        h('dt', {}, 'Cuentas de prueba'),
        h('dd', {}, h('code', {}, 'ORG0001'), ' / organizador123 · Organizador'),
        h('dd', {}, h('code', {}, 'AUD0001'), ' / auditor123 · Auditor'),
        h('dd', {}, 'Los participantes se registran solos.')),
      h('p', { class: 'cliente' }, 'Nexo Digital Labs · Equipo 1')),
    panel);
}

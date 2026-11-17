// Utilidades de interfaz compartidas por todas las vistas.

/** Crea un elemento: h('button', { class: 'btn', onclick }, 'Texto'). */
export function h(etiqueta, atributos = {}, ...hijos) {
  const el = document.createElement(etiqueta);
  for (const [clave, valor] of Object.entries(atributos || {})) {
    if (valor === null || valor === undefined || valor === false) continue;
    if (clave.startsWith('on') && typeof valor === 'function') el.addEventListener(clave.slice(2), valor);
    else if (clave === 'class') el.className = valor;
    else if (clave === 'html') el.innerHTML = valor;
    else if (valor === true) el.setAttribute(clave, '');
    else el.setAttribute(clave, valor);
  }
  for (const hijo of hijos.flat(Infinity)) {
    if (hijo === null || hijo === undefined || hijo === false) continue;
    el.append(hijo instanceof Node ? hijo : document.createTextNode(String(hijo)));
  }
  return el;
}

export function aviso(mensaje, tipo = 'ok') {
  let zona = document.getElementById('avisos');
  if (!zona) {
    zona = h('div', { id: 'avisos', role: 'status', 'aria-live': 'polite' });
    document.body.append(zona);
  }
  const nota = h('div', { class: `aviso aviso-${tipo}` }, mensaje);
  zona.append(nota);
  setTimeout(() => nota.classList.add('saliendo'), 4200);
  setTimeout(() => nota.remove(), 4700);
}

/** Campo de formulario con etiqueta y espacio para el mensaje de error. */
export function campo(etiqueta, control, ayuda) {
  const id = control.getAttribute('name');
  control.id = `f-${id}`;
  return h('div', { class: 'campo', 'data-campo': id },
    h('label', { for: control.id }, etiqueta),
    control,
    ayuda ? h('small', { class: 'ayuda' }, ayuda) : null,
    h('small', { class: 'error-campo' }));
}

export function limpiarErrores(form) {
  form.querySelectorAll('.campo.con-error').forEach((c) => c.classList.remove('con-error'));
  form.querySelectorAll('.error-campo').forEach((e) => { e.textContent = ''; });
  form.querySelector('.error-general')?.remove();
}

/** Muestra el error: resalta el campo indicado por la base de datos o lo deja arriba. */
export function mostrarError(form, error) {
  limpiarErrores(form);
  const caja = error.campo && form.querySelector(`.campo[data-campo="${error.campo}"]`);
  if (caja) {
    caja.classList.add('con-error');
    caja.querySelector('.error-campo').textContent = error.message;
    caja.querySelector('input,select,textarea')?.focus();
  } else {
    form.prepend(h('p', { class: 'error-general', role: 'alert' }, error.message));
  }
}

/** Ejecuta la acción del formulario deshabilitando el botón mientras espera. */
export async function enviar(form, accion) {
  const boton = form.querySelector('button[type="submit"]');
  boton?.setAttribute('disabled', '');
  try {
    limpiarErrores(form);
    return await accion();
  } catch (e) {
    mostrarError(form, e);
    return undefined;
  } finally {
    boton?.removeAttribute('disabled');
  }
}

const fechaHora = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
const soloFecha = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeZone: 'UTC' });

export const formato = {
  fechaHora: (iso) => (iso ? fechaHora.format(new Date(iso)) : '—'),
  fecha: (iso) => (iso ? soloFecha.format(new Date(`${iso}T00:00:00Z`)) : '—'),
  creditos: (n) => new Intl.NumberFormat('es-CO').format(n ?? 0),
};

/** Convierte el valor de <input type="datetime-local"> a ISO con la zona del navegador. */
export function localAIso(valor) {
  return valor ? new Date(valor).toISOString() : null;
}

export function encabezado(titulo, hu, descripcion) {
  return h('header', { class: 'vista-encabezado' },
    h('div', { class: 'vista-titulo' }, h('h1', {}, titulo), hu ? h('span', { class: 'chip-hu' }, hu) : null),
    descripcion ? h('p', { class: 'vista-descripcion' }, descripcion) : null);
}

export function vacio(texto) {
  return h('div', { class: 'vacio' }, texto);
}

export function estadoChip(estado) {
  return h('span', { class: `estado estado-${String(estado).toLowerCase()}` }, estado === 'ABIERTO' ? 'Abierto' : 'Cerrado');
}

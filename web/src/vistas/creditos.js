// HU-07 · Asignar créditos iniciales (organizador), con confirmación.
import { rpc } from '../api.js';
import { h, campo, enviar, aviso, encabezado, formato, vacio } from '../ui.js';

export default {
  id: 'creditos',
  titulo: 'Créditos iniciales',
  hu: 'HU-07',
  resumen: 'Asigne el saldo inicial a cada participante (una sola vez).',
  roles: ['ORGANIZADOR'],
  async render(contenedor) {
    let usuarios = await rpc('listar_usuarios_creditos');
    let seleccionado = null;
    const tabla = h('div', {});
    const elegido = h('input', { name: 'usuario', readonly: true, placeholder: 'Seleccione en la lista' });

    function pintar() {
      tabla.replaceChildren(usuarios.length === 0 ? vacio('Aún no hay participantes registrados.')
        : h('table', { class: 'tabla' },
          h('thead', {}, h('tr', {}, h('th', {}, ''), h('th', {}, 'Identificación'), h('th', {}, 'Nombre'),
            h('th', { class: 'num' }, 'Saldo inicial'), h('th', { class: 'num' }, 'Saldo actual'))),
          h('tbody', {}, usuarios.map((u) => h('tr', {},
            h('td', {}, h('input', {
              type: 'radio', name: 'sel', 'aria-label': `Seleccionar ${u.nombre}`,
              checked: seleccionado?.identificacion === u.identificacion,
              onchange: () => { seleccionado = u; elegido.value = `${u.identificacion} · ${u.nombre}`; },
            })),
            h('td', {}, h('code', {}, u.identificacion)), h('td', {}, u.nombre),
            h('td', { class: 'num' }, u.saldo_inicial ?? h('span', { class: 'estado estado-cerrado' }, 'Sin asignar')),
            h('td', { class: 'num' }, formato.creditos(u.saldo)))))));
    }

    const form = h('form', { class: 'formulario', novalidate: true },
      campo('Usuario *', elegido),
      campo('Cantidad de créditos *', h('input', { name: 'cantidad', inputmode: 'numeric' }), 'Entero de 1 a 10 000.'),
      h('div', { class: 'acciones' }, h('button', { class: 'btn btn-primario', type: 'submit' }, 'Asignar')));

    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const cantidad = new FormData(form).get('cantidad');
      // Confirmación previa: si elige "No" no se registra nada y se conservan los datos.
      if (seleccionado && /^\d+$/.test(String(cantidad).trim())
        && !window.confirm(`¿Asignar ${cantidad} créditos a ${seleccionado.nombre}?`)) return;
      enviar(form, async () => {
        const r = await rpc('asignar_creditos_iniciales', { p_usuario: seleccionado?.identificacion ?? '', p_cantidad: cantidad });
        aviso(`Saldo inicial registrado: ${r.cantidad} créditos para ${r.usuario}.`);
        form.reset();
        seleccionado = null;
        usuarios = await rpc('listar_usuarios_creditos');
        pintar();
      });
    });

    contenedor.append(
      encabezado('Créditos iniciales', 'HU-07', 'Los créditos son virtuales: no tienen valor monetario, no se retiran ni se convierten en dinero.'),
      h('div', { class: 'rejilla' },
        h('section', { class: 'panel' }, h('h2', {}, 'Asignar'), form),
        h('section', { class: 'panel' }, h('h2', {}, 'Participantes'), tabla)));
    pintar();
  },
};

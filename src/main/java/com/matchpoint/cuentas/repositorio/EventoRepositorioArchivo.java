package com.matchpoint.cuentas.repositorio;

import static com.matchpoint.cuentas.repositorio.ArchivoLineas.cod;
import static com.matchpoint.cuentas.repositorio.ArchivoLineas.dec;

import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.ModalidadPronostico;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Eventos en archivo de texto.
 * Formato: id;torneoId;nombre;inicio;MODALIDAD;opcion1,opcion2,...;cierre;costo
 */
public class EventoRepositorioArchivo implements EventoRepositorio {

    private final Path archivo;

    public EventoRepositorioArchivo(Path archivo) {
        this.archivo = archivo;
    }

    @Override
    public synchronized List<Evento> listar() {
        try {
            return ArchivoLineas.leer(archivo, "eventos").stream()
                    .map(p -> new Evento(dec(p[0]), dec(p[1]), dec(p[2]), LocalDateTime.parse(p[3]),
                            ModalidadPronostico.valueOf(p[4]),
                            Arrays.stream(p[5].split(",")).map(ArchivoLineas::dec).toList(),
                            LocalDateTime.parse(p[6]), Integer.parseInt(p[7])))
                    .toList();
        } catch (RuntimeException e) {
            if (e instanceof RepositorioException) {
                throw e;
            }
            throw new RepositorioException("El archivo de eventos está dañado.", e);
        }
    }

    @Override
    public List<Evento> listarPorTorneo(String torneoId) {
        return listar().stream().filter(e -> e.torneoId().equals(torneoId)).toList();
    }

    @Override
    public Optional<Evento> buscarPorId(String id) {
        return listar().stream().filter(e -> e.id().equals(id)).findFirst();
    }

    @Override
    public synchronized void guardar(Evento e) {
        String opciones = String.join(",", e.opciones().stream().map(ArchivoLineas::cod).toList());
        ArchivoLineas.agregar(archivo, "el evento", cod(e.id()), cod(e.torneoId()), cod(e.nombre()),
                e.inicio().toString(), e.modalidad().name(), opciones, e.cierre().toString(),
                Integer.toString(e.costoCreditos()));
    }
}

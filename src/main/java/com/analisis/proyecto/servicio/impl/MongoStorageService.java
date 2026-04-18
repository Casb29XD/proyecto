package com.analisis.proyecto.servicio.impl;

import com.analisis.proyecto.modelo.Articulo;
import com.analisis.proyecto.modelo.ArticuloDuplicado;
import com.analisis.proyecto.modelo.Favorito;
import com.analisis.proyecto.modelo.HistorialBusqueda;
import com.analisis.proyecto.repositorio.RepositorioArticulo;
import com.analisis.proyecto.repositorio.RepositorioArticuloDuplicado;
import com.analisis.proyecto.repositorio.RepositorioFavorito;
import com.analisis.proyecto.repositorio.RepositorioHistorial;
import com.analisis.proyecto.servicio.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación de almacenamiento que utiliza MongoDB.
 */
@Service
public class MongoStorageService implements StorageService {

    private final RepositorioArticulo repositorioArticulo;
    private final RepositorioArticuloDuplicado repositorioArticuloDuplicado;
    private final RepositorioHistorial repositorioHistorial;
    private final RepositorioFavorito repositorioFavorito;

    public MongoStorageService(RepositorioArticulo repositorioArticulo,
                               RepositorioArticuloDuplicado repositorioArticuloDuplicado,
                               RepositorioHistorial repositorioHistorial,
                               RepositorioFavorito repositorioFavorito) {
        this.repositorioArticulo = repositorioArticulo;
        this.repositorioArticuloDuplicado = repositorioArticuloDuplicado;
        this.repositorioHistorial = repositorioHistorial;
        this.repositorioFavorito = repositorioFavorito;
    }

    @Override
    public void guardarTodos(List<Articulo> articulos) {
        repositorioArticulo.saveAll(articulos);
    }

    @Override
    public List<Articulo> obtenerTodos() {
        return repositorioArticulo.findAll();
    }

    @Override
    public Page<Articulo> obtenerPaginados(Pageable pageable) {
        return repositorioArticulo.findAll(pageable);
    }

    @Override
    public boolean estaDisponible() {
        try {
            // Un chequeo simple para ver si MongoDB responde
            repositorioArticulo.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Métodos para Duplicados
    public void guardarDuplicados(List<ArticuloDuplicado> duplicados) {
        repositorioArticuloDuplicado.saveAll(duplicados);
    }

    public List<ArticuloDuplicado> obtenerDuplicados() {
        return repositorioArticuloDuplicado.findAll();
    }

    // Métodos para Historial
    public void registrarBusqueda(HistorialBusqueda historial) {
        repositorioHistorial.save(historial);
    }

    public List<HistorialBusqueda> obtenerHistorial() {
        return repositorioHistorial.findAll();
    }

    // Métodos para Favoritos
    public void guardarFavorito(Favorito favorito) {
        repositorioFavorito.save(favorito);
    }

    public void eliminarFavorito(String articuloId) {
        repositorioFavorito.findByArticuloId(articuloId).ifPresent(repositorioFavorito::delete);
    }

    public List<Favorito> obtenerFavoritos() {
        return repositorioFavorito.findAll();
    }
}

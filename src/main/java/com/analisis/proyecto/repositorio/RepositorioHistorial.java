package com.analisis.proyecto.repositorio;

import com.analisis.proyecto.modelo.HistorialBusqueda;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioHistorial extends MongoRepository<HistorialBusqueda, String> {
    java.util.List<HistorialBusqueda> findByUsuarioIdOrderByFechaDesc(String usuarioId);
}

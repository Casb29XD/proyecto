package com.analisis.proyecto.repositorio;

import com.analisis.proyecto.modelo.Articulo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositorioArticulo extends MongoRepository<Articulo, String> {
    Optional<Articulo> findByTituloIgnoreCase(String titulo);
}

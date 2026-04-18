package com.analisis.proyecto.repositorio;

import com.analisis.proyecto.modelo.Favorito;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositorioFavorito extends MongoRepository<Favorito, String> {
    Optional<Favorito> findByArticuloId(String articuloId);
}

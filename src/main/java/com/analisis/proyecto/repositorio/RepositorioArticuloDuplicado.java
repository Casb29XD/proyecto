package com.analisis.proyecto.repositorio;

import com.analisis.proyecto.modelo.ArticuloDuplicado;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioArticuloDuplicado extends MongoRepository<ArticuloDuplicado, String> {
}

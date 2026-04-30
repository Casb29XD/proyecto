package com.analisis.proyecto.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * Entidad que representa un artículo marcado como favorito.
 */
@Document(collection = "favoritos")
public class Favorito {

    @Id
    private String id;
    private String usuarioId;
    private String articuloId;
    private String titulo;
    private LocalDateTime fechaAgregado;

    public Favorito() {
        this.fechaAgregado = LocalDateTime.now();
    }

    public Favorito(Articulo articulo) {
        this();
        this.articuloId = articulo.getId();
        this.titulo = articulo.getTitulo();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getArticuloId() { return articuloId; }
    public void setArticuloId(String articuloId) { this.articuloId = articuloId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public LocalDateTime getFechaAgregado() { return fechaAgregado; }
    public void setFechaAgregado(LocalDateTime fechaAgregado) { this.fechaAgregado = fechaAgregado; }
}

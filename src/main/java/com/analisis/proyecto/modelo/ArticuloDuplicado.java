package com.analisis.proyecto.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "articulosduplicados")
public class ArticuloDuplicado {

    @Id
    private String id;
    private String titulo;
    private String origen;
    private String motivo;
    
    private Articulo articuloOriginal;

    public ArticuloDuplicado() {
    }

    public ArticuloDuplicado(Articulo articuloOriginal, String motivo) {
        this.titulo = articuloOriginal.getTitulo();
        this.origen = articuloOriginal.getOrigen();
        this.articuloOriginal = articuloOriginal;
        this.motivo = motivo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Articulo getArticuloOriginal() {
        return articuloOriginal;
    }

    public void setArticuloOriginal(Articulo articuloOriginal) {
        this.articuloOriginal = articuloOriginal;
    }
}

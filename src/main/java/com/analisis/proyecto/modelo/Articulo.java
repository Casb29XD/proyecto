package com.analisis.proyecto.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Objects;

@Document(collection = "articulos_unicos")
public class Articulo {

    @Id
    private String id;
    private String titulo;
    private List<String> autores;
    private String resumen;
    private List<String> palabrasClave;
    private String origen;

    public Articulo() {
    }

    public Articulo(String titulo, List<String> autores, String resumen, List<String> palabrasClave, String origen) {
        this.titulo = titulo;
        this.autores = autores;
        this.resumen = resumen;
        this.palabrasClave = palabrasClave;
        this.origen = origen;
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

    public List<String> getAutores() {
        return autores;
    }

    public void setAutores(List<String> autores) {
        this.autores = autores;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public List<String> getPalabrasClave() {
        return palabrasClave;
    }

    public void setPalabrasClave(List<String> palabrasClave) {
        this.palabrasClave = palabrasClave;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Articulo articulo = (Articulo) o;
        return Objects.equals(titulo != null ? titulo.toLowerCase() : null, articulo.titulo != null ? articulo.titulo.toLowerCase() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(titulo != null ? titulo.toLowerCase() : null);
    }
}

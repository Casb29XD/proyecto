package com.analisis.proyecto.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Objects;

/**
 * Entidad que representa un artículo científico en el contexto de la bibliometría.
 */
@Document(collection = "articulos")
public class Articulo {

    @Id
    private String id;
    private String titulo;
    private List<String> autores;
    private String resumen; // Abstract
    private Integer anio;
    private String revista;
    private String doi;
    private List<String> palabrasClave;
    private String origen;

    public Articulo() {
    }

    public Articulo(String titulo, List<String> autores, String resumen, Integer anio, String revista, String doi) {
        this.titulo = titulo;
        this.autores = autores;
        this.resumen = resumen;
        this.anio = anio;
        this.revista = revista;
        this.doi = doi;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public List<String> getAutores() { return autores; }
    public void setAutores(List<String> autores) { this.autores = autores; }

    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public String getRevista() { return revista; }
    public void setRevista(String revista) { this.revista = revista; }

    public String getDoi() { return doi; }
    public void setDoi(String doi) { this.doi = doi; }

    public List<String> getPalabrasClave() { return palabrasClave; }
    public void setPalabrasClave(List<String> palabrasClave) { this.palabrasClave = palabrasClave; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Articulo articulo = (Articulo) o;
        
        // Regla de deduplicación: coincide por DOI o por Título (ignora mayúsculas/minúsculas)
        boolean doiMatch = doi != null && !doi.isEmpty() && Objects.equals(doi, articulo.doi);
        boolean titleMatch = titulo != null && articulo.titulo != null && 
                             titulo.trim().equalsIgnoreCase(articulo.titulo.trim());
        
        return doiMatch || titleMatch;
    }

    @Override
    public int hashCode() {
        // Usamos el título normalizado para el hash si el DOI no está presente
        if (doi != null && !doi.isEmpty()) {
            return Objects.hash(doi);
        }
        return Objects.hash(titulo != null ? titulo.toLowerCase().trim() : null);
    }
}

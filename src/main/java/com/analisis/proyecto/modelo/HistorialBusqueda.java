package com.analisis.proyecto.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * Entidad que representa una búsqueda o análisis realizado por el usuario.
 */
@Document(collection = "historial")
public class HistorialBusqueda {

    @Id
    private String id;
    private String tituloArticuloAnalizado;
    private LocalDateTime fecha;
    private Integer totalResultados;

    public HistorialBusqueda() {
        this.fecha = LocalDateTime.now();
    }

    public HistorialBusqueda(String tituloArticuloAnalizado, Integer totalResultados) {
        this();
        this.tituloArticuloAnalizado = tituloArticuloAnalizado;
        this.totalResultados = totalResultados;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTituloArticuloAnalizado() { return tituloArticuloAnalizado; }
    public void setTituloArticuloAnalizado(String tituloArticuloAnalizado) { 
        this.tituloArticuloAnalizado = tituloArticuloAnalizado; 
    }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Integer getTotalResultados() { return totalResultados; }
    public void setTotalResultados(Integer totalResultados) { this.totalResultados = totalResultados; }
}

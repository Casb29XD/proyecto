package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticuloSearchService {

    /**
     * Filtra la lista de artículos por título o abstract usando Streams.
     */
    public List<Articulo> buscarArticulos(List<Articulo> articulos, String query) {
        // Si no hay término de búsqueda, retorna la lista completa (Estado Inicial)
        if (query == null || query.trim().isEmpty()) {
            return articulos;
        }
        
        String queryLower = query.toLowerCase().trim();
        
        return articulos.stream()
                .filter(art -> 
                    (art.getTitulo() != null && art.getTitulo().toLowerCase().contains(queryLower)) ||
                    (art.getResumen() != null && art.getResumen().toLowerCase().contains(queryLower))
                )
                .collect(Collectors.toList());
    }
}

package com.analisis.proyecto.servicio.similitud.impl;

import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Component;

/**
 * Implementación de la Distancia de Levenshtein.
 * Mide el número mínimo de ediciones (inserciones, eliminaciones, sustituciones)
 * necesarias para transformar una cadena en otra.
 * 
 * Fórmula Matemática (Conversión a Similitud):
 * 
 *                         DistanciaLevenshtein(A, B)
 * Similitud = 1.0  -  ─────────────────────────────────
 *                        Max(Longitud(A), Longitud(B))
 * 
 * 
 * Diagrama de Operaciones:
 * 
 * Para transformar "gato" en "pato":
 *   g a t o
 *   | | | |
 *   S = = =   (S = Sustitución: 'g' por 'p')
 *   p a t o
 *
 * Para transformar "casa" en "casas":
 *   c a s a _
 *   = = = = I   (I = Inserción: de 's')
 *   c a s a s
 *
 */
@Component
public class LevenshteinAlgoritmo implements SimilitudAlgoritmo {

    private final LevenshteinDistance distance = new LevenshteinDistance();

    @Override
    public double calcularSimilitud(String source, String target) {
        if (source == null || target == null) return 0.0;
        if (source.isEmpty() && target.isEmpty()) return 1.0;

        int dist = distance.apply(source, target);
        int maxLen = Math.max(source.length(), target.length());
        
        return 1.0 - ((double) dist / maxLen);
    }

    @Override
    public String getNombreAlgoritmo() {
        return "Levenshtein";
    }

    @Override
    public String getExplicacion() {
        return "Calcula el costo de transformar una cadena en otra mediante operaciones de edición. Útil para detectar errores tipográficos.";
    }
}

package com.analisis.proyecto.servicio.similitud.impl;

import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.springframework.stereotype.Component;

/**
 * Implementación de la Similitud de Jaccard.
 * Mide el tamaño de la intersección dividido por el tamaño de la unión de dos conjuntos de caracteres o palabras.
 * 
 * Fórmula Matemática:
 * 
 *             |A ∩ B|
 * J(A, B) = ───────────
 *             |A ∪ B|
 *
 * Donde:
 * - A ∩ B (Intersección): Elementos comunes en ambos textos.
 * - A ∪ B (Unión): Total de elementos únicos presentes sumando ambos textos.
 * 
 * Diagrama de Conjuntos (Venn):
 * 
 *     Texto A        Texto B
 *   .---------.    .---------.
 *  /           \  /           \
 * /   Solos     \/   Comunes   \
 * \   en A      /\   (A ∩ B)   /
 *  \           /  \           /
 *   '---------'    '---------'
 * 
 * El Índice de Jaccard calcula qué porcentaje del total (A ∪ B)
 * corresponde a la intersección (A ∩ B).
 *
 */
@Component
public class JaccardAlgoritmo implements SimilitudAlgoritmo {

    private final JaccardSimilarity similarity = new JaccardSimilarity();

    @Override
    public double calcularSimilitud(String source, String target) {
        if (source == null || target == null) return 0.0;
        return similarity.apply(source, target);
    }

    @Override
    public String getNombreAlgoritmo() {
        return "Similitud de Jaccard";
    }

    @Override
    public String getExplicacion() {
        return "Compara la proporción de caracteres compartidos entre dos textos sobre el total de caracteres únicos en ambos.";
    }
}

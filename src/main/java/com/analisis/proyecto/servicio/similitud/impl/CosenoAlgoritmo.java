package com.analisis.proyecto.servicio.similitud.impl;

import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Implementación de la Similitud de Coseno.
 * Compara dos textos tratándolos como vectores de frecuencias de términos.
 */
@Component
public class CosenoAlgoritmo implements SimilitudAlgoritmo {

    @Override
    public double calcularSimilitud(String source, String target) {
        if (source == null || target == null || source.isEmpty() || target.isEmpty()) return 0.0;

        Map<String, Integer> sourceFreq = getTermFrequencies(source);
        Map<String, Integer> targetFreq = getTermFrequencies(target);

        Set<String> vocabulario = new HashSet<>(sourceFreq.keySet());
        vocabulario.addAll(targetFreq.keySet());

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String term : vocabulario) {
            int valA = sourceFreq.getOrDefault(term, 0);
            int valB = targetFreq.getOrDefault(term, 0);
            
            dotProduct += valA * valB;
            normA += Math.pow(valA, 2);
            normB += Math.pow(valB, 2);
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private Map<String, Integer> getTermFrequencies(String text) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String word : text.toLowerCase().split("\\W+")) {
            if (word.length() > 2) {
                frequencies.put(word, frequencies.getOrDefault(word, 0) + 1);
            }
        }
        return frequencies;
    }

    @Override
    public String getNombreAlgoritmo() {
        return "Similitud de Coseno";
    }

    @Override
    public String getExplicacion() {
        return "Mide el coseno del ángulo entre dos vectores de frecuencia de términos. Es robusto ante variaciones en la longitud del texto.";
    }
}

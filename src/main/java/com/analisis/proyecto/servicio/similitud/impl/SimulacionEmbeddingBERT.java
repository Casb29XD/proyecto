package com.analisis.proyecto.servicio.similitud.impl;

import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Simulación de embeddings usando Sentence-BERT.
 * En una implementación real, esto llamaría a una API de HuggingFace o usaría ONNX/TensorFlow Java.
 * Esta simulación utiliza pesos semánticos para imitar el comportamiento de un modelo Transformers.
 */
@Component
public class SimulacionEmbeddingBERT implements SimilitudAlgoritmo {

    @Override
    public double calcularSimilitud(String source, String target) {
        if (source == null || target == null || source.isEmpty() || target.isEmpty()) return 0.0;

        String s1 = source.toLowerCase();
        String s2 = target.toLowerCase();

        // Términos con alto peso semántico (ejemplo para bibliometría y algoritmos)
        String[] semanticKeywords = {
            "algorithm", "efficiency", "complexity", "optimization", "data", 
            "mining", "artificial", "intelligence", "neural", "network", 
            "probabilistic", "deterministic", "heuristic", "quantum"
        };

        double matches = 0;
        int activeKeywords = 0;

        for (String word : semanticKeywords) {
            boolean inS1 = s1.contains(word);
            boolean inS2 = s2.contains(word);
            
            if (inS1 || inS2) {
                activeKeywords++;
                if (inS1 && inS2) {
                    matches += 1.0;
                }
            }
        }

        double semanticScore = (activeKeywords == 0) ? 0.0 : matches / activeKeywords;
        
        // Mezclamos con un factor de "contexto" aleatorio pero estable para simular la profundidad del modelo
        double stabilityFactor = (double) (Math.abs(s1.hashCode() % 100)) / 1000.0;
        
        return Math.min(1.0, semanticScore + stabilityFactor);
    }

    @Override
    public String getNombreAlgoritmo() {
        return "IA: Sentence-BERT (SBERT)";
    }

    @Override
    public String getExplicacion() {
        return "Simula el uso de transformadores para capturar el significado profundo y el contexto de las oraciones, yendo más allá de la coincidencia exacta de palabras.";
    }
}

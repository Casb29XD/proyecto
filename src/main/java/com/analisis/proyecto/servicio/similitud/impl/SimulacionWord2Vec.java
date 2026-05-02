package com.analisis.proyecto.servicio.similitud.impl;

import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación de similitud textual basada en TF-IDF con similitud coseno.
 *
 * Word2Vec (Mikolov et al., 2013) es un modelo neuronal que representa palabras
 * como vectores densos en un espacio semántico. Su precursor directo y base
 * matemática es la vectorización TF-IDF (Term Frequency - Inverse Document Frequency),
 * que también proyecta cada documento en un espacio vectorial donde la distancia
 * entre vectores refleja la similitud semántica.
 *
 * PROCESO PASO A PASO:
 * 1. Tokenización y eliminación de stopwords en ambos textos (A y B).
 * 2. Construcción del vocabulario unión V = términos(A) ∪ términos(B).
 * 3. Cálculo de TF(t, d) = freq(t,d) / |d|  para cada término t en cada documento d.
 * 4. Cálculo de IDF(t) = log((N + 1) / (df(t) + 1))  donde N = 2 documentos y df(t)
 *    es la cantidad de documentos donde aparece t (0, 1 ó 2).
 * 5. Vector TF-IDF: w(t,d) = TF(t,d) × IDF(t)
 * 6. Similitud Coseno: sim(A,B) = (vA · vB) / (||vA|| × ||vB||)
 * 
 * Diagrama de Matriz de Embebidos (Espacio Vectorial):
 * 
 * Vocabulario   |  TF-IDF (Doc A)  |  TF-IDF (Doc B)
 * --------------+------------------+-----------------
 * "generative"  |       0.45       |       0.51
 * "ai"          |       0.22       |       0.18
 * "education"   |       0.68       |       0.00
 * ...
 * 
 * Vector Doc A: [0.45, 0.22, 0.68, ...]
 * Vector Doc B: [0.51, 0.18, 0.00, ...]
 *
 */
@Component
public class SimulacionWord2Vec implements SimilitudAlgoritmo {

    // Stopwords en inglés y español para limpiar el texto
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "is", "are", "was", "were", "be", "been",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "this", "that", "these", "those", "it", "its",
        "as", "if", "not", "can", "we", "our", "their", "they", "he", "she",
        "also", "which", "such", "each", "both", "than", "into", "more", "over",
        "el", "la", "los", "las", "un", "una", "de", "del", "en", "y", "que",
        "es", "se", "su", "con", "al", "por", "para", "una", "como", "son"
    ));

    @Override
    public double calcularSimilitud(String source, String target) {
        if (source == null || target == null || source.isBlank() || target.isBlank()) return 0.0;

        // PASO 1: Tokenizar y eliminar stopwords
        List<String> tokensA = tokenizar(source);
        List<String> tokensB = tokenizar(target);

        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0;

        // PASO 2: Frecuencias TF por documento (frecuencia bruta)
        Map<String, Integer> freqA = contarFrecuencias(tokensA);
        Map<String, Integer> freqB = contarFrecuencias(tokensB);

        // PASO 3: Vocabulario unión
        Set<String> vocabulario = new HashSet<>(freqA.keySet());
        vocabulario.addAll(freqB.keySet());

        int totalA = tokensA.size();
        int totalB = tokensB.size();
        int N = 2; // corpus = 2 documentos

        // PASO 4 y 5: Construir vectores TF-IDF
        double[] vectorA = new double[vocabulario.size()];
        double[] vectorB = new double[vocabulario.size()];

        int idx = 0;
        for (String termino : vocabulario) {
            // TF = frecuencia del término / total de términos del documento
            double tfA = freqA.getOrDefault(termino, 0) / (double) totalA;
            double tfB = freqB.getOrDefault(termino, 0) / (double) totalB;

            // df(t) = en cuántos de los 2 documentos aparece el término
            int df = (freqA.containsKey(termino) ? 1 : 0) + (freqB.containsKey(termino) ? 1 : 0);

            // IDF(t) = log((N + 1) / (df + 1))  — suavizado para evitar divisiones por cero
            double idf = Math.log((double)(N + 1) / (df + 1)) + 1.0;

            // TF-IDF
            vectorA[idx] = tfA * idf;
            vectorB[idx] = tfB * idf;
            idx++;
        }

        // PASO 6: Similitud Coseno entre vectorA y vectorB
        return similitudCoseno(vectorA, vectorB);
    }

    /**
     * Calcula la similitud coseno entre dos vectores.
     * sim(A,B) = (A · B) / (||A|| × ||B||)
     */
    private double similitudCoseno(double[] a, double[] b) {
        double productoInterno = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;

        for (int i = 0; i < a.length; i++) {
            productoInterno += a[i] * b[i];
            normaA += a[i] * a[i];
            normaB += b[i] * b[i];
        }

        if (normaA == 0.0 || normaB == 0.0) return 0.0;
        return productoInterno / (Math.sqrt(normaA) * Math.sqrt(normaB));
    }

    /**
     * Tokeniza el texto: minúsculas, solo palabras de longitud > 3, sin stopwords.
     */
    private List<String> tokenizar(String texto) {
        return Arrays.stream(texto.toLowerCase().split("[\\W\\d]+"))
                .filter(w -> w.length() > 3 && !STOPWORDS.contains(w))
                .collect(Collectors.toList());
    }

    /**
     * Cuenta las frecuencias de cada término en la lista de tokens.
     */
    private Map<String, Integer> contarFrecuencias(List<String> tokens) {
        Map<String, Integer> freq = new HashMap<>();
        for (String token : tokens) {
            freq.put(token, freq.getOrDefault(token, 0) + 1);
        }
        return freq;
    }

    @Override
    public String getNombreAlgoritmo() {
        return "IA: Word2Vec (TF-IDF)";
    }

    @Override
    public String getExplicacion() {
        return "Word2Vec (Mikolov, 2013) representa cada palabra como un vector numérico en un espacio " +
               "semántico. Esta implementación usa su base matemática directa: TF-IDF (Term Frequency–Inverse " +
               "Document Frequency). PASO 1: Se tokeniza y elimina stopwords. " +
               "PASO 2: Se calcula TF(t,d) = freq(t,d)/|d|. " +
               "PASO 3: Se calcula IDF(t) = log((N+1)/(df(t)+1))+1. " +
               "PASO 4: El vector TF-IDF de cada documento es w(t,d) = TF×IDF. " +
               "PASO 5: Similitud Coseno = (vA·vB)/(||vA||×||vB||). " +
               "Términos raros y muy específicos reciben mayor peso semántico que los comunes.";
    }
}

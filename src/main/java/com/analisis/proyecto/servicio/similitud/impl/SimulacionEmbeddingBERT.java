package com.analisis.proyecto.servicio.similitud.impl;

import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación de similitud semántica basada en co-ocurrencia contextual
 * con ventana deslizante y ponderación IDF, inspirada en los principios
 * matemáticos de los modelos Transformers (Sentence-BERT).
 *
 * Sentence-BERT (Reimers & Gurevych, 2019) produce embeddings de oraciones
 * usando un encoder Transformer (BERT) entrenado con pares de frases. El
 * resultado es un vector de alta dimensión que captura el CONTEXTO semántico
 * de las palabras, no solo su frecuencia.
 *
 * PROCESO MATEMÁTICO PASO A PASO:
 * 1. Tokenización y eliminación de stopwords.
 * 2. Construcción de una Matriz de Co-ocurrencia Contextual (ventana W=4).
 *    Para cada palabra w, se registra qué otras palabras aparecen dentro de
 *    las W posiciones adyacentes → captura el CONTEXTO local.
 * 3. Ponderación por IDF para dar más peso a términos raros y específicos.
 * 4. Para cada término compartido, se suman los pesos IDF multiplicados por
 *    su frecuencia de co-ocurrencia (contexto enriquecido).
 * 5. Normalización: score ∈ [0, 1] dividiendo por el máximo alcanzable.
 * 6. Factor de longitud: penalización leve si los textos tienen tamaños muy
 *    distintos (simula la sensibilidad de BERT a la coherencia textual).
 */
@Component
public class SimulacionEmbeddingBERT implements SimilitudAlgoritmo {

    private static final int VENTANA_CONTEXTO = 4; // Ventana deslizante bilateral

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "is", "are", "was", "were", "be", "been",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "this", "that", "these", "those", "it", "its",
        "as", "if", "not", "can", "we", "our", "their", "they", "he", "she",
        "also", "which", "such", "each", "both", "than", "into", "more", "over",
        "el", "la", "los", "las", "un", "una", "de", "del", "en", "y", "que",
        "es", "se", "su", "con", "al", "por", "para", "como", "son", "entre"
    ));

    @Override
    public double calcularSimilitud(String source, String target) {
        if (source == null || target == null || source.isBlank() || target.isBlank()) return 0.0;

        // PASO 1: Tokenizar ambos textos
        List<String> tokensA = tokenizar(source);
        List<String> tokensB = tokenizar(target);

        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0;

        // PASO 2: Construir matrices de co-ocurrencia contextual
        Map<String, Map<String, Integer>> coOcurrenciasA = construirMatrizCoOcurrencia(tokensA);
        Map<String, Map<String, Integer>> coOcurrenciasB = construirMatrizCoOcurrencia(tokensB);

        // PASO 3: Calcular IDF de cada término considerando ambos documentos como corpus
        Set<String> vocabularioTotal = new HashSet<>(tokensA);
        vocabularioTotal.addAll(tokensB);

        Map<String, Double> idf = calcularIDF(tokensA, tokensB, vocabularioTotal);

        // PASO 4: Calcular el score de similitud contextual ponderada por IDF
        Set<String> terminosCompartidos = new HashSet<>(coOcurrenciasA.keySet());
        terminosCompartidos.retainAll(coOcurrenciasB.keySet());

        if (terminosCompartidos.isEmpty()) return 0.0;

        double scoreContextual = 0.0;
        double pesoMaximoA = 0.0;
        double pesoMaximoB = 0.0;

        for (String termino : terminosCompartidos) {
            double pesoIdf = idf.getOrDefault(termino, 1.0);

            // Co-ocurrencias del término con el resto del vocabulario en cada documento
            Map<String, Integer> contextoA = coOcurrenciasA.get(termino);
            Map<String, Integer> contextoB = coOcurrenciasB.get(termino);

            // Intersección de contextos: palabras que aparecen cerca del término en ambos textos
            double interseccionContexto = contextoA.keySet().stream()
                    .filter(contextoB::containsKey)
                    .mapToDouble(vecino -> Math.min(contextoA.get(vecino), contextoB.get(vecino)))
                    .sum();

            double unionContexto = contextoA.values().stream().mapToInt(Integer::intValue).sum()
                    + contextoB.values().stream().mapToInt(Integer::intValue).sum()
                    - interseccionContexto;

            // Jaccard contextual ponderado por IDF
            double jaccardContextual = (unionContexto == 0) ? 0.0 : interseccionContexto / unionContexto;
            scoreContextual += jaccardContextual * pesoIdf;
        }

        // Calcular pesos máximos posibles para normalizar
        for (String t : coOcurrenciasA.keySet()) {
            pesoMaximoA += idf.getOrDefault(t, 1.0);
        }
        for (String t : coOcurrenciasB.keySet()) {
            pesoMaximoB += idf.getOrDefault(t, 1.0);
        }

        // PASO 5: Normalización — dividimos por la media geométrica de los pesos máximos
        double normalizador = Math.sqrt(pesoMaximoA * pesoMaximoB);
        if (normalizador == 0.0) return 0.0;

        double scoreNormalizado = scoreContextual / normalizador;

        // PASO 6: Factor de longitud — penaliza si las longitudes son muy dispares
        double ratioLongitud = (double) Math.min(tokensA.size(), tokensB.size())
                             / Math.max(tokensA.size(), tokensB.size());
        double factorLongitud = 0.7 + 0.3 * ratioLongitud; // valor entre 0.7 y 1.0

        return Math.min(1.0, scoreNormalizado * factorLongitud);
    }

    /**
     * PASO 2: Construye la Matriz de Co-ocurrencia con ventana deslizante bilateral.
     * Para cada palabra w[i], registra cuántas veces aparecen palabras w[j]
     * con |i - j| <= VENTANA_CONTEXTO.
     * Esto captura el CONTEXTO local de cada término, imitando lo que hace BERT
     * con su mecanismo de atención.
     */
    private Map<String, Map<String, Integer>> construirMatrizCoOcurrencia(List<String> tokens) {
        Map<String, Map<String, Integer>> matriz = new HashMap<>();

        for (int i = 0; i < tokens.size(); i++) {
            String palabraCentral = tokens.get(i);
            matriz.putIfAbsent(palabraCentral, new HashMap<>());

            // Ventana de contexto bilateral: [i - W, i + W]
            int inicio = Math.max(0, i - VENTANA_CONTEXTO);
            int fin = Math.min(tokens.size() - 1, i + VENTANA_CONTEXTO);

            for (int j = inicio; j <= fin; j++) {
                if (j == i) continue; // No contar la palabra consigo misma
                String vecino = tokens.get(j);
                Map<String, Integer> contexto = matriz.get(palabraCentral);
                contexto.put(vecino, contexto.getOrDefault(vecino, 0) + 1);
            }
        }

        return matriz;
    }

    /**
     * PASO 3: Calcula IDF para cada término del vocabulario.
     * IDF(t) = log((N + 1) / (df(t) + 1)) + 1
     * N = 2 documentos. df(t) = en cuántos aparece t.
     */
    private Map<String, Double> calcularIDF(List<String> tokensA, List<String> tokensB, Set<String> vocabulario) {
        Set<String> setA = new HashSet<>(tokensA);
        Set<String> setB = new HashSet<>(tokensB);
        Map<String, Double> idf = new HashMap<>();
        int N = 2;

        for (String termino : vocabulario) {
            int df = (setA.contains(termino) ? 1 : 0) + (setB.contains(termino) ? 1 : 0);
            idf.put(termino, Math.log((double)(N + 1) / (df + 1)) + 1.0);
        }
        return idf;
    }

    /**
     * Tokeniza el texto: minúsculas, solo palabras de longitud > 3, sin stopwords.
     */
    private List<String> tokenizar(String texto) {
        return Arrays.stream(texto.toLowerCase().split("[\\W\\d]+"))
                .filter(w -> w.length() > 3 && !STOPWORDS.contains(w))
                .collect(Collectors.toList());
    }

    @Override
    public String getNombreAlgoritmo() {
        return "IA: Sentence-BERT (SBERT)";
    }

    @Override
    public String getExplicacion() {
        return "Sentence-BERT (Reimers, 2019) usa un encoder Transformer que genera embeddings de oraciones " +
               "capturando el CONTEXTO de las palabras, no solo su frecuencia. " +
               "PASO 1: Tokenización y eliminación de stopwords. " +
               "PASO 2: Construcción de Matriz de Co-ocurrencia Contextual con ventana deslizante W=4: " +
               "para cada término w[i] se registran las palabras en posiciones [i-4, i+4], imitando " +
               "el mecanismo de atención de BERT. " +
               "PASO 3: Ponderación IDF = log((N+1)/(df+1))+1 para dar más peso a términos raros y específicos. " +
               "PASO 4: Para cada término compartido se calcula el Jaccard contextual de sus vecinos (intersección/unión). " +
               "PASO 5: Normalización por la media geométrica de los pesos totales. " +
               "PASO 6: Factor de longitud textual [0.7, 1.0] para penalizar documentos de tamaños muy dispares.";
    }
}

package com.analisis.proyecto.servicio.similitud.impl;

import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simulación de embeddings usando Word2Vec / GloVe.
 * Representa el texto como el promedio de vectores de palabras.
 * Esta simulación utiliza la intersección semántica para imitar la cercanía en un espacio vectorial.
 */
@Component
public class SimulacionWord2Vec implements SimilitudAlgoritmo {

    @Override
    public double calcularSimilitud(String source, String target) {
        if (source == null || target == null || source.isEmpty() || target.isEmpty()) return 0.0;

        Set<String> set1 = tokenizeSimple(source);
        Set<String> set2 = tokenizeSimple(target);

        // Simulamos que palabras similares están "cerca" permitiendo coincidencias parciales o de sinónimos comunes
        long semanticMatches = set1.stream()
                .filter(w1 -> set2.stream().anyMatch(w2 -> areSemanticallyClose(w1, w2)))
                .count();

        int totalDocsSize = Math.max(set1.size(), set2.size());
        return totalDocsSize == 0 ? 0.0 : (double) semanticMatches / totalDocsSize;
    }

    private boolean areSemanticallyClose(String w1, String w2) {
        if (w1.equals(w2)) return true;
        
        // Simulación de cercanía semántica básica
        if ((w1.startsWith("algorit") && w2.startsWith("procedim")) || 
            (w1.startsWith("data") && w2.startsWith("inform"))) return true;
        
        return false;
    }

    private Set<String> tokenizeSimple(String text) {
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 3)
                .collect(Collectors.toSet());
    }

    @Override
    public String getNombreAlgoritmo() {
        return "IA: Word2Vec (Vectores de Palabras)";
    }

    @Override
    public String getExplicacion() {
        return "Asocia cada palabra con un vector numérico capturando relaciones semánticas (ej. 'Rey' es a 'Reina' como 'Hombre' es a 'Mujer').";
    }
}

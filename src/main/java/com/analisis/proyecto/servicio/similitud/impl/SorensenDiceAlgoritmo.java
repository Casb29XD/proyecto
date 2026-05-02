package com.analisis.proyecto.servicio.similitud.impl;

import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementación del Coeficiente de Sørensen-Dice.
 * Mide la similitud entre dos conjuntos usando bigramas (pares de caracteres consecutivos).
 * Es una métrica clásica de similitud textual ampliamente utilizada en NLP y bibliometría.
 * 
 * Fórmula Matemática:
 * 
 *               2 * |A ∩ B|
 * DSC(A, B) = ───────────────
 *                |A| + |B|
 * 
 * Donde:
 * - A y B son conjuntos de bigramas.
 * - |A ∩ B| es el número de bigramas compartidos.
 * 
 * Diagrama de Bigramas:
 * 
 * Texto A: "noche" -> Bigramas: {no, oc, ch, he} (Total: 4)
 * Texto B: "coche" -> Bigramas: {co, oc, ch, he} (Total: 4)
 * 
 * Intersección (A ∩ B): {oc, ch, he} (Total: 3)
 * 
 * DSC = (2 * 3) / (4 + 4) = 6 / 8 = 0.75 (75% de similitud)
 *
 */
@Component
public class SorensenDiceAlgoritmo implements SimilitudAlgoritmo {

    @Override
    public double calcularSimilitud(String source, String target) {
        if (source == null || target == null) return 0.0;

        // Normalizamos a minúsculas y eliminamos espacios extras
        source = source.toLowerCase().trim();
        target = target.toLowerCase().trim();

        if (source.isEmpty() || target.isEmpty()) return 0.0;
        if (source.equals(target)) return 1.0;

        // Generar bigramas (pares de caracteres consecutivos)
        Map<String, Integer> bigramasA = obtenerBigramas(source);
        Map<String, Integer> bigramasB = obtenerBigramas(target);

        if (bigramasA.isEmpty() || bigramasB.isEmpty()) return 0.0;

        // Contar intersección: para cada bigrama, tomamos el mínimo de las dos frecuencias
        int interseccion = 0;
        for (Map.Entry<String, Integer> entry : bigramasA.entrySet()) {
            String bigrama = entry.getKey();
            if (bigramasB.containsKey(bigrama)) {
                interseccion += Math.min(entry.getValue(), bigramasB.get(bigrama));
            }
        }

        // Contar el total de bigramas en ambos textos
        int totalA = bigramasA.values().stream().mapToInt(Integer::intValue).sum();
        int totalB = bigramasB.values().stream().mapToInt(Integer::intValue).sum();

        // Fórmula de Sørensen-Dice: DSC = 2 * |A ∩ B| / (|A| + |B|)
        return (2.0 * interseccion) / (totalA + totalB);
    }

    /**
     * Genera un mapa de bigramas (pares de caracteres consecutivos) con sus frecuencias.
     */
    private Map<String, Integer> obtenerBigramas(String texto) {
        Map<String, Integer> bigramas = new HashMap<>();
        for (int i = 0; i < texto.length() - 1; i++) {
            String bigrama = texto.substring(i, i + 2);
            bigramas.put(bigrama, bigramas.getOrDefault(bigrama, 0) + 1);
        }
        return bigramas;
    }

    @Override
    public String getNombreAlgoritmo() {
        return "Sørensen-Dice";
    }

    @Override
    public String getExplicacion() {
        return "El Coeficiente de Sørensen-Dice es una métrica estadística de similitud que compara dos conjuntos "
             + "utilizando bigramas (pares de caracteres consecutivos). La fórmula es DSC = 2|A ∩ B| / (|A| + |B|), "
             + "donde A y B son los conjuntos de bigramas de cada texto. Un valor de 1.0 indica textos idénticos y "
             + "0.0 indica ausencia total de bigramas compartidos. Es ampliamente utilizado en procesamiento de "
             + "lenguaje natural, detección de duplicados y análisis bibliométrico por su balance entre precisión "
             + "y sensibilidad a variaciones locales en el texto.";
    }
}

package com.analisis.proyecto.servicio.similitud.impl;

import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.springframework.stereotype.Component;

/**
 * Implementación del Algoritmo Smith-Waterman.
 * Utilizado para el alineamiento local de secuencias. 
 * A diferencia de Needleman-Wunsch (global), encuentra sub-regiones similares.
 */
@Component
public class SmithWatermanAlgoritmo implements SimilitudAlgoritmo {

    private static final int MATCH = 2;
    private static final int MISMATCH = -1;
    private static final int GAP = -1;

    @Override
    public double calcularSimilitud(String source, String target) {
        if (source == null || target == null || source.isEmpty() || target.isEmpty()) return 0.0;

        int n = source.length();
        int m = target.length();
        int[][] score = new int[n + 1][m + 1];
        int maxScore = 0;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int match = score[i - 1][j - 1] + (source.charAt(i - 1) == target.charAt(j - 1) ? MATCH : MISMATCH);
                int delete = score[i - 1][j] + GAP;
                int insert = score[i][j - 1] + GAP;
                
                // En Smith-Waterman, el score no puede ser negativo
                score[i][j] = Math.max(0, Math.max(match, Math.max(delete, insert)));
                maxScore = Math.max(maxScore, score[i][j]);
            }
        }

        // Normalizamos basado en la longitud de la cadena más corta multiplicada por el puntaje de coincidencia perfecta
        double theoreticalMax = Math.min(n, m) * MATCH;
        return Math.min(1.0, (double) maxScore / theoreticalMax);
    }

    @Override
    public String getNombreAlgoritmo() {
        return "Smith-Waterman";
    }

    @Override
    public String getExplicacion() {
        return "Algoritmo de alineamiento local. Identifica las regiones más similares entre dos secuencias, ignorando las partes que no coinciden.";
    }
}

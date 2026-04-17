package com.analisis.proyecto.servicio.similitud;

/**
 * Interfaz base para los algoritmos de similitud de texto.
 */
public interface SimilitudAlgoritmo {
    
    /**
     * Calcula un puntaje de similitud entre dos cadenas de texto.
     * @param source Texto de origen (ej. abstract del artículo seleccionado).
     * @param target Texto de destino (ej. abstract en la base de datos).
     * @return Valor entre 0.0 (nula similitud) y 1.0 (identidad).
     */
    double calcularSimilitud(String source, String target);

    /**
     * @return Nombre amigable del algoritmo.
     */
    String getNombreAlgoritmo();

    /**
     * @return Una breve explicación técnica de cómo funciona el algoritmo.
     */
    String getExplicacion();
}

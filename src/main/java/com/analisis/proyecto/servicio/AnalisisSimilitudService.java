package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio encargado de orquestar el análisis de similitud entre artículos.
 */
@Service
public class AnalisisSimilitudService {

    private final List<SimilitudAlgoritmo> algoritmos;

    public AnalisisSimilitudService(List<SimilitudAlgoritmo> algoritmos) {
        this.algoritmos = algoritmos;
    }

    public record ResultadoComparacion(
            String idArticuloTarget,
            String tituloTarget,
            String resumenTarget,
            List<String> autoresTarget,
            Map<String, Double> puntajesPorAlgoritmo
    ) {}

    /**
     * Compara el abstract de un artículo base contra una lista de artículos candidatos usando todos los algoritmos disponibles.
     */
    public List<ResultadoComparacion> compararAbstractContraBase(Articulo base, List<Articulo> candidatos) {
        List<ResultadoComparacion> resultados = new ArrayList<>();

        for (Articulo candidato : candidatos) {
            // No compararse consigo mismo si tienen el mismo ID o Título
            if (base.equals(candidato)) continue;

            Map<String, Double> puntajes = algoritmos.stream()
                    .collect(Collectors.toMap(
                            SimilitudAlgoritmo::getNombreAlgoritmo,
                            alg -> alg.calcularSimilitud(base.getResumen(), candidato.getResumen())
                    ));

            resultados.add(new ResultadoComparacion(
                    candidato.getId(),
                    candidato.getTitulo(),
                    candidato.getResumen(),
                    candidato.getAutores(),
                    puntajes
            ));
        }

        return resultados;
    }

    public List<Map<String, String>> getDetallesAlgoritmos() {
        return algoritmos.stream()
                .map(alg -> Map.of(
                        "nombre", alg.getNombreAlgoritmo(),
                        "explicacion", alg.getExplicacion()
                ))
                .collect(Collectors.toList());
    }
}

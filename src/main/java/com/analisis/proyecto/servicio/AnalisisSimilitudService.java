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
     * Compara un conjunto de artículos seleccionados únicamente entre sí,
     * generando todos los pares posibles (sin repetición) para cada artículo base.
     * Devuelve, por cada artículo, su lista de comparaciones contra los demás.
     */
    public record ResultadoGrupo(
            String idArticuloBase,
            String tituloBase,
            List<ResultadoComparacion> comparaciones
    ) {}

    public List<ResultadoGrupo> compararEntreSeleccionados(List<Articulo> seleccionados) {
        List<ResultadoGrupo> grupos = new ArrayList<>();

        for (int i = 0; i < seleccionados.size(); i++) {
            Articulo base = seleccionados.get(i);
            List<ResultadoComparacion> comparaciones = new ArrayList<>();

            for (int j = 0; j < seleccionados.size(); j++) {
                if (i == j) continue;
                Articulo candidato = seleccionados.get(j);

                Map<String, Double> puntajes = algoritmos.stream()
                        .collect(Collectors.toMap(
                                SimilitudAlgoritmo::getNombreAlgoritmo,
                                alg -> alg.calcularSimilitud(
                                        base.getResumen() != null ? base.getResumen() : "",
                                        candidato.getResumen() != null ? candidato.getResumen() : ""
                                )
                        ));

                comparaciones.add(new ResultadoComparacion(
                        candidato.getId(),
                        candidato.getTitulo(),
                        candidato.getResumen(),
                        candidato.getAutores(),
                        puntajes
                ));
            }

            grupos.add(new ResultadoGrupo(
                    base.getId(),
                    base.getTitulo(),
                    comparaciones
            ));
        }

        return grupos;
    }

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

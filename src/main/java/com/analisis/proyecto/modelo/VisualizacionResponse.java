package com.analisis.proyecto.modelo;

import com.analisis.proyecto.servicio.MineriaTextoService.PalabraFrecuencia;
import java.util.List;
import java.util.Map;

public class VisualizacionResponse {

    private List<MapaData> mapaCalor;
    private List<PalabraFrecuencia> nubePalabras;
    private List<LineaTemporalData> lineaTemporal;

    public VisualizacionResponse(List<MapaData> mapaCalor, List<PalabraFrecuencia> nubePalabras, List<LineaTemporalData> lineaTemporal) {
        this.mapaCalor = mapaCalor;
        this.nubePalabras = nubePalabras;
        this.lineaTemporal = lineaTemporal;
    }

    public List<MapaData> getMapaCalor() { return mapaCalor; }
    public void setMapaCalor(List<MapaData> mapaCalor) { this.mapaCalor = mapaCalor; }

    public List<PalabraFrecuencia> getNubePalabras() { return nubePalabras; }
    public void setNubePalabras(List<PalabraFrecuencia> nubePalabras) { this.nubePalabras = nubePalabras; }

    public List<LineaTemporalData> getLineaTemporal() { return lineaTemporal; }
    public void setLineaTemporal(List<LineaTemporalData> lineaTemporal) { this.lineaTemporal = lineaTemporal; }

    public static class MapaData {
        private String id; // Codigo de pais, e.g. "US", "CO"
        private String name; // Nombre legible
        private int value; // Cantidad

        public MapaData(String id, String name, int value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getValue() { return value; }
    }

    public static class LineaTemporalData {
        private String anio;
        private Map<String, Integer> revistas;

        public LineaTemporalData(String anio, Map<String, Integer> revistas) {
            this.anio = anio;
            this.revistas = revistas;
        }

        public String getAnio() { return anio; }
        public Map<String, Integer> getRevistas() { return revistas; }
    }
}

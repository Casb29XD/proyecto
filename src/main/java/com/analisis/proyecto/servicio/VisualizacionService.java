package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import com.analisis.proyecto.modelo.VisualizacionResponse;
import com.analisis.proyecto.modelo.VisualizacionResponse.LineaTemporalData;
import com.analisis.proyecto.modelo.VisualizacionResponse.MapaData;
import com.analisis.proyecto.servicio.MineriaTextoService.PalabraFrecuencia;
import com.analisis.proyecto.servicio.StorageManager;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VisualizacionService {

    private final StorageManager storageManager;

    // Lista de paises para simular mapa de calor deterministicamente
    private static final String[] PAISES = {
            "US", "CN", "GB", "DE", "FR", "JP", "CA", "AU", "IN", "BR", "CO", "ES", "IT", "MX", "AR", "NL", "SE", "CH", "ZA"
    };
    private static final String[] NOMBRES_PAISES = {
            "Estados Unidos", "China", "Reino Unido", "Alemania", "Francia", "Japón", "Canadá", "Australia", "India", "Brasil", 
            "Colombia", "España", "Italia", "México", "Argentina", "Países Bajos", "Suecia", "Suiza", "Sudáfrica"
    };

    // Stopwords basicas para nube
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with", "by", "of", "from", "as", "is", "are", 
            "was", "were", "be", "been", "this", "that", "these", "those", "it", "they", "we", "can", "could", "will", "would",
            "which", "how", "may", "what", "where", "our", "their", "its", "has", "have", "had", "not", "no", "such", "also",
            "using", "used", "based", "through", "between", "both", "all", "any", "each", "into", "about", "more", "most", "than"
    ));

    public VisualizacionService(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public VisualizacionResponse obtenerDatosVisualizacion() {
        List<Articulo> articulos = storageManager.listarTodos();

        List<MapaData> mapaCalor = generarMapaCalor(articulos);
        List<PalabraFrecuencia> nubePalabras = generarNubePalabras(articulos);
        List<LineaTemporalData> lineaTemporal = generarLineaTemporal(articulos);

        return new VisualizacionResponse(mapaCalor, nubePalabras, lineaTemporal);
    }

    private List<MapaData> generarMapaCalor(List<Articulo> articulos) {
        Map<String, Integer> countsPorPais = new HashMap<>();

        for (Articulo art : articulos) {
            String primerAutor = "";
            if (art.getAutores() != null && !art.getAutores().isEmpty()) {
                primerAutor = art.getAutores().get(0);
            } else if (art.getTitulo() != null) {
                primerAutor = art.getTitulo(); // fallback
            }

            // Asignación determinista basada en el hash del primer autor
            int hash = Math.abs(primerAutor.hashCode());
            int index = hash % PAISES.length;
            String paisCode = PAISES[index];

            countsPorPais.put(paisCode, countsPorPais.getOrDefault(paisCode, 0) + 1);
        }

        List<MapaData> resultado = new ArrayList<>();
        for (int i = 0; i < PAISES.length; i++) {
            String code = PAISES[i];
            if (countsPorPais.containsKey(code)) {
                resultado.add(new MapaData(code, NOMBRES_PAISES[i], countsPorPais.get(code)));
            }
        }
        return resultado;
    }

    private List<PalabraFrecuencia> generarNubePalabras(List<Articulo> articulos) {
        Map<String, Integer> freqMap = new HashMap<>();

        for (Articulo art : articulos) {
            // Unir abstract y palabras clave
            StringBuilder texto = new StringBuilder();
            if (art.getResumen() != null) texto.append(art.getResumen()).append(" ");
            if (art.getPalabrasClave() != null) {
                for (String pk : art.getPalabrasClave()) {
                    texto.append(pk).append(" ");
                }
            }

            String cleanedText = texto.toString().toLowerCase().replaceAll("[^a-z\\s-]", " ");
            String[] tokens = cleanedText.split("\\s+");

            for (String token : tokens) {
                token = token.trim();
                if (token.length() > 3 && !STOP_WORDS.contains(token)) {
                    freqMap.put(token, freqMap.getOrDefault(token, 0) + 1);
                }
            }
        }

        // Devolver top 50
        return freqMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(50)
                .map(e -> new PalabraFrecuencia(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<LineaTemporalData> generarLineaTemporal(List<Articulo> articulos) {
        // Estructura: Año -> (Revista -> Cantidad)
        Map<Integer, Map<String, Integer>> agrupacion = new TreeMap<>(); // TreeMap para ordenar por año

        for (Articulo art : articulos) {
            Integer anio = art.getAnio();
            if (anio == null || anio == 0) {
                anio = 2023; // fallback año por defecto si no viene
            }
            
            String revista = art.getRevista();
            if (revista == null || revista.trim().isEmpty()) {
                revista = "Revista Desconocida (" + art.getOrigen() + ")";
            }

            agrupacion.putIfAbsent(anio, new HashMap<>());
            Map<String, Integer> revistasEnAnio = agrupacion.get(anio);
            revistasEnAnio.put(revista, revistasEnAnio.getOrDefault(revista, 0) + 1);
        }

        List<LineaTemporalData> resultado = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, Integer>> entry : agrupacion.entrySet()) {
            resultado.add(new LineaTemporalData(entry.getKey().toString(), entry.getValue()));
        }

        return resultado;
    }
}

package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServicioApiSemanticScholar {

    private final WebClient webClient;

    public ServicioApiSemanticScholar(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<Articulo> descargarArticulos(String busqueda, int limite) {
        List<Articulo> articulos = new ArrayList<>();
        try {
            String query = busqueda.replace(" ", "+");
            String url = String.format("https://api.semanticscholar.org/graph/v1/paper/search?query=%s&limit=%d&fields=title,authors,abstract", query, limite);

            JsonNode response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("data")) {
                JsonNode dataNode = response.get("data");
                if (dataNode.isArray()) {
                    for (JsonNode paperNode : dataNode) {
                        String titulo = paperNode.has("title") && !paperNode.get("title").isNull() ? paperNode.get("title").asText() : "";
                        String resumen = paperNode.has("abstract") && !paperNode.get("abstract").isNull() ? paperNode.get("abstract").asText() : "";

                        List<String> autores = new ArrayList<>();
                        if (paperNode.has("authors") && paperNode.get("authors").isArray()) {
                            for (JsonNode autorNode : paperNode.get("authors")) {
                                autores.add(autorNode.has("name") ? autorNode.get("name").asText() : "");
                            }
                        }

                        List<String> palabrasClave = new ArrayList<>();

                        Articulo articulo = new Articulo(titulo, autores, resumen, palabrasClave, "Semantic Scholar");
                        articulos.add(articulo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error descargando datos de Semantic Scholar: " + e.getMessage());
        }
        return articulos;
    }
}

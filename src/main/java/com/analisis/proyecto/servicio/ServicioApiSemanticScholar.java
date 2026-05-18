package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        
        int maxReintentos = 3;
        
        for (int intento = 1; intento <= maxReintentos; intento++) {
            try {
                String queryEncoded = URLEncoder.encode(busqueda, StandardCharsets.UTF_8);
                String url = "https://api.semanticscholar.org/graph/v1/paper/search?query=" + queryEncoded 
                           + "&limit=" + limite 
                           + "&fields=title,authors,abstract,year,venue,url,publicationDate";
                
                System.out.println("Solicitando a Semantic Scholar (intento " + intento + "): " + url);

                JsonNode response = webClient.get()
                        .uri(URI.create(url))
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
                            
                            int anio = paperNode.has("year") && !paperNode.get("year").isNull() ? paperNode.get("year").asInt() : 2023;
                            String revista = paperNode.has("venue") && !paperNode.get("venue").isNull() ? paperNode.get("venue").asText() : "";
                            String publicationDate = paperNode.has("publicationDate") && !paperNode.get("publicationDate").isNull() ? paperNode.get("publicationDate").asText() : "";
                            
                            if (revista.isEmpty()) {
                                revista = "Semantic Scholar Journal" + (!publicationDate.isEmpty() ? " (" + publicationDate + ")" : "");
                            } else {
                                if (!publicationDate.isEmpty() && !revista.contains(publicationDate)) {
                                    revista = revista + " (" + publicationDate + ")";
                                }
                            }
                            String urlArticulo = paperNode.has("url") && !paperNode.get("url").isNull() ? paperNode.get("url").asText() : "";

                            Articulo articulo = new Articulo(titulo, autores, resumen, anio, revista, urlArticulo);
                            articulo.setOrigen("Semantic Scholar");
                            articulos.add(articulo);
                        }
                    }
                }
                
                // Si llegamos aquí sin excepción, salimos del loop de reintentos
                break;
                
            } catch (Exception e) {
                String mensaje = e.getMessage() != null ? e.getMessage() : "";
                System.err.println("Error descargando datos de Semantic Scholar (intento " + intento + "): " + mensaje);
                
                if (mensaje.contains("429") && intento < maxReintentos) {
                    System.out.println("Rate limited. Esperando " + (intento * 5) + " segundos antes de reintentar...");
                    try {
                        Thread.sleep(intento * 5000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else if (intento == maxReintentos) {
                    e.printStackTrace();
                }
            }
        }
        
        return articulos;
    }
}

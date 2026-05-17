package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServicioApiArxiv {

    private final WebClient webClient;

    public ServicioApiArxiv(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<Articulo> descargarArticulos(String busqueda, int limite) {
        List<Articulo> articulos = new ArrayList<>();
        try {
            String queryEncoded = URLEncoder.encode("\"" + busqueda + "\"", StandardCharsets.UTF_8);
            String url = "https://export.arxiv.org/api/query?search_query=all:" + queryEncoded + "&start=0&max_results=" + limite;
            
            System.out.println("Consultando arXiv...");

            String xmlResponse = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                    


            if (xmlResponse != null) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));

                NodeList entryNodes = document.getElementsByTagName("entry");

                
                for (int i = 0; i < entryNodes.getLength(); i++) {
                    Node node = entryNodes.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;

                        String titulo = getElementValue(element, "title");
                        String resumen = getElementValue(element, "summary");
                        
                        List<String> autores = new ArrayList<>();
                        NodeList authorNodes = element.getElementsByTagName("author");
                        for (int j = 0; j < authorNodes.getLength(); j++) {
                            Element authorElement = (Element) authorNodes.item(j);
                            autores.add(getElementValue(authorElement, "name"));
                        }

                        String published = getElementValue(element, "published");
                        int anio = 2023;
                        if (!published.isEmpty() && published.length() >= 4) {
                            try {
                                anio = Integer.parseInt(published.substring(0, 4));
                            } catch (NumberFormatException e) {
                                // ignorar
                            }
                        }

                        String revista = getElementValue(element, "arxiv:journal_ref");
                        if (revista.isEmpty()) {
                            revista = "arXiv Preprint";
                        }
                        
                        String urlArticulo = getElementValue(element, "id");

                        // arXiv no devuelve siempre keywords de manera fácil, usamos lista vacía
                        List<String> palabrasClave = new ArrayList<>();

                        Articulo articulo = new Articulo(titulo, autores, resumen, anio, revista, urlArticulo);
                        articulo.setOrigen("arXiv");
                        articulos.add(articulo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error descargando datos de arXiv: " + e.getMessage());
        }
        return articulos;
    }

    private String getElementValue(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().replace("\n", " ").trim();
        }
        return "";
    }
}

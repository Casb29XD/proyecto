package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio encargado de la unificación de archivos bibliométricos y detección de duplicados.
 */
@Service
public class UnificacionService {

    public record ResultadoUnificacion(
            List<Articulo> unificados,
            List<Articulo> eliminados
    ) {}

    /**
     * Procesa una lista de archivos y genera una base de datos unificada sin duplicados.
     */
    public ResultadoUnificacion unificarArchivos(List<MultipartFile> archivos) throws IOException {
        List<Articulo> todosLosArticulos = new ArrayList<>();

        for (MultipartFile archivo : archivos) {
            String nombre = archivo.getOriginalFilename();
            if (nombre == null) continue;

            if (nombre.endsWith(".csv")) {
                todosLosArticulos.addAll(parsearCSV(archivo));
            } else if (nombre.endsWith(".bib")) {
                todosLosArticulos.addAll(parsearBibTeX(archivo));
            }
        }

        return deduplicar(todosLosArticulos);
    }

    private ResultadoUnificacion deduplicar(List<Articulo> articulos) {
        List<Articulo> unificados = new ArrayList<>();
        List<Articulo> eliminados = new ArrayList<>();

        for (Articulo actual : articulos) {
            boolean esDuplicado = unificados.stream()
                    .anyMatch(existente -> existente.equals(actual));

            if (esDuplicado) {
                eliminados.add(actual);
            } else {
                unificados.add(actual);
            }
        }

        return new ResultadoUnificacion(unificados, eliminados);
    }

    private List<Articulo> parsearCSV(MultipartFile archivo) throws IOException {
        List<Articulo> articulos = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(archivo.getInputStream()));
             CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            String[] linea;
            while ((linea = csvReader.readNext()) != null) {
                // Asumiendo un formato estándar: Titulo, Autores, Abstract, Año, Revista, DOI
                // Si la línea tiene menos campos, intentamos manejarlo con seguridad
                String titulo = getSafe(linea, 0);
                String autoresRaw = getSafe(linea, 1);
                String resumen = getSafe(linea, 2);
                String anioStr = getSafe(linea, 3);
                String revista = getSafe(linea, 4);
                String doi = getSafe(linea, 5);

                Articulo art = new Articulo();
                art.setTitulo(titulo);
                art.setAutores(Arrays.asList(autoresRaw.split(";")));
                art.setResumen(resumen);
                try {
                    art.setAnio(Integer.parseInt(anioStr.replaceAll("\\D", "")));
                } catch (Exception e) {
                    art.setAnio(0);
                }
                art.setRevista(revista);
                art.setDoi(doi);
                art.setOrigen(archivo.getOriginalFilename());

                articulos.add(art);
            }
        } catch (CsvValidationException e) {
            throw new IOException("Error validando CSV: " + e.getMessage());
        }
        return articulos;
    }

    private List<Articulo> parsearBibTeX(MultipartFile archivo) throws IOException {
        List<Articulo> articulos = new ArrayList<>();
        String contenido = new String(archivo.getInputStream().readAllBytes());
        
        // Matcher simple para entradas @article{...} o @inproceedings{...}
        Pattern pattern = Pattern.compile("@\\w+\\{[^,]+,\\s*([\\s\\S]*?)\\n\\}");
        Matcher matcher = pattern.matcher(contenido);

        while (matcher.find()) {
            String entryBody = matcher.group(1);
            Articulo art = new Articulo();
            art.setTitulo(extractBibField(entryBody, "title"));
            art.setAutores(Arrays.asList(extractBibField(entryBody, "author").split(" and ")));
            art.setResumen(extractBibField(entryBody, "abstract"));
            String year = extractBibField(entryBody, "year");
            try {
                art.setAnio(Integer.parseInt(year.replaceAll("\\D", "")));
            } catch (Exception e) {
                art.setAnio(0);
            }
            art.setRevista(extractBibField(entryBody, "journal"));
            if (art.getRevista().isEmpty()) {
                art.setRevista(extractBibField(entryBody, "booktitle"));
            }
            art.setDoi(extractBibField(entryBody, "doi"));
            art.setOrigen(archivo.getOriginalFilename());
            
            articulos.add(art);
        }
        
        return articulos;
    }

    private String extractBibField(String body, String field) {
        Pattern p = Pattern.compile(field + "\\s*=\\s*\\{?([^\\},]+)\\}?", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(body);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }

    private String getSafe(String[] line, int index) {
        return (line != null && index < line.length) ? line[index] : "";
    }
}

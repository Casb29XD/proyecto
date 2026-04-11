package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import com.analisis.proyecto.modelo.ArticuloDuplicado;
import com.analisis.proyecto.repositorio.RepositorioArticulo;
import com.analisis.proyecto.repositorio.RepositorioArticuloDuplicado;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

@Service
public class ServicioGestorReferencias {

    private final ServicioApiArxiv servicioApiArxiv;
    private final ServicioApiSemanticScholar servicioApiSemanticScholar;
    private final RepositorioArticulo repositorioArticulo;
    private final RepositorioArticuloDuplicado repositorioArticuloDuplicado;
    private final ObjectMapper objectMapper;

    public ServicioGestorReferencias(ServicioApiArxiv servicioApiArxiv,
                                     ServicioApiSemanticScholar servicioApiSemanticScholar,
                                     RepositorioArticulo repositorioArticulo,
                                     RepositorioArticuloDuplicado repositorioArticuloDuplicado) {
        this.servicioApiArxiv = servicioApiArxiv;
        this.servicioApiSemanticScholar = servicioApiSemanticScholar;
        this.repositorioArticulo = repositorioArticulo;
        this.repositorioArticuloDuplicado = repositorioArticuloDuplicado;
        this.objectMapper = new ObjectMapper();
    }

    private List<Articulo> cacheUnicosLocal = new ArrayList<>();

    public String ejecutarProcesoDescarga(String consulta, int limite) {
        System.out.println("Iniciando descarga de artículos...");
        
        List<Articulo> articulosArxiv = servicioApiArxiv.descargarArticulos(consulta, limite);
        System.out.println("Descargados " + articulosArxiv.size() + " de arXiv.");

        List<Articulo> articulosSemantic = servicioApiSemanticScholar.descargarArticulos(consulta, limite);
        System.out.println("Descargados " + articulosSemantic.size() + " de Semantic Scholar.");

        List<Articulo> todosLosArticulos = new ArrayList<>();
        todosLosArticulos.addAll(articulosArxiv);
        todosLosArticulos.addAll(articulosSemantic);

        // Listas locales para deduplicación en memoria (por si MongoDB falla)
        List<Articulo> unicosLocal = new ArrayList<>(cacheUnicosLocal);
        List<ArticuloDuplicado> duplicadosLocal = new ArrayList<>();

        int procesados = 0;
        int unicosGuardados = 0;
        int duplicadosEncontrados = 0;
        boolean mongoDisponible = true;

        // Intentar verificar si hay conexión a MongoDB
        try {
            repositorioArticulo.count();
        } catch (Exception e) {
            mongoDisponible = false;
            System.err.println("MongoDB no disponible, usando deduplicación en memoria: " + e.getMessage());
        }

        for (Articulo articulo : todosLosArticulos) {
            if (articulo.getTitulo() == null || articulo.getTitulo().trim().isEmpty()) {
                continue; // Ignorar nulos
            }

            if (mongoDisponible) {
                try {
                    Optional<Articulo> existente = repositorioArticulo.findByTituloIgnoreCase(articulo.getTitulo().trim());
                    if (existente.isPresent()) {
                        ArticuloDuplicado duplicado = new ArticuloDuplicado(articulo, "Ya existía un artículo con este título proveniente de: " + existente.get().getOrigen());
                        repositorioArticuloDuplicado.save(duplicado);
                        duplicadosEncontrados++;
                    } else {
                        repositorioArticulo.save(articulo);
                        unicosGuardados++;
                    }
                } catch (Exception e) {
                    mongoDisponible = false;
                    System.err.println("Error al guardar en MongoDB, cambiando a modo en memoria: " + e.getMessage());
                    // Re-procesar este artículo en memoria
                    unicosLocal.add(articulo);
                    unicosGuardados++;
                }
            } else {
                // Deduplicación en memoria
                boolean esDuplicado = unicosLocal.stream()
                        .anyMatch(a -> a.getTitulo().equalsIgnoreCase(articulo.getTitulo().trim()));
                if (esDuplicado) {
                    ArticuloDuplicado duplicado = new ArticuloDuplicado(articulo, "Duplicado detectado en memoria");
                    duplicadosLocal.add(duplicado);
                    duplicadosEncontrados++;
                } else {
                    unicosLocal.add(articulo);
                    unicosGuardados++;
                }
            }
            procesados++;
        }

        // Actualizar cache local
        this.cacheUnicosLocal = unicosLocal;

        // Generar archivos JSON
        try {
            List<Articulo> todosUnicos;
            List<ArticuloDuplicado> todosDuplicados;
            
            if (mongoDisponible) {
                todosUnicos = repositorioArticulo.findAll();
                todosDuplicados = repositorioArticuloDuplicado.findAll();
            } else {
                todosUnicos = unicosLocal;
                todosDuplicados = duplicadosLocal;
            }
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("articulos_unificados.json"), todosUnicos);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("articulos_duplicados.json"), todosDuplicados);
            System.out.println("Archivos generados exitosamente. Únicos: " + todosUnicos.size() + ", Duplicados: " + todosDuplicados.size());
        } catch (Exception e) {
            System.err.println("Error al generar los archivos JSON: " + e.getMessage());
            e.printStackTrace();
        }

        return String.format("Se procesaron %d artículos en total. Se guardaron %d artículos únicos nuevos y se encontraron %d duplicados. Archivos articulos_unificados.json y articulos_duplicados.json generados con todo el registro histórico.", procesados, unicosGuardados, duplicadosEncontrados);
    }

    public List<Articulo> obtenerArticulosUnicos() {
        try {
            return repositorioArticulo.findAll();
        } catch (Exception e) {
            System.err.println("MongoDB no disponible al recuperar, devolviendo cache local.");
            return cacheUnicosLocal;
        }
    }
}

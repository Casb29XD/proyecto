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

@Service
public class ServicioGestorReferencias {

    private final ServicioApiArxiv servicioApiArxiv;
    private final ServicioApiSemanticScholar servicioApiSemanticScholar;
    private final RepositorioArticulo repositorioArticulo;
    private final RepositorioArticuloDuplicado repositorioArticuloDuplicado;

    public ServicioGestorReferencias(ServicioApiArxiv servicioApiArxiv,
                                     ServicioApiSemanticScholar servicioApiSemanticScholar,
                                     RepositorioArticulo repositorioArticulo,
                                     RepositorioArticuloDuplicado repositorioArticuloDuplicado) {
        this.servicioApiArxiv = servicioApiArxiv;
        this.servicioApiSemanticScholar = servicioApiSemanticScholar;
        this.repositorioArticulo = repositorioArticulo;
        this.repositorioArticuloDuplicado = repositorioArticuloDuplicado;
    }

    public String ejecutarProcesoDescarga(String consulta, int limite) {
        System.out.println("Iniciando descarga de artículos...");
        
        List<Articulo> articulosArxiv = servicioApiArxiv.descargarArticulos(consulta, limite);
        System.out.println("Descargados " + articulosArxiv.size() + " de arXiv.");

        List<Articulo> articulosSemantic = servicioApiSemanticScholar.descargarArticulos(consulta, limite);
        System.out.println("Descargados " + articulosSemantic.size() + " de Semantic Scholar.");

        List<Articulo> todosLosArticulos = new ArrayList<>();
        todosLosArticulos.addAll(articulosArxiv);
        todosLosArticulos.addAll(articulosSemantic);

        int procesados = 0;
        int unicosGuardados = 0;
        int duplicadosEncontrados = 0;

        for (Articulo articulo : todosLosArticulos) {
            if (articulo.getTitulo() == null || articulo.getTitulo().trim().isEmpty()) {
                continue; // Ignorar nulos
            }
            
            // Buscar si ya existe por nombre
            Optional<Articulo> existente = repositorioArticulo.findByTituloIgnoreCase(articulo.getTitulo().trim());
            if (existente.isPresent()) {
                // Es un duplicado
                ArticuloDuplicado duplicado = new ArticuloDuplicado(articulo, "Ya existía un artículo con este título proveniente de: " + existente.get().getOrigen());
                repositorioArticuloDuplicado.save(duplicado);
                duplicadosEncontrados++;
            } else {
                // Es nuevo
                repositorioArticulo.save(articulo);
                unicosGuardados++;
            }
            procesados++;
        }

        return String.format("Se procesaron %d artículos en total. Se guardaron %d artículos únicos y se encontraron %d duplicados.", procesados, unicosGuardados, duplicadosEncontrados);
    }
}

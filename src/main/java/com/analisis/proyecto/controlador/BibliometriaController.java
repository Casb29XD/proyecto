package com.analisis.proyecto.controlador;

import com.analisis.proyecto.modelo.Articulo;
import com.analisis.proyecto.repositorio.RepositorioArticulo;
import com.analisis.proyecto.servicio.AnalisisSimilitudService;
import com.analisis.proyecto.servicio.UnificacionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controlador principal para la gestión de bibliometría.
 */
@RestController
@RequestMapping("/api/bibliometria")
@CrossOrigin(origins = "*") // Para desarrollo
public class BibliometriaController {

    private final UnificacionService unificacionService;
    private final AnalisisSimilitudService analisisSimilitudService;
    private final RepositorioArticulo repositorioArticulo;

    public BibliometriaController(UnificacionService unificacionService,
                                  AnalisisSimilitudService analisisSimilitudService,
                                  RepositorioArticulo repositorioArticulo) {
        this.unificacionService = unificacionService;
        this.analisisSimilitudService = analisisSimilitudService;
        this.repositorioArticulo = repositorioArticulo;
    }

    /**
     * Requerimiento 1: Carga y unificación de archivos CSV/BibTeX.
     */
    @PostMapping("/cargar")
    public ResponseEntity<UnificacionService.ResultadoUnificacion> cargarArchivos(
            @RequestParam("archivos") List<MultipartFile> archivos) throws IOException {
        
        UnificacionService.ResultadoUnificacion resultado = unificacionService.unificarArchivos(archivos);
        
        // Guardamos los artículos unificados en la base de datos
        repositorioArticulo.saveAll(resultado.unificados());
        
        return ResponseEntity.ok(resultado);
    }

    /**
     * Requerimiento 2: Analizar similitud de un artículo contra el resto de la base de datos.
     */
    @PostMapping("/analizar-similitud")
    public ResponseEntity<List<AnalisisSimilitudService.ResultadoComparacion>> analizarSimilitud(
            @RequestBody Articulo articuloBase) {
        
        List<Articulo> baseDeDatos = repositorioArticulo.findAll();
        List<AnalisisSimilitudService.ResultadoComparacion> resultados = 
                analisisSimilitudService.compararAbstractContraBase(articuloBase, baseDeDatos);
        
        return ResponseEntity.ok(resultados);
    }

    /**
     * Endpoint auxiliar para obtener artículos con paginación.
     */
    @GetMapping("/articulos")
    public Page<Articulo> listarArticulos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repositorioArticulo.findAll(pageable);
    }

    /**
     * Endpoint auxiliar para obtener detalles de los algoritmos implementados.
     */
    @GetMapping("/algoritmos")
    public List<Map<String, String>> obtenerAlgoritmos() {
        return analisisSimilitudService.getDetallesAlgoritmos();
    }
}

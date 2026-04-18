package com.analisis.proyecto.controlador;

import com.analisis.proyecto.modelo.Articulo;
import com.analisis.proyecto.modelo.ArticuloDuplicado;
import com.analisis.proyecto.modelo.Favorito;
import com.analisis.proyecto.modelo.HistorialBusqueda;
import com.analisis.proyecto.servicio.AnalisisSimilitudService;
import com.analisis.proyecto.servicio.StorageManager;
import com.analisis.proyecto.servicio.UnificacionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador principal para la gestión de bibliometría con soporte para Fallback.
 */
@RestController
@RequestMapping("/api/bibliometria")
public class BibliometriaController {

    private static final Logger logger = LoggerFactory.getLogger(BibliometriaController.class);
    
    private final UnificacionService unificacionService;
    private final AnalisisSimilitudService analisisSimilitudService;
    private final StorageManager storageManager;

    public BibliometriaController(UnificacionService unificacionService,
                                  AnalisisSimilitudService analisisSimilitudService,
                                  StorageManager storageManager) {
        this.unificacionService = unificacionService;
        this.analisisSimilitudService = analisisSimilitudService;
        this.storageManager = storageManager;
    }

    /**
     * Requerimiento 1: Carga y unificación de archivos.
     */
    @PostMapping("/cargar")
    public ResponseEntity<UnificacionService.ResultadoUnificacion> cargarArchivos(
            @RequestParam("archivos") List<MultipartFile> archivos) throws IOException {
        
        UnificacionService.ResultadoUnificacion resultado = unificacionService.unificarArchivos(archivos);
        
        // Guardamos los artículos únicos
        storageManager.guardar(resultado.unificados());
        
        // Guardamos los que fueron eliminados como duplicados en la colección correspondiente
        List<ArticuloDuplicado> duplicadosMapeados = resultado.eliminados().stream()
                .map(art -> new ArticuloDuplicado(art, "Duplicado por título/DOI"))
                .toList();
        storageManager.guardarDuplicados(duplicadosMapeados);
        
        return ResponseEntity.ok(resultado);
    }

    /**
     * Requerimiento 2: Analizar similitud.
     */
    @PostMapping("/analizar-similitud")
    public ResponseEntity<List<AnalisisSimilitudService.ResultadoComparacion>> analizarSimilitud(
            @RequestBody Articulo articuloBase) {
        
        // Obtenemos todos los artículos del almacenamiento actual
        List<Articulo> baseDeDatos = storageManager.listarTodos();
        
        List<AnalisisSimilitudService.ResultadoComparacion> resultados = 
                analisisSimilitudService.compararAbstractContraBase(articuloBase, baseDeDatos);
        
        // Registramos la búsqueda en el historial
        storageManager.registrarBusqueda(articuloBase.getTitulo(), resultados.size());
        
        return ResponseEntity.ok(resultados);
    }

    /**
     * Endpoint para obtener artículos con paginación (Soporta Fallback).
     */
    @GetMapping("/articulos")
    public Page<Articulo> listarArticulos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("Petición recibida: listarArticulos(page={}, size={})", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return storageManager.listarPaginados(pageable);
    }

    /**
     * Endpoint para obtener estado del sistema y algoritmos.
     */
    @GetMapping("/estado")
    public Map<String, Object> obtenerEstado() {
        Map<String, Object> estado = new HashMap<>();
        estado.put("modoAlmacenamiento", storageManager.getMode());
        estado.put("algoritmos", analisisSimilitudService.getDetallesAlgoritmos());
        return estado;
    }

    @GetMapping("/algoritmos")
    public List<Map<String, String>> obtenerAlgoritmos() {
        return analisisSimilitudService.getDetallesAlgoritmos();
    }

    // --- Endpoints para Favoritos ---

    @PostMapping("/favoritos")
    public ResponseEntity<Void> agregarFavorito(@RequestBody Articulo articulo) {
        storageManager.agregarFavorito(articulo);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/favoritos/{articuloId}")
    public ResponseEntity<Void> quitarFavorito(@PathVariable String articuloId) {
        storageManager.quitarFavorito(articuloId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/favoritos")
    public List<Favorito> listarFavoritos() {
        return storageManager.listarFavoritos();
    }
}

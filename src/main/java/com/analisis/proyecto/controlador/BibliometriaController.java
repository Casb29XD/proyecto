package com.analisis.proyecto.controlador;

import com.analisis.proyecto.modelo.Articulo;
import com.analisis.proyecto.modelo.ArticuloDuplicado;
import com.analisis.proyecto.modelo.Favorito;
import com.analisis.proyecto.modelo.HistorialBusqueda;
import com.analisis.proyecto.servicio.AnalisisSimilitudService;
import com.analisis.proyecto.servicio.ArticuloSearchService;
import com.analisis.proyecto.servicio.MineriaTextoService;
import com.analisis.proyecto.servicio.ServicioApiArxiv;
import com.analisis.proyecto.servicio.ServicioApiSemanticScholar;
import com.analisis.proyecto.servicio.StorageManager;
import com.analisis.proyecto.servicio.UnificacionService;
import com.analisis.proyecto.servicio.AgrupamientoJerarquicoService;
import com.analisis.proyecto.servicio.VisualizacionService;
import com.analisis.proyecto.servicio.impl.MongoStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final MongoStorageService mongoStorage;
    private final ArticuloSearchService articuloSearchService;
    private final MineriaTextoService mineriaTextoService;
    private final ServicioApiArxiv apiArxiv;
    private final ServicioApiSemanticScholar apiSemantic;
    private final AgrupamientoJerarquicoService agrupamientoService;
    private final VisualizacionService visualizacionService;

    public BibliometriaController(UnificacionService unificacionService,
                                  AnalisisSimilitudService analisisSimilitudService,
                                  StorageManager storageManager,
                                  MongoStorageService mongoStorage,
                                  ArticuloSearchService articuloSearchService,
                                  MineriaTextoService mineriaTextoService,
                                  ServicioApiArxiv apiArxiv,
                                  ServicioApiSemanticScholar apiSemantic,
                                  AgrupamientoJerarquicoService agrupamientoService,
                                  VisualizacionService visualizacionService) {
        this.unificacionService = unificacionService;
        this.analisisSimilitudService = analisisSimilitudService;
        this.storageManager = storageManager;
        this.mongoStorage = mongoStorage;
        this.articuloSearchService = articuloSearchService;
        this.mineriaTextoService = mineriaTextoService;
        this.apiArxiv = apiArxiv;
        this.apiSemantic = apiSemantic;
        this.agrupamientoService = agrupamientoService;
        this.visualizacionService = visualizacionService;
    }

    /**
     * Requerimiento 1 Extra: Automatización de extracción mediante APIs de terceros.
     */
    @PostMapping("/automatizar")
    public ResponseEntity<UnificacionService.ResultadoUnificacion> automatizarDescarga(
            @RequestParam(defaultValue = "generative artificial intelligence") String query) {
        
        logger.info("Iniciando extracción automática para: {}", query);
        List<Articulo> arxivDocs = apiArxiv.descargarArticulos(query, 50);
        List<Articulo> semanticDocs = apiSemantic.descargarArticulos(query, 50);
        
        List<Articulo> baseDeDatos = storageManager.listarTodos();
        
        // Unificar, deduplicar contra los existentes
        UnificacionService.ResultadoUnificacion resultado = unificacionService.unificarListas(baseDeDatos, arxivDocs, semanticDocs);
        
        // Extraer los nuevos que no estaban en la base de datos
        List<Articulo> nuevosUnificados = resultado.unificados().stream()
                .filter(art -> !baseDeDatos.contains(art))
                .toList();

        // Guardar persistente solo los nuevos
        storageManager.guardar(nuevosUnificados);
        List<ArticuloDuplicado> duplicadosMapeados = resultado.eliminados().stream()
                .map(art -> new ArticuloDuplicado(art, "Duplicado tras automatización API"))
                .toList();
        storageManager.guardarDuplicados(duplicadosMapeados);
        
        return ResponseEntity.ok(new UnificacionService.ResultadoUnificacion(nuevosUnificados, resultado.eliminados()));
    }

    /**
     * Requerimiento 1: Carga y unificación de archivos.
     */
    @PostMapping("/cargar")
    public ResponseEntity<UnificacionService.ResultadoUnificacion> cargarArchivos(
            @RequestParam("archivos") List<MultipartFile> archivos) throws IOException {
        
        List<Articulo> baseDeDatos = storageManager.listarTodos();
        UnificacionService.ResultadoUnificacion resultadoTemporal = unificacionService.unificarArchivos(archivos);
        UnificacionService.ResultadoUnificacion resultado = unificacionService.unificarListas(baseDeDatos, resultadoTemporal.unificados());
        
        // Extraer los nuevos que no estaban en la base de datos
        List<Articulo> nuevosUnificados = resultado.unificados().stream()
                .filter(art -> !baseDeDatos.contains(art))
                .toList();

        // Guardamos los artículos únicos nuevos
        storageManager.guardar(nuevosUnificados);
        
        // Unimos los eliminados del archivo internamente + los eliminados contra la BD
        List<Articulo> todosEliminados = new java.util.ArrayList<>(resultadoTemporal.eliminados());
        todosEliminados.addAll(resultado.eliminados());

        // Guardamos los que fueron eliminados como duplicados en la colección correspondiente
        List<ArticuloDuplicado> duplicadosMapeados = todosEliminados.stream()
                .map(art -> new ArticuloDuplicado(art, "Duplicado por título/DOI"))
                .toList();
        storageManager.guardarDuplicados(duplicadosMapeados);
        
        return ResponseEntity.ok(new UnificacionService.ResultadoUnificacion(nuevosUnificados, todosEliminados));
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
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("Petición recibida: listarArticulos(query={}, page={}, size={})", query, page, size);
        Pageable pageable = PageRequest.of(page, size);
        
        if (query != null && !query.trim().isEmpty()) {
            List<Articulo> todos = storageManager.listarTodos();
            List<Articulo> filtrados = articuloSearchService.buscarArticulos(todos, query);
            
            // Convertimos la lista filtrada nuevamente a una sub-lista paginada para mantener el formato original
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filtrados.size());
            List<Articulo> subList = start > filtrados.size() ? List.of() : filtrados.subList(start, end);
            
            return new PageImpl<>(subList, pageable, filtrados.size());
        }
        
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

    // --- Exportación Requerimiento 1 ---
    
    @GetMapping(value = "/exportar/unificados", produces = "text/csv; charset=utf-8")
    public ResponseEntity<String> exportarUnificados() {
        List<Articulo> todos = storageManager.listarTodos();
        StringBuilder csv = new StringBuilder("Titulo;Autores;Origen;Abstract;Año;Revista;DOI\n");
        for(Articulo a : todos) {
            String aut = a.getAutores() != null ? String.join(", ", a.getAutores()).replace("\"", "\"\"") : "";
            String tit = a.getTitulo() != null ? a.getTitulo().replace("\"", "\"\"") : "";
            String res = a.getResumen() != null ? a.getResumen().replace("\"", "\"\"") : "";
            String ori = a.getOrigen() != null ? a.getOrigen() : "";
            String rev = a.getRevista() != null ? a.getRevista() : "";
            String doi = a.getDoi() != null ? a.getDoi() : "";
            
            csv.append(String.format("\"%s\";\"%s\";\"%s\";\"%s\";%d;\"%s\";\"%s\"\n",
                tit, aut, ori, res, a.getAnio(), rev, doi));
        }
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"articulos_unificados.csv\"")
            .body(csv.toString());
    }

    @GetMapping(value = "/exportar/eliminados", produces = "text/csv; charset=utf-8")
    public ResponseEntity<String> exportarEliminados() {
        List<com.analisis.proyecto.modelo.ArticuloDuplicado> eliminados = storageManager.obtenerDuplicados();
            
        StringBuilder csv = new StringBuilder("Titulo;Origen;Motivo\n");
        for(com.analisis.proyecto.modelo.ArticuloDuplicado a : eliminados) {
            String tit = a.getTitulo() != null ? a.getTitulo().replace("\"", "\"\"") : "";
            String ori = a.getOrigen() != null ? a.getOrigen() : "";
            String mot = a.getMotivo() != null ? a.getMotivo() : "Duplicado";
            
            csv.append(String.format("\"%s\";\"%s\";\"%s\"\n", tit, ori, mot));
        }
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"articulos_repetidos_eliminados.csv\"")
            .body(csv.toString());
    }

    @GetMapping("/duplicados")
    public List<com.analisis.proyecto.modelo.ArticuloDuplicado> obtenerDuplicados() {
        return storageManager.obtenerDuplicados();
    }

    // --- Minería de Textos (Requerimiento 3) ---
    @GetMapping("/mineria/frecuencias")
    public MineriaTextoService.ResultadoMineria extraerFrecuenciasTextuales() {
        // Ejecutamos minería sobre toda la base de datos unificada
        List<com.analisis.proyecto.modelo.Articulo> todos = storageManager.listarTodos();
        return mineriaTextoService.analizarFrecuencias(todos);
    }

    @GetMapping("/mineria/frecuencias/{articuloId}")
    public ResponseEntity<MineriaTextoService.ResultadoMineria> extraerFrecuenciasPorArticulo(@PathVariable String articuloId) {
        return storageManager.obtenerPorId(articuloId)
                .map(articulo -> ResponseEntity.ok(mineriaTextoService.analizarFrecuenciasDocumento(articulo)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public static class AgrupamientoRequest {
        private List<String> ids;
        private String linkage;
        private String metric;

        public List<String> getIds() { return ids; }
        public void setIds(List<String> ids) { this.ids = ids; }
        public String getLinkage() { return linkage; }
        public void setLinkage(String linkage) { this.linkage = linkage; }
        public String getMetric() { return metric; }
        public void setMetric(String metric) { this.metric = metric; }
    }

    // --- Clustering Jerárquico (Requerimiento 4) ---
    @PostMapping("/agrupamiento")
    public ResponseEntity<com.analisis.proyecto.modelo.ClusterNode> obtenerAgrupamiento(
            @RequestBody AgrupamientoRequest request) {
        
        List<Articulo> todos = storageManager.listarTodos();
        if (todos.isEmpty() || request.getIds() == null || request.getIds().isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        List<Articulo> sublist = todos.stream()
                .filter(a -> request.getIds().contains(a.getId()))
                .toList();
                
        if (sublist.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(agrupamientoService.agrupar(sublist, request.getLinkage(), request.getMetric()));
    }

    @PostMapping("/agrupamiento/comparar")
    public ResponseEntity<List<com.analisis.proyecto.servicio.AgrupamientoJerarquicoService.ComparacionMetodo>> compararMetodos(
            @RequestBody AgrupamientoRequest request) {
        
        List<Articulo> todos = storageManager.listarTodos();
        if (todos.isEmpty() || request.getIds() == null || request.getIds().isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        List<Articulo> sublist = todos.stream()
                .filter(a -> request.getIds().contains(a.getId()))
                .toList();
                
        if (sublist.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(agrupamientoService.compararMetodos(sublist, request.getMetric()));
    }

    // --- Visualización (Requerimiento 5) ---
    @GetMapping("/visualizacion")
    public ResponseEntity<com.analisis.proyecto.modelo.VisualizacionResponse> obtenerDatosVisualizacion() {
        return ResponseEntity.ok(visualizacionService.obtenerDatosVisualizacion());
    }
}

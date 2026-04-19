package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import com.analisis.proyecto.modelo.ArticuloDuplicado;
import com.analisis.proyecto.modelo.Favorito;
import com.analisis.proyecto.modelo.HistorialBusqueda;
import com.analisis.proyecto.servicio.impl.InMemoryStorageService;
import com.analisis.proyecto.servicio.impl.MongoStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.util.List;

/**
 * Gestor inteligente que alterna entre MongoDB y Memoria Local.
 */
@Service
public class StorageManager {

    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);
    
    private final MongoStorageService mongoStorage;
    private final InMemoryStorageService inMemoryStorage;
    private Boolean useFallback = null; // null significa que aún no se ha comprobado

    public StorageManager(MongoStorageService mongoStorage, InMemoryStorageService inMemoryStorage) {
        this.mongoStorage = mongoStorage;
        this.inMemoryStorage = inMemoryStorage;
    }

    private synchronized void checkConnectionLazily() {
        if (useFallback != null) return;

        logger.info("Verificando disponibilidad de MongoDB...");
        if (!mongoStorage.estaDisponible()) {
            logger.warn(">>> MONGODB NO DISPONIBLE. Activando modo Fallback (In-Memory). <<<");
            useFallback = true;
        } else {
            logger.info(">>> Conexión con MongoDB exitosa. Usando almacenamiento en la nube. <<<");
            useFallback = false;
        }
    }

    public void guardar(List<Articulo> articulos) {
        checkConnectionLazily();
        if (!useFallback) {
            try {
                mongoStorage.guardarTodos(articulos);
                return;
            } catch (Exception e) {
                logger.error("Error al guardar en MongoDB. Cambiando a Fallback.", e);
                useFallback = true;
            }
        }
        inMemoryStorage.guardarTodos(articulos);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void inicializarDatos() {
        checkConnectionLazily();
        List<Articulo> actuales = listarTodos();
        if (actuales == null || actuales.isEmpty()) {
            logger.info("La base de datos está vacía. Intentando cargar datos de articulos_unificados.json automáticamente...");
            try {
                File archivoJson = new File("articulos_unificados.json");
                if (archivoJson.exists()) {
                    ObjectMapper mapper = new ObjectMapper();
                    List<Articulo> articulos = mapper.readValue(archivoJson, new TypeReference<List<Articulo>>() {});
                    if (!articulos.isEmpty()) {
                        guardar(articulos);
                        logger.info("Se han cargado {} artículos iniciales exitosamente.", articulos.size());
                    }
                } else {
                    logger.warn("El archivo articulos_unificados.json no existe en el directorio principal.");
                }
            } catch (Exception e) {
                logger.error("No se pudo cargar articulos_unificados.json automáticamente.", e);
            }
        } else {
            logger.info("La base de datos ya contiene {} artículos. Omitiendo carga automática.", actuales.size());
        }
    }

    public List<Articulo> listarTodos() {
        checkConnectionLazily();
        if (!useFallback) {
            try {
                return mongoStorage.obtenerTodos();
            } catch (Exception e) {
                logger.warn("Fallo en lectura de MongoDB. Cambiando a Fallback.");
                useFallback = true;
            }
        }
        return inMemoryStorage.obtenerTodos();
    }

    public Page<Articulo> listarPaginados(Pageable pageable) {
        checkConnectionLazily();
        if (!useFallback) {
            try {
                return mongoStorage.obtenerPaginados(pageable);
            } catch (Exception e) {
                logger.warn("Fallo en lectura paginada de MongoDB. Cambiando a Fallback.");
                useFallback = true;
            }
        }
        return inMemoryStorage.obtenerPaginados(pageable);
    }

    public String getMode() {
        if (useFallback == null) return "Verificando...";
        return useFallback ? "Offline (In-Memory)" : "Online (MongoDB Atlas)";
    }

    public boolean isUseFallback() {
        checkConnectionLazily();
        return useFallback != null && useFallback;
    }

    // --- Nuevas funcionalidades ---

    public void guardarDuplicados(List<ArticuloDuplicado> duplicados) {
        checkConnectionLazily();
        if (!useFallback) {
            try {
                mongoStorage.guardarDuplicados(duplicados);
            } catch (Exception e) {
                logger.error("Error al guardar duplicados en MongoDB", e);
            }
        }
    }

    public void registrarBusqueda(String titulo, int resultados) {
        checkConnectionLazily();
        if (!useFallback) {
            try {
                mongoStorage.registrarBusqueda(new HistorialBusqueda(titulo, resultados));
            } catch (Exception e) {
                logger.error("Error al registrar búsqueda en MongoDB", e);
            }
        }
    }

    public void agregarFavorito(Articulo articulo) {
        checkConnectionLazily();
        if (!useFallback) {
            try {
                mongoStorage.guardarFavorito(new Favorito(articulo));
            } catch (Exception e) {
                logger.error("Error al agregar favorito en MongoDB", e);
            }
        }
    }

    public void quitarFavorito(String articuloId) {
        checkConnectionLazily();
        if (!useFallback) {
            try {
                mongoStorage.eliminarFavorito(articuloId);
            } catch (Exception e) {
                logger.error("Error al quitar favorito en MongoDB", e);
            }
        }
    }

    public List<Favorito> listarFavoritos() {
        checkConnectionLazily();
        if (!useFallback) {
            try {
                return mongoStorage.obtenerFavoritos();
            } catch (Exception e) {
                logger.error("Error al listar favoritos en MongoDB", e);
            }
        }
        return List.of();
    }
}

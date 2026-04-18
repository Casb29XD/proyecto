package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interfaz genérica para el almacenamiento de artículos.
 * Permite cambiar entre MongoDB y almacenamiento en memoria (Fallback).
 */
public interface StorageService {
    void guardarTodos(List<Articulo> articulos);
    List<Articulo> obtenerTodos();
    Page<Articulo> obtenerPaginados(Pageable pageable);
    boolean estaDisponible();
}

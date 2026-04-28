package com.analisis.proyecto.servicio.impl;

import com.analisis.proyecto.modelo.Articulo;
import com.analisis.proyecto.modelo.ArticuloDuplicado;
import com.analisis.proyecto.servicio.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación de almacenamiento en memoria (Fallback).
 * Se activa si MongoDB no está disponible.
 */
@Service
public class InMemoryStorageService implements StorageService {

    private static final List<Articulo> baseDeDatosMemoria = Collections.synchronizedList(new ArrayList<>());
    private static final List<ArticuloDuplicado> duplicadosMemoria = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void guardarTodos(List<Articulo> articulos) {
        for (Articulo nuevo : articulos) {
            // Evitar duplicados simples en memoria por DOI o Título
            boolean existe = baseDeDatosMemoria.stream()
                    .anyMatch(a -> (a.getDoi() != null && !a.getDoi().isEmpty() && a.getDoi().equalsIgnoreCase(nuevo.getDoi())) ||
                                   (a.getTitulo() != null && a.getTitulo().equalsIgnoreCase(nuevo.getTitulo())));
            if (!existe) {
                if (nuevo.getId() == null || nuevo.getId().isEmpty()) {
                    nuevo.setId(java.util.UUID.randomUUID().toString());
                }
                baseDeDatosMemoria.add(nuevo);
            }
        }
    }

    @Override
    public List<Articulo> obtenerTodos() {
        return new ArrayList<>(baseDeDatosMemoria);
    }

    @Override
    public Page<Articulo> obtenerPaginados(Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), baseDeDatosMemoria.size());
        
        List<Articulo> subList = (start < baseDeDatosMemoria.size()) 
                ? baseDeDatosMemoria.subList(start, end) 
                : new ArrayList<>();
                
        return new PageImpl<>(subList, pageable, baseDeDatosMemoria.size());
    }

    @Override
    public java.util.Optional<Articulo> obtenerPorId(String id) {
        return baseDeDatosMemoria.stream()
                .filter(a -> id.equals(a.getId()))
                .findFirst();
    }

    @Override
    public boolean estaDisponible() {
        return true; // La memoria siempre está disponible
    }

    public void guardarDuplicados(List<ArticuloDuplicado> duplicados) {
        duplicadosMemoria.addAll(duplicados);
    }

    public List<ArticuloDuplicado> obtenerDuplicados() {
        return new ArrayList<>(duplicadosMemoria);
    }
}

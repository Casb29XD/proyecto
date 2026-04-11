package com.analisis.proyecto.controlador;

import com.analisis.proyecto.modelo.Articulo;
import com.analisis.proyecto.servicio.ServicioSimilitud;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/similitud")
public class ControladorSimilitud {

    private final ServicioSimilitud servicioSimilitud;

    public ControladorSimilitud(ServicioSimilitud servicioSimilitud) {
        this.servicioSimilitud = servicioSimilitud;
    }

    @PostMapping("/analizar")
    public Map<String, List<ServicioSimilitud.ResultadoSimilitud>> analizar(@RequestBody List<Articulo> articulos) {
        if (articulos == null || articulos.size() < 2) {
            throw new IllegalArgumentException("Se requieren al menos 2 artículos para el análisis.");
        }
        return servicioSimilitud.analizarSimilitud(articulos);
    }
}

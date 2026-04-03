package com.analisis.proyecto.controlador;

import com.analisis.proyecto.servicio.ServicioGestorReferencias;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bibliometria")
public class ControladorBibliometrico {

    private final ServicioGestorReferencias servicioGestorReferencias;

    public ControladorBibliometrico(ServicioGestorReferencias servicioGestorReferencias) {
        this.servicioGestorReferencias = servicioGestorReferencias;
    }

    @GetMapping("/descargar")
    public String descargarDatos(
            @RequestParam(defaultValue = "generative artificial intelligence") String consulta,
            @RequestParam(defaultValue = "100") int limite) {
        
        return servicioGestorReferencias.ejecutarProcesoDescarga(consulta, limite);
    }
}

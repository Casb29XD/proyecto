package com.analisis.proyecto.servicio.similitud.impl;

import com.analisis.proyecto.servicio.AIClientService;
import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulacionWord2Vec implements SimilitudAlgoritmo {

    private static final Logger logger = LoggerFactory.getLogger(SimulacionWord2Vec.class);

    private final AIClientService aiClientService;
    private final CosenoAlgoritmo fallbackCoseno;

    public SimulacionWord2Vec(AIClientService aiClientService, CosenoAlgoritmo fallbackCoseno) {
        this.aiClientService = aiClientService;
        this.fallbackCoseno = fallbackCoseno;
    }

    @Override
    public double calcularSimilitud(String source, String target) {
        return aiClientService.calcularWord2Vec(source, target)
                .map(score -> {
                    logger.debug("Word2Vec real aplicado con éxito.");
                    return score;
                })
                .orElseGet(() -> {
                    logger.info("Usando fallback local (Similitud de Coseno) para Word2Vec.");
                    return fallbackCoseno.calcularSimilitud(source, target);
                });
    }

    @Override
    public String getNombreAlgoritmo() {
        return "IA: Word2Vec (Real)";
    }

    @Override
    public String getExplicacion() {
        return "Invoca un microservicio de inferencia con embeddings preentrenados (KeyedVectors) para "
                + "obtener similitud semántica real. Si el servicio no está disponible o está desactivado, "
                + "aplica fallback local con similitud de coseno.";
    }
}

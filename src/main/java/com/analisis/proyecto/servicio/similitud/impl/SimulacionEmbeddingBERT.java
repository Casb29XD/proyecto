package com.analisis.proyecto.servicio.similitud.impl;

import com.analisis.proyecto.servicio.AIClientService;
import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulacionEmbeddingBERT implements SimilitudAlgoritmo {

    private static final Logger logger = LoggerFactory.getLogger(SimulacionEmbeddingBERT.class);

    private final AIClientService aiClientService;
    private final JaccardAlgoritmo fallbackJaccard;

    public SimulacionEmbeddingBERT(AIClientService aiClientService, JaccardAlgoritmo fallbackJaccard) {
        this.aiClientService = aiClientService;
        this.fallbackJaccard = fallbackJaccard;
    }

    @Override
    public double calcularSimilitud(String source, String target) {
        return aiClientService.calcularSBERT(source, target)
                .map(score -> {
                    logger.debug("SBERT real aplicado con éxito.");
                    return score;
                })
                .orElseGet(() -> {
                    logger.info("Usando fallback local (Jaccard) para SBERT.");
                    return fallbackJaccard.calcularSimilitud(source, target);
                });
    }

    @Override
    public String getNombreAlgoritmo() {
        return "IA: Sentence-BERT (Real)";
    }

    @Override
    public String getExplicacion() {
        return "Invoca un microservicio con Sentence-Transformers para calcular embeddings semánticos reales "
                + "y similitud coseno. Si el servicio no responde o está desactivado, aplica fallback local "
                + "basado en similitud de Jaccard.";
    }
}

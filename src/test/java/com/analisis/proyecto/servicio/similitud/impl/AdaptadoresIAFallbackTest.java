package com.analisis.proyecto.servicio.similitud.impl;

import com.analisis.proyecto.servicio.AIClientService;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdaptadoresIAFallbackTest {

    @Test
    void word2vecDebeUsarIaCuandoEstaDisponible() {
        Word2VecAlgoritmo algoritmo = new Word2VecAlgoritmo(
                new AIClientFake(Optional.of(0.82), Optional.empty()),
                new CosenoAlgoritmo()
        );

        double resultado = algoritmo.calcularSimilitud("generative ai model", "generative language model");
        assertEquals(0.82, resultado, 0.0001);
    }

    @Test
    void sbertDebeUsarFallbackCuandoIaNoDisponible() {
        JaccardAlgoritmo fallback = new JaccardAlgoritmo();
        SentenceBERTAlgoritmo algoritmo = new SentenceBERTAlgoritmo(
                new AIClientFake(Optional.empty(), Optional.empty()),
                fallback
        );

        String t1 = "transformer model for text";
        String t2 = "text model with transformer";
        double esperado = fallback.calcularSimilitud(t1, t2);
        double resultado = algoritmo.calcularSimilitud(t1, t2);
        assertEquals(esperado, resultado, 0.0001);
    }

    private static class AIClientFake extends AIClientService {
        private final Optional<Double> word2vec;
        private final Optional<Double> sbert;

        AIClientFake(Optional<Double> word2vec, Optional<Double> sbert) {
            super(WebClient.builder().build(), true, "http://localhost:9999", 100, 0, 0);
            this.word2vec = word2vec;
            this.sbert = sbert;
        }

        @Override
        public Optional<Double> calcularWord2Vec(String source, String target) {
            return word2vec;
        }

        @Override
        public Optional<Double> calcularSBERT(String source, String target) {
            return sbert;
        }
    }
}

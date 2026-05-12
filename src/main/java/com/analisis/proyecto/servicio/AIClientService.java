package com.analisis.proyecto.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;

@Service
public class AIClientService {

    private static final Logger logger = LoggerFactory.getLogger(AIClientService.class);

    private final WebClient webClient;
    private final boolean aiEnabled;
    private final int timeoutMs;
    private final int maxRetries;
    private final int retryDelayMs;

    public AIClientService(
            WebClient webClient,
            @Value("${similarity.ai.enabled:true}") boolean aiEnabled,
            @Value("${similarity.ai.service-url:http://ai-engine:8000}") String aiServiceUrl,
            @Value("${similarity.ai.timeout-ms:2500}") int timeoutMs,
            @Value("${similarity.ai.max-retries:1}") int maxRetries,
            @Value("${similarity.ai.retry-delay-ms:250}") int retryDelayMs
    ) {
        this.webClient = webClient.mutate().baseUrl(aiServiceUrl).build();
        this.aiEnabled = aiEnabled;
        this.timeoutMs = timeoutMs;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public Optional<Double> calcularWord2Vec(String source, String target) {
        return solicitarSimilitud("/v1/similarity/word2vec", source, target);
    }

    public Optional<Double> calcularSBERT(String source, String target) {
        return solicitarSimilitud("/v1/similarity/sbert", source, target);
    }

    private Optional<Double> solicitarSimilitud(String path, String source, String target) {
        if (!aiEnabled || source == null || target == null || source.isBlank() || target.isBlank()) {
            return Optional.empty();
        }

        SimilarityRequest request = new SimilarityRequest(source, target);

        return webClient.post()
                .uri(path)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("Sin detalles")
                        .flatMap(errorBody -> Mono.error(new IllegalStateException(
                                "Respuesta de error IA " + response.statusCode() + ": " + errorBody))))
                .bodyToMono(SimilarityResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.fixedDelay(maxRetries, Duration.ofMillis(retryDelayMs))
                        .filter(this::isRetryableError))
                .map(SimilarityResponse::normalizedScore)
                .onErrorResume(ex -> {
                    logger.warn("Fallo al invocar servicio IA en {}. Se aplicará fallback local. Causa: {}", path, ex.getMessage());
                    return Mono.empty();
                })
                .blockOptional();
    }

    private boolean isRetryableError(Throwable throwable) {
        return !(throwable instanceof IllegalArgumentException);
    }

    private record SimilarityRequest(String source, String target) {}

    private record SimilarityResponse(Double similarity) {
        private double normalizedScore() {
            if (similarity == null || similarity.isNaN() || similarity.isInfinite()) {
                return 0.0;
            }
            return Math.max(0.0, Math.min(1.0, similarity));
        }
    }
}

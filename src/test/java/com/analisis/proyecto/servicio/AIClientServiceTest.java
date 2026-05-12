package com.analisis.proyecto.servicio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIClientServiceTest {

    private DisposableServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.disposeNow();
        }
    }

    @Test
    void debeNormalizarPuntajeCuandoServicioResponde() throws Exception {
        server = HttpServer.create()
                .port(0)
                .route(routes -> routes.post("/v1/similarity/word2vec", (request, response) -> response
                        .header("Content-Type", "application/json")
                        .sendString(Mono.just("{\"similarity\":1.4}"))))
                .bindNow();

        String baseUrl = "http://localhost:" + server.port();
        AIClientService client = new AIClientService(WebClient.builder().build(), true, baseUrl, 1500, 0, 10);

        double resultado = client.calcularWord2Vec("texto uno", "texto dos").orElse(-1.0);
        assertEquals(1.0, resultado, 0.0001);
    }

    @Test
    void debeRetornarVacioCuandoIaDesactivada() {
        AIClientService client = new AIClientService(WebClient.builder().build(), false, "http://localhost:9999", 500, 0, 10);
        assertTrue(client.calcularSBERT("a", "b").isEmpty());
    }
}

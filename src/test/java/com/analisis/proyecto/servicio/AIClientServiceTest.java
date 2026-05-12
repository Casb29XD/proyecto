package com.analisis.proyecto.servicio;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIClientServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void debeNormalizarPuntajeCuandoServicioResponde() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/similarity/word2vec", this::handleOk);
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        AIClientService client = new AIClientService(WebClient.builder().build(), true, baseUrl, 1500, 0, 10);

        double resultado = client.calcularWord2Vec("texto uno", "texto dos").orElse(-1.0);
        assertEquals(1.0, resultado, 0.0001);
    }

    @Test
    void debeRetornarVacioCuandoIaDesactivada() {
        AIClientService client = new AIClientService(WebClient.builder().build(), false, "http://localhost:9999", 500, 0, 10);
        assertTrue(client.calcularSBERT("a", "b").isEmpty());
    }

    private void handleOk(HttpExchange exchange) throws IOException {
        byte[] body = "{\"similarity\":1.4}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}

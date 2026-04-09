package it.unibo.distributedbooking.hotelnode.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.unibo.distributedbooking.common.model.HealthResponse;
import it.unibo.distributedbooking.hotelnode.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;

public class HealthHttpHandler implements HttpHandler {

    private final String hotelId;

    public HealthHttpHandler(final String hotelId) {
        this.hotelId = hotelId;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        try {
            HealthResponse response = new HealthResponse(this.hotelId, "UP");
            byte[] responseBytes = JsonUtil.toJsonBytes(response);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e){
            byte[] errorBytes = JsonUtil.toJsonBytes(new HealthResponse(hotelId, "DOWN"));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }
    }
}

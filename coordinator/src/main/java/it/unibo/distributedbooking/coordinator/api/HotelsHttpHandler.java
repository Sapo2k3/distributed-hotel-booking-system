package it.unibo.distributedbooking.coordinator.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.unibo.distributedbooking.coordinator.service.HotelRegistryService;
import it.unibo.distributedbooking.hotelnode.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;

public class HotelsHttpHandler implements HttpHandler {

    private final HotelRegistryService hotelRegistryService;

    public HotelsHttpHandler(final HotelRegistryService hotelRegistryService) {
        this.hotelRegistryService = hotelRegistryService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        try {
            byte[] responseBytes = JsonUtil.toJsonBytes(hotelRegistryService.findAllHotels());
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            byte[] errorBytes = JsonUtil.toJsonBytes(new HotelsErrorResponse("Internal server error: " + e.getMessage()));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }
    }

    private record HotelsErrorResponse(String message) {
    }
}

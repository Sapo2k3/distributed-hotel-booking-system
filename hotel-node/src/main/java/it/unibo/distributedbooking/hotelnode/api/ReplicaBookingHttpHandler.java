package it.unibo.distributedbooking.hotelnode.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.unibo.distributedbooking.common.model.ReplicaBookingRequest;
import it.unibo.distributedbooking.common.model.ReplicaBookingResponse;
import it.unibo.distributedbooking.hotelnode.repository.InMemoryBookingRepository;
import it.unibo.distributedbooking.hotelnode.service.InMemoryBookingService;
import it.unibo.distributedbooking.hotelnode.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;

public class ReplicaBookingHttpHandler implements HttpHandler {

    private final InMemoryBookingService bookingService;

    public ReplicaBookingHttpHandler(final InMemoryBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        try {
            final ReplicaBookingRequest request = JsonUtil.fromJson(exchange.getRequestBody(), ReplicaBookingRequest.class);
            final ReplicaBookingResponse response = bookingService.replicateBooking(request);
            final byte[] responseBytes = JsonUtil.toJsonBytes(response);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            final byte[] errorBytes = JsonUtil.toJsonBytes(new ReplicaBookingResponse(
                    null,
                    false,
                    "Internal server error: " + e.getMessage(),
                    null
            ));
            exchange.getResponseHeaders().add("Contet-Type", "application/json");
            exchange.sendResponseHeaders(500, errorBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }
    }

}

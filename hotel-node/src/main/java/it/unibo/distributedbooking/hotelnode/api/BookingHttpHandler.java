package it.unibo.distributedbooking.hotelnode.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.service.BookingService;
import it.unibo.distributedbooking.hotelnode.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;

public class BookingHttpHandler implements HttpHandler {

    private final BookingService bookingService;

    public BookingHttpHandler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        try {
            BookingRequest request = JsonUtil.fromJson(exchange.getRequestBody(), BookingRequest.class);
            BookingResponse response = bookingService.createBooking(request);
            byte[] responseBytes = JsonUtil.toJsonBytes(response);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            byte[] errorBytes = JsonUtil.toJsonBytes(new BookingResponse(
                    null, false, "Internal server error: " + e.getMessage(), null));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }
    }
}

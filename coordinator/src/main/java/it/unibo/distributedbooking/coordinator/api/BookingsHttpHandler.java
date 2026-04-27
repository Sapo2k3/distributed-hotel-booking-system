package it.unibo.distributedbooking.coordinator.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.coordinator.service.BookingLocatorService;
import it.unibo.distributedbooking.hotelnode.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class BookingsHttpHandler implements HttpHandler {

    private final BookingLocatorService bookingLocatorService;

    public BookingsHttpHandler(final BookingLocatorService bookingLocatorService) {
        this.bookingLocatorService = bookingLocatorService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        try {
            List<Booking> bookings = bookingLocatorService.findAllBookings();
            byte[] responseBytes = JsonUtil.toJsonBytes(bookings);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            byte[] errorBytes = JsonUtil.toJsonBytes(java.util.Map.of("error", "Failed to fetch bookings"));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }
    }
}

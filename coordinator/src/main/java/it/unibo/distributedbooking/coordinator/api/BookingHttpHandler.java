package it.unibo.distributedbooking.coordinator.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.coordinator.service.BookingCoordinatorService;
import it.unibo.distributedbooking.coordinator.service.BookingLocatorService;
import it.unibo.distributedbooking.hotelnode.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class BookingHttpHandler implements HttpHandler {

    private final BookingCoordinatorService bookingCoordinatorService;
    private final BookingLocatorService bookingLocatorService;

    public BookingHttpHandler(final BookingCoordinatorService bookingCoordinatorService,
                              final BookingLocatorService bookingLocatorService) {
        this.bookingCoordinatorService = bookingCoordinatorService;
        this.bookingLocatorService = bookingLocatorService;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(method)) {
            handleCreateBooking(exchange);
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            handleGetAllBookings(exchange);
            return;
        }

        exchange.sendResponseHeaders(405, -1);
    }

    private void handleCreateBooking(final HttpExchange exchange) throws IOException {
        try {
            BookingRequest request = JsonUtil.fromJson(exchange.getRequestBody(), BookingRequest.class);
            BookingResponse response = bookingCoordinatorService.coordinateBooking(request);

            byte[] responseBytes = JsonUtil.toJsonBytes(response);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            BookingResponse errorResponse = new BookingResponse(
                    null,
                    false,
                    "Internal server error: " + e.getMessage(),
                    null
            );
            byte[] errorBytes = JsonUtil.toJsonBytes(errorResponse);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }
    }

    private void handleGetAllBookings(final HttpExchange exchange) throws IOException {
        try {
            List<Booking> bookings = bookingLocatorService.findAllBookings();
            byte[] responseBytes = JsonUtil.toJsonBytes(bookings);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            byte[] errorBytes = JsonUtil.toJsonBytes(
                    java.util.Map.of("error", "Failed to fetch bookings")
            );
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }
    }
}
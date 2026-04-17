package it.unibo.distributedbooking.coordinator.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.coordinator.service.BookingCoordinatorService;
import it.unibo.distributedbooking.hotelnode.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;

public class BookingHttpHandler implements HttpHandler {

    private final BookingCoordinatorService bookingCoordinatorService;

    public BookingHttpHandler(final BookingCoordinatorService bookingCoordinatorService){
        this.bookingCoordinatorService = bookingCoordinatorService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if(!"POST".equalsIgnoreCase(exchange.getRequestMethod())){
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        try{
            BookingRequest request = JsonUtil.fromJson(exchange.getRequestBody(), BookingRequest.class);
            BookingResponse response = bookingCoordinatorService.coordinateBooking(request);
            byte[] responseBytes = JsonUtil.toJsonBytes(response);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()){
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
}

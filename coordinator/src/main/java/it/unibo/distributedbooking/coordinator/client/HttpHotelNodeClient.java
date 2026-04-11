package it.unibo.distributedbooking.coordinator.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unibo.distributedbooking.hotelnode.util.JsonUtil;
import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.http.HttpResponse;

public class HttpHotelNodeClient implements HotelNodeClient {

    private final HttpClient httpClient;

    public HttpHotelNodeClient() {
        this(HttpClient.newHttpClient());
    }

    public HttpHotelNodeClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private BookingResponse executePost(final String url,  final Object requestBody, final String requestId){
        try {
            String jsonRequest = JsonUtil.toJson(requestBody);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return JsonUtil.fromJson(response.body(), BookingResponse.class);
            }
            return new BookingResponse(
                    requestId,
                    false,
                    "HTTP " + response.statusCode() + ": " + response.body(),
                    null
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BookingResponse(
                    requestId,
                    false,
                    "Client interrupted: " + e.getMessage(),
                    null
            );
        } catch (IOException e) {
            return new BookingResponse(
                    requestId,
                    false,
                    "Client error: " + e.getMessage(),
                    null
            );
        }
    }

    @Override
    public BookingResponse createBooking(String baseUrl, BookingRequest request) {
        return executePost(
                baseUrl + "/bookings",
                request,
                request.requestId()
        );
    }

    @Override
    public BookingResponse cancelBooking(String baseUrl, BookingCancellationRequest request) {
        return executePost(
                baseUrl + "/bookings/cancel",
                request,
                request.requestId()
        );
    }

    @Override
    public BookingResponse modifyBooking(String baseUrl, BookingModificationRequest request) {
        return executePost(
                baseUrl + "/bookings/modify",
                request,
                request.getRequestId()
        );
    }

    @Override
    public boolean isHealthy(String baseUrl) {
        try{
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}

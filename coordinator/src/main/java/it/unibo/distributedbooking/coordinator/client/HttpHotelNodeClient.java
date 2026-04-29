package it.unibo.distributedbooking.coordinator.client;

import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.model.ReplicaBookingRequest;
import it.unibo.distributedbooking.common.model.ReplicaBookingResponse;
import it.unibo.distributedbooking.hotelnode.util.JsonUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpHotelNodeClient implements HotelNodeClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final HttpClient httpClient;

    public HttpHotelNodeClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build());
    }

    public HttpHotelNodeClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private BookingResponse executeBookingPost(final String url,
                                               final Object requestBody, final String requestId) {
        try {
            final String jsonRequest = JsonUtil.toJson(requestBody);
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();
            final HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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

    private ReplicaBookingResponse executeReplicaPost(final String url, final ReplicaBookingRequest requestBody) {
        try {
            final String jsonRequest = JsonUtil.toJson(requestBody);
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();
            final HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return JsonUtil.fromJson(response.body(), ReplicaBookingResponse.class);
            }
            return new ReplicaBookingResponse(
                    requestBody.requestId(),
                    false,
                    "HTTP " + response.statusCode() + ": " + response.body(),
                    null
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ReplicaBookingResponse(
                    requestBody.requestId(),
                    false,
                    "Client interrupted: " + e.getMessage(),
                    null
            );
        } catch (IOException e) {
            return new ReplicaBookingResponse(
                    requestBody.requestId(),
                    false,
                    "Client error: " + e.getMessage(),
                    null
            );
        }
    }

    @Override
    public BookingResponse createBooking(final String baseUrl, final BookingRequest request) {
        return executeBookingPost(
                baseUrl + "/bookings",
                request,
                request.requestId()
        );
    }

    @Override
    public BookingResponse cancelBooking(final String baseUrl, final BookingCancellationRequest request) {
        return executeBookingPost(
                baseUrl + "/bookings/cancel",
                request,
                request.requestId()
        );
    }

    @Override
    public BookingResponse modifyBooking(final String baseUrl, final BookingModificationRequest request) {
        return executeBookingPost(
                baseUrl + "/bookings/modify",
                request,
                request.requestId()
        );
    }

    @Override
    public ReplicaBookingResponse replicateBooking(final String baseUrl, final ReplicaBookingRequest request) {
        return executeReplicaPost(
                baseUrl + "/internal/bookings/replicate",
                request
        );
    }

    @Override
    public boolean isHealthy(final String baseUrl) {
        final String healthUrl = baseUrl + "/health";

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            final HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Health check " + healthUrl + " -> " + response.statusCode());
            return response.statusCode() == 200;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Health check interrupted for " + healthUrl + ": " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.out.println("Health check failed for " + healthUrl + ": " + e.getMessage());
            return false;
        }
    }
}
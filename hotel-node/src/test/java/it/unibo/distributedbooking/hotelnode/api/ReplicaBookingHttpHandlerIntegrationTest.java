package it.unibo.distributedbooking.hotelnode.api;

import com.sun.net.httpserver.HttpServer;
import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingStatus;
import it.unibo.distributedbooking.common.model.ReplicaBookingRequest;
import it.unibo.distributedbooking.common.model.ReplicaBookingResponse;
import it.unibo.distributedbooking.hotelnode.repository.H2BookingRepository;
import it.unibo.distributedbooking.hotelnode.service.InMemoryBookingService;
import it.unibo.distributedbooking.hotelnode.util.JsonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplicaBookingHttpHandlerIntegrationTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldReplicateBookingIdempotentlyAndPersistOnlyOneRow() throws Exception {
        final String uniqueSuffix = UUID.randomUUID().toString();
        final String jdbcUrl = "jdbc:h2:file:" + Files.createTempDirectory("replica-test-db-" + uniqueSuffix)
                .resolve("booking-db")
                .toAbsolutePath();

        final H2BookingRepository bookingRepository = new H2BookingRepository(jdbcUrl);
        final InMemoryBookingService bookingService = new InMemoryBookingService(bookingRepository);
        final int port = findFreePort();

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/internal/bookings/replicate", new ReplicaBookingHttpHandler(bookingService));
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        final Booking booking = new Booking(
                "booking-replica-" + uniqueSuffix,
                "hotel-2",
                "room-101",
                "customer-1",
                java.time.LocalDate.of(2026, 5, 10),
                java.time.LocalDate.of(2026, 5, 12),
                BookingStatus.CONFIRMED
        );

        final ReplicaBookingRequest replicaRequest =
                new ReplicaBookingRequest("replica-request-" + uniqueSuffix, booking);

        final ReplicaBookingResponse firstResponse = sendReplicaRequest(port, replicaRequest);
        assertTrue(firstResponse.success());
        assertEquals("Booking replicated successfully", firstResponse.message());
        assertEquals(booking.bookingId(), firstResponse.booking().bookingId());

        final ReplicaBookingResponse secondResponse = sendReplicaRequest(port, replicaRequest);
        assertTrue(secondResponse.success());
        assertEquals("Booking already replicated", secondResponse.message());
        assertEquals(booking.bookingId(), secondResponse.booking().bookingId());

        final long matchingBookings = bookingRepository.findAll().stream()
                .filter(savedBooking -> savedBooking.bookingId().equals(booking.bookingId()))
                .count();

        assertEquals(1L, matchingBookings);
    }

    private ReplicaBookingResponse sendReplicaRequest(final int port,
                                                      final ReplicaBookingRequest request) throws Exception {
        final HttpClient client = HttpClient.newHttpClient();
        final String body = JsonUtil.toJson(request);

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/internal/bookings/replicate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        return JsonUtil.fromJson(response.body(), ReplicaBookingResponse.class);
    }

    private int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
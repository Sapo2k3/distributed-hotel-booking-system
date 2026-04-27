package it.unibo.distributedbooking.coordinator.api;

import com.sun.net.httpserver.HttpServer;
import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.model.BookingStatus;
import it.unibo.distributedbooking.coordinator.client.HotelNodeClient;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;
import it.unibo.distributedbooking.coordinator.service.BookingCoordinatorService;
import it.unibo.distributedbooking.coordinator.service.BookingLocatorService;
import it.unibo.distributedbooking.coordinator.service.HotelRegistryService;
import it.unibo.distributedbooking.coordinator.service.InMemoryBookingLocatorService;
import it.unibo.distributedbooking.coordinator.service.InMemoryHotelRegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CoordinatorHttpIntegrationTest {

    private HttpServer server;
    private int port;
    private HttpClient httpClient;
    private HotelRegistryService hotelRegistryService;
    private BookingLocatorService bookingLocatorService;
    private HotelNodeClient hotelNodeClient;
    private BookingCoordinatorService bookingCoordinatorService;

    @BeforeEach
    void setUp() throws IOException {
        hotelRegistryService = new InMemoryHotelRegistryService();
        bookingLocatorService = new InMemoryBookingLocatorService();
        hotelNodeClient = Mockito.mock(HotelNodeClient.class);
        bookingCoordinatorService = new BookingCoordinatorService(
                hotelRegistryService,
                hotelNodeClient,
                bookingLocatorService
        );
        hotelRegistryService.registerHotel(new HotelNodeInfo("hotel-1", "localhost", 18081));
        hotelRegistryService.registerHotel(new HotelNodeInfo("hotel-2", "localhost", 18082));
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/bookings", new BookingHttpHandler(bookingCoordinatorService, bookingLocatorService));
        server.createContext("/cancellations", new CancelHttpHandler(bookingCoordinatorService));
        server.createContext("/bookings/modify", new ModifyHttpHandler(bookingCoordinatorService));
        server.createContext("/hotels", new HotelsHttpHandler(hotelRegistryService));
        server.start();
        httpClient = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldExposeHotelsViaGetHotels() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/hotels"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("hotel-1");
        assertThat(response.body()).contains("hotel-2");
    }

    @Test
    void shouldExposeBookingsViaGetBookings() throws Exception {
        bookingLocatorService.registerBooking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/bookings"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("booking-1");
        assertThat(response.body()).contains("hotel-1");
        assertThat(response.body()).contains("CONFIRMED");
    }

    @Test
    void shouldCreateBookingViaPostBookings() throws Exception {
        HotelNodeInfo hotelNode = new HotelNodeInfo("hotel-1", "localhost", 18081);
        when(hotelNodeClient.isHealthy("http://localhost:18081")).thenReturn(true);
        Booking createdBooking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                BookingStatus.CONFIRMED
        );
        when(hotelNodeClient.createBooking(anyString(), Mockito.any(BookingRequest.class)))
                .thenReturn(new BookingResponse(
                        "req-1",
                        true,
                        "Booking created successfully",
                        createdBooking
                ));
        BookingRequest requestBody = new BookingRequest(
                "req-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );
        String json = """
                {
                  "requestId": "req-1",
                  "hotelId": "hotel-1",
                  "roomId": "room-101",
                  "customerId": "customer-1",
                  "checkInDate": "2026-04-10",
                  "checkOutDate": "2026-04-12"
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/bookings"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Booking created successfully");
        assertThat(response.body()).contains("booking-1");
    }
}
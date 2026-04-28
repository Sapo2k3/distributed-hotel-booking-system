package it.unibo.distributedbooking.coordinator;

import com.sun.net.httpserver.HttpServer;
import it.unibo.distributedbooking.coordinator.api.BookingHttpHandler;
import it.unibo.distributedbooking.coordinator.api.CancelHttpHandler;
import it.unibo.distributedbooking.coordinator.api.HotelsHttpHandler;
import it.unibo.distributedbooking.coordinator.api.ModifyHttpHandler;
import it.unibo.distributedbooking.coordinator.client.HotelNodeClient;
import it.unibo.distributedbooking.coordinator.client.HttpHotelNodeClient;
import it.unibo.distributedbooking.coordinator.heartbeat.HeartbeatService;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;
import it.unibo.distributedbooking.coordinator.service.BookingCoordinatorService;
import it.unibo.distributedbooking.coordinator.service.HotelRegistryService;
import it.unibo.distributedbooking.coordinator.service.InMemoryBookingLocatorService;
import it.unibo.distributedbooking.coordinator.service.InMemoryHotelRegistryService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CoordinatorApplication {

    private static final int DEFAULT_COORDINATOR_PORT = 8080;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private static final String DEFAULT_HOTEL_1_ID = "hotel-1";
    private static final String DEFAULT_HOTEL_1_HOST = "localhost";
    private static final int DEFAULT_HOTEL_1_PORT = 8081;

    private static final String DEFAULT_HOTEL_2_ID = "hotel-2";
    private static final String DEFAULT_HOTEL_2_HOST = "localhost";
    private static final int DEFAULT_HOTEL_2_PORT = 8082;

    public static void main(String[] args) throws IOException {
        final int coordinatorPort = resolveIntEnv("COORDINATOR_PORT", DEFAULT_COORDINATOR_PORT);

        HotelRegistryService registryService = new InMemoryHotelRegistryService();
        InMemoryBookingLocatorService bookingLocatorService = new InMemoryBookingLocatorService();
        HotelNodeClient hotelNodeClient = new HttpHotelNodeClient();

        registerConfiguredHotels(registryService);

        BookingCoordinatorService coordinatorService = new BookingCoordinatorService(
                registryService,
                hotelNodeClient,
                bookingLocatorService
        );

        HeartbeatService heartbeatService = new HeartbeatService(
                registryService,
                hotelNodeClient
        );

        HttpServer server = HttpServer.create(new InetSocketAddress(coordinatorPort), 0);
        server.createContext("/bookings", new BookingHttpHandler(coordinatorService, bookingLocatorService));
        server.createContext("/cancellations", new CancelHttpHandler(coordinatorService));
        server.createContext("/bookings/modify", new ModifyHttpHandler(coordinatorService));
        server.createContext("/hotels", new HotelsHttpHandler(registryService));

        ExecutorService executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);

        heartbeatService.start();
        server.start();

        System.out.println("Coordinator listening on port " + coordinatorPort);
        System.out.println("Registered hotels:");
        registryService.findAllHotels().forEach(hotel ->
                System.out.println("  - " + hotel.getHotelId() + " -> " + hotel.getHost() + ":" + hotel.getPort()));
        System.out.println("Endpoints:");
        System.out.println("  POST /bookings");
        System.out.println("  GET  /bookings");
        System.out.println("  POST /cancellations");
        System.out.println("  POST /bookings/modify");
        System.out.println("  GET  /hotels");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping coordinator...");
            heartbeatService.stop();
            server.stop(0);
            executor.shutdown();
            try {
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }

    private static void registerConfiguredHotels(final HotelRegistryService registryService) {
        registryService.registerHotel(new HotelNodeInfo(
                resolveStringEnv("HOTEL_1_ID", DEFAULT_HOTEL_1_ID),
                resolveStringEnv("HOTEL_1_HOST", DEFAULT_HOTEL_1_HOST),
                resolveIntEnv("HOTEL_1_PORT", DEFAULT_HOTEL_1_PORT)
        ));

        registryService.registerHotel(new HotelNodeInfo(
                resolveStringEnv("HOTEL_2_ID", DEFAULT_HOTEL_2_ID),
                resolveStringEnv("HOTEL_2_HOST", DEFAULT_HOTEL_2_HOST),
                resolveIntEnv("HOTEL_2_PORT", DEFAULT_HOTEL_2_PORT)
        ));
    }

    private static String resolveStringEnv(final String envName, final String defaultValue) {
        final String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static int resolveIntEnv(final String envName, final int defaultValue) {
        final String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for " + envName + ": " + value, e);
        }
    }
}

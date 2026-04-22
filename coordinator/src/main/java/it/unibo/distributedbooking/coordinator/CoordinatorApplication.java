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

    private static final int COORDINATOR_PORT = 8080;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    public static void main(String[] args) throws IOException {
        HotelRegistryService registryService = new InMemoryHotelRegistryService();
        InMemoryBookingLocatorService bookingLocatorService = new InMemoryBookingLocatorService();
        HotelNodeClient hotelNodeClient = new HttpHotelNodeClient();
        registryService.registerHotel(new HotelNodeInfo("hotel-1", "localhost", 8081));
        registryService.registerHotel(new HotelNodeInfo("hotel-2", "localhost", 8082));
        BookingCoordinatorService coordinatorService = new BookingCoordinatorService(
                registryService,
                hotelNodeClient,
                bookingLocatorService
        );
        HeartbeatService heartbeatService = new HeartbeatService(
                registryService,
                hotelNodeClient
        );
        HttpServer server = HttpServer.create(new InetSocketAddress(COORDINATOR_PORT), 0);
        server.createContext("/bookings", new BookingHttpHandler(coordinatorService));
        server.createContext("/cancellations", new CancelHttpHandler(coordinatorService));
        server.createContext("/bookings/modify", new ModifyHttpHandler(coordinatorService));
        server.createContext("/hotels", new HotelsHttpHandler(registryService));
        ExecutorService executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        heartbeatService.start();
        server.start();
        System.out.println("=== Distributed Hotel Booking Coordinator ===");
        System.out.println("Listening on http://localhost:" + COORDINATOR_PORT);
        System.out.println("Registered hotels:");
        registryService.findAllHotels().forEach(hotel ->
                System.out.println("  " + hotel.getHotelId() + " -> " + hotel.getHost() + ":" + hotel.getPort())
        );
        System.out.println("Endpoints:");
        System.out.println("  POST /bookings");
        System.out.println("  POST /cancellations");
        System.out.println("  POST /bookings/modify");
        System.out.println("  GET  /hotels");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== Coordinator shutdown initiated ===");
            try {
                heartbeatService.stop();
                server.stop(0);
                executor.shutdown();
                if (executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    System.out.println("Executor terminated gracefully");
                } else {
                    System.out.println("Force stopping executor...");
                    executor.shutdownNow();
                }
                System.out.println("Coordinator stopped cleanly");
            } catch (InterruptedException e) {
                System.out.println("Shutdown interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }));
    }
}
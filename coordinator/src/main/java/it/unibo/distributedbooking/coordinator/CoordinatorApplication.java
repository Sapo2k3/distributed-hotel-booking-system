package it.unibo.distributedbooking.coordinator;

import com.sun.net.httpserver.HttpServer;
import it.unibo.distributedbooking.coordinator.api.BookingHttpHandler;
import it.unibo.distributedbooking.coordinator.api.CancelHttpHandler;
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
import java.util.concurrent.Executors;

public class CoordinatorApplication {

    private static final int COORDINATOR_PORT = 8080;

    public static void main(String[] args) throws IOException {
        HotelRegistryService registryService = new InMemoryHotelRegistryService();
        InMemoryBookingLocatorService bookingLocatorService = new InMemoryBookingLocatorService();
        HotelNodeClient hotelNodeClient = new HttpHotelNodeClient();

        registryService.registerHotel(new HotelNodeInfo("hotel-1", "hotel-node-1", 8081));
        registryService.registerHotel(new HotelNodeInfo("hotel-2", "hotel-node-2", 8082));

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
        server.setExecutor(Executors.newCachedThreadPool());

        heartbeatService.start();
        server.start();

        System.out.println("Coordinator listening on port " + COORDINATOR_PORT);
        System.out.println("Endpoints:");
        System.out.println("  POST /bookings");
        System.out.println("  POST /cancellations");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping coordinator...");
            heartbeatService.stop();
            server.stop(0);
        }));
    }
}
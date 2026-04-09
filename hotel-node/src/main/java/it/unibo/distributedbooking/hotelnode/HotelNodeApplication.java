package it.unibo.distributedbooking.hotelnode;

import com.sun.net.httpserver.HttpServer;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.service.BookingService;
import it.unibo.distributedbooking.hotelnode.api.BookingHttpHandler;
import it.unibo.distributedbooking.hotelnode.api.HealthHttpHandler;
import it.unibo.distributedbooking.hotelnode.repository.BookingRepository;
import it.unibo.distributedbooking.hotelnode.repository.H2BookingRepository;
import it.unibo.distributedbooking.hotelnode.service.InMemoryBookingService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.Executors;

public class HotelNodeApplication {

    private static final int PORT = 8081;
    private static final String HOTEL_ID = "hotel-1";

    public static void main(String[] args) throws SQLException {
        try {
            BookingRepository repository = new H2BookingRepository("jdbc:h2:mem:hotel-node;DB_CLOSE_DELAY=-1");
            BookingService service = new InMemoryBookingService(repository);

            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            server.createContext("/bookings", new BookingHttpHandler(service));
            server.createContext("/health", new HealthHttpHandler(HOTEL_ID));

            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("Hotel node '" + HOTEL_ID + "' listening on port " + PORT);
            System.out.println("Endpoints:");
            System.out.println("  POST http://localhost:" + PORT + "/bookings");
            System.out.println("  GET  http://localhost:" + PORT + "/health");

        } catch (IOException e) {
            System.err.println("Failed to start hotel node: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

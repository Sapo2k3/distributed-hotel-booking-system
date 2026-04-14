package it.unibo.distributedbooking.hotelnode;

import com.sun.net.httpserver.HttpServer;
import it.unibo.distributedbooking.common.service.BookingService;
import it.unibo.distributedbooking.hotelnode.api.BookingHttpHandler;
import it.unibo.distributedbooking.hotelnode.api.CancelHttpHandler;
import it.unibo.distributedbooking.hotelnode.api.HealthHttpHandler;
import it.unibo.distributedbooking.hotelnode.api.ModifyHttpHandler;
import it.unibo.distributedbooking.hotelnode.repository.BookingRepository;
import it.unibo.distributedbooking.hotelnode.repository.H2BookingRepository;
import it.unibo.distributedbooking.hotelnode.service.InMemoryBookingService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.Executors;

public class HotelNodeApplication {

    private static final int DEFAULT_PORT = 8081;
    private static final String DEFAULT_HOTEL_ID = "hotel-1";

    public static void main(final String[] args) {
        final int port = resolvePort();
        final String hotelId = resolveHotelId();
        final String jdbcUrl = buildJdbcUrl(hotelId);

        try {
            final BookingRepository repository = new H2BookingRepository(jdbcUrl);
            final BookingService service = new InMemoryBookingService(repository);

            final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/bookings", new BookingHttpHandler(service));
            server.createContext("/bookings/cancel", new CancelHttpHandler(service));
            server.createContext("/bookings/modify", new ModifyHttpHandler(service));
            server.createContext("/health", new HealthHttpHandler(hotelId));

            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("Hotel node '" + hotelId + "' listening on port " + port);
            System.out.println("Database: " + jdbcUrl);
            System.out.println("Endpoints:");
            System.out.println("  POST /bookings");
            System.out.println("  POST /bookings/cancel");
            System.out.println("  POST /bookings/modify");
            System.out.println("  GET  /health");

        } catch (SQLException | IOException e) {
            System.err.println("Failed to start hotel node '" + hotelId + "': " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static int resolvePort() {
        final String portValue = System.getenv("HOTEL_NODE_PORT");

        if (portValue == null || portValue.isBlank()) {
            return DEFAULT_PORT;
        }

        try {
            return Integer.parseInt(portValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid HOTEL_NODE_PORT: " + portValue, e);
        }
    }

    private static String resolveHotelId() {
        final String hotelId = System.getenv("HOTEL_NODE_ID");

        if (hotelId == null || hotelId.isBlank()) {
            return DEFAULT_HOTEL_ID;
        }

        return hotelId;
    }

    private static String buildJdbcUrl(final String hotelId) {
        return "jdbc:h2:mem:" + hotelId + ";DB_CLOSE_DELAY=-1";
    }
}
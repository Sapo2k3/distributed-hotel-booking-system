package it.unibo.distributedbooking.hotelnode;

import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.hotelnode.repository.H2BookingRepository;
import it.unibo.distributedbooking.hotelnode.service.InMemoryBookingService;

import java.sql.SQLException;
import java.time.LocalDate;

public class HotelNodeApplication {

    public static void main(String[] args) throws SQLException {
        H2BookingRepository bookingRepository = new H2BookingRepository("jdbc:h2:mem:hotel-node;DB_CLOSE_DELAY=-1");
        InMemoryBookingService bookingService = new InMemoryBookingService(bookingRepository);
        BookingRequest request = new BookingRequest(
                "req-demo-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );
        BookingResponse response = bookingService.createBooking(request);
        System.out.println("Success: " + response.isSuccess());
        System.out.println("Message: " + response.getMessage());
        if (response.getBooking() != null) {
            System.out.println("Booking ID: " + response.getBooking().getId());
            System.out.println("Status: " + response.getBooking().getStatus());
        }
    }
}

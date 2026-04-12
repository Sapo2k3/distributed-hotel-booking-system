package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingStatus;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryBookingLocatorService implements BookingLocatorService {

    private final Map<String, Booking> bookingsById = new HashMap<>();

    public void registerBooking(String bookingId, String hotelId, String roomId, String customerId, LocalDate checkInDate, LocalDate checkOutDate) {
        bookingsById.put(bookingId, new Booking(
                bookingId,
                hotelId,
                roomId,
                customerId,
                checkInDate,
                checkOutDate,
                BookingStatus.CONFIRMED
        ));
    }

    @Override
    public Optional<Booking> findByBookingId(String bookingId) {
        return Optional.ofNullable(bookingsById.get(bookingId));
    }
}

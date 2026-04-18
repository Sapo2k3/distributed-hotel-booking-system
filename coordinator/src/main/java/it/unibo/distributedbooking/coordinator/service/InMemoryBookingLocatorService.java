package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingStatus;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookingLocatorService implements BookingLocatorService {

    private final Map<String, Booking> bookingsById = new ConcurrentHashMap<>();

    @Override
    public void registerBooking(final String bookingId,
                                final String hotelId,
                                final String roomId,
                                final String customerId,
                                final LocalDate checkInDate,
                                final LocalDate checkOutDate) {
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

package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    public Optional<Booking> findByBookingId(final String bookingId) {
        return Optional.ofNullable(bookingsById.get(bookingId));
    }

    @Override
    public List<Booking> findAllBookings() {
        return new ArrayList<>(bookingsById.values());
    }

    @Override
    public void updateBooking(final Booking booking) {
        if (booking != null && booking.bookingId() != null) {
            bookingsById.put(booking.bookingId(), booking);
        }
    }

    @Override
    public void markCancelled(final String bookingId) {
        bookingsById.computeIfPresent(bookingId, (id, existingBooking) -> new Booking(
                existingBooking.bookingId(),
                existingBooking.hotelId(),
                existingBooking.roomId(),
                existingBooking.customerId(),
                existingBooking.checkInDate(),
                existingBooking.checkOutDate(),
                BookingStatus.CANCELLED
        ));
    }
}
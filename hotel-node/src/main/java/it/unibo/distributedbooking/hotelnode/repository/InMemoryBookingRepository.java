package it.unibo.distributedbooking.hotelnode.repository;

import it.unibo.distributedbooking.common.model.Booking;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryBookingRepository implements BookingRepository {

    private final ConcurrentMap<String, Booking> bookingsById = new ConcurrentHashMap<>();

    @Override
    public void save(final Booking booking) {
        bookingsById.put(booking.getId(), booking);
    }

    @Override
    public Optional<Booking> findById(final String bookingId) {
        return Optional.ofNullable(bookingsById.get(bookingId));
    }

    @Override
    public List<Booking> findAll() {
        return new ArrayList<>(bookingsById.values());
    }

    @Override
    public void update(final Booking booking) {
        bookingsById.put(booking.getId(), booking);
    }
}

package it.unibo.distributedbooking.hotelnode.repository;

import it.unibo.distributedbooking.common.model.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    void save(final Booking booking);

    Optional<Booking> findById(final String bookingId);

    List<Booking> findAll();

    void update(final Booking booking);
}

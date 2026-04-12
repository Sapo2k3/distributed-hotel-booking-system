package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.Booking;

import java.util.Optional;

public interface BookingLocatorService {

    Optional<Booking> findByBookingId(String bookingId);

}

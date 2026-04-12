package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.Booking;

import java.time.LocalDate;
import java.util.Optional;

public interface BookingLocatorService {

    Optional<Booking> findByBookingId(final String bookingId);

    public void registerBooking(final String bookingId,
                                final String hotelId,
                                final String roomId,
                                final String customerId,
                                final LocalDate checkInDate,
                                final LocalDate checkOutDate);

}

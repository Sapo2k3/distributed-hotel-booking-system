package it.unibo.distributedbooking.common.model;

import java.time.LocalDate;

public record Booking(
        String bookingId,
        String hotelId,
        String roomId,
        String customerId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BookingStatus status
        ) {
}
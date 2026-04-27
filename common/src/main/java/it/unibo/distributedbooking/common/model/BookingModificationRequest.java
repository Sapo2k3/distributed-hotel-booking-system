package it.unibo.distributedbooking.common.model;

import java.time.LocalDate;

public record BookingModificationRequest(
        String requestId,
        String bookingId,
        String hotelId,
        String roomId,
        String customerId,
        LocalDate checkInDate,
        LocalDate checkOutDate
) {
}

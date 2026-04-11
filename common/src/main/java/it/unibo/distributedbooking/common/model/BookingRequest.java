package it.unibo.distributedbooking.common.model;

import java.time.LocalDate;

public record BookingRequest(
        String requestId,
        String hotelId,
        String roomId,
        String customerId,
        LocalDate checkInDate,
        LocalDate checkOutDate
) {
}

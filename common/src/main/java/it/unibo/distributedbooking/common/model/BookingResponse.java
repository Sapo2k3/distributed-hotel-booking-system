package it.unibo.distributedbooking.common.model;

public record BookingResponse(
        String requestId,
        boolean success,
        String message,
        Booking booking
) {
}
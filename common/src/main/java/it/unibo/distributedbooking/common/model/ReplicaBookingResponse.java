package it.unibo.distributedbooking.common.model;

public record ReplicaBookingResponse(
        String requestId,
        boolean success,
        String message,
        Booking booking
) {
}

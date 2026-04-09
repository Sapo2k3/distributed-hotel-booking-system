package it.unibo.distributedbooking.common.model;

public record BookingCancellationRequest(String requestId, String bookingId) {}

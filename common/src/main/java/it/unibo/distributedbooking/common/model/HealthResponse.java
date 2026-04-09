package it.unibo.distributedbooking.common.model;

import java.util.Objects;

public class HealthResponse {

    private final String hotelId;
    private final String status;

    public HealthResponse(String hotelId, String status) {
        this.hotelId = Objects.requireNonNull(hotelId);
        this.status = Objects.requireNonNull(status);
    }

    public String getHotelId() {
        return hotelId;
    }

    public String getStatus() {
        return status;
    }
}
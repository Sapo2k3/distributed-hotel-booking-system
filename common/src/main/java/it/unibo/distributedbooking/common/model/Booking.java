package it.unibo.distributedbooking.common.model;

import java.time.LocalDate;

public class Booking {

    private final String id;
    private final String hotelId;
    private final String roomId;
    private final String customerId;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final BookingStatus status;

    public Booking(final String id,
                   final String hotelId,
                   final String roomId,
                   final String customerId,
                   final LocalDate checkInDate,
                   final LocalDate checkOutDate,
                   final BookingStatus status) {
        this.id = id;
        this.hotelId = hotelId;
        this.roomId = roomId;
        this.customerId = customerId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.status = status;
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("checkOutDate must be after checkInDate");
        }
    }

    public String getId(){
        return this.id;
    }

    public String getHotelId() {
        return this.hotelId;
    }

    public String getRoomId() {
        return this.roomId;
    }

    public String getCustomerId() {
        return this.customerId;
    }

    public LocalDate getCheckInDate() {
        return this.checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return this.checkOutDate;
    }

    public BookingStatus getStatus() {
        return this.status;
    }
}

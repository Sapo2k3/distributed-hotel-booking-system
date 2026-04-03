package it.unibo.distributedbooking.common.model;

import java.time.LocalDate;

public class BookingModificationRequest {

    private final String requestId;
    private final String bookingId;
    private final String hotelId;
    private final String roomId;
    private final String customerId;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;

    public BookingModificationRequest(final String requestId,
                                      final String bookingId,
                                      final String hotelId,
                                      final String roomId,
                                      final String customerId,
                                      final LocalDate checkInDate,
                                      final LocalDate checkOutDate
                                      ){
        this.requestId = requestId;
        this.bookingId = bookingId;
        this.hotelId = hotelId;
        this.roomId = roomId;
        this.customerId = customerId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("checkOutDate must be after checkInDate");
        }
    }

    public String getRequestId() {
        return this.requestId;
    }

    public String getBookingId() {
        return this.bookingId;
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
}

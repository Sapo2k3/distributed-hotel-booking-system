package it.unibo.distributedbooking.clientgui.model;

public class BookingViewModel {

    private final String bookingId;
    private final String hotelId;
    private final String roomId;
    private final String customerId;
    private final String checkInDate;
    private final String checkOutDate;
    private final String status;

    public BookingViewModel(final String bookingId,
                            final String hotelId,
                            final String roomId,
                            final String customerId,
                            final String checkInDate,
                            final String checkOutDate,
                            final String status) {
        this.bookingId = bookingId;
        this.hotelId = hotelId;
        this.roomId = roomId;
        this.customerId = customerId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.status = status;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getHotelId() {
        return hotelId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public String getCheckOutDate() {
        return checkOutDate;
    }

    public String getStatus() {
        return status;
    }
}
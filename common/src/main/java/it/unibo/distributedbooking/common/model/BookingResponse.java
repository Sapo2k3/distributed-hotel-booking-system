package it.unibo.distributedbooking.common.model;

public class BookingResponse {

    private final String requestId;
    private final boolean success;
    private final String message;
    private final Booking booking;;

    public BookingResponse(final String requestId,
                           final boolean success,
                           final String message,
                           final Booking booking){
        this.requestId = requestId;
        this.success = success;
        this.message = message;
        this.booking = booking;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }

    public Booking getBooking() {
        return this.booking;
    }
}

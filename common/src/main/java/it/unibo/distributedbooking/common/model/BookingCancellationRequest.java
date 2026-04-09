package it.unibo.distributedbooking.common.model;

public class BookingCancellationRequest {

    private final String requestId;
    private final String bookingId;

    public BookingCancellationRequest(final String requestId, final String bookingId){
        this.requestId = requestId;
        this.bookingId = bookingId;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public String getBookingId() {
        return this.bookingId;
    }
}

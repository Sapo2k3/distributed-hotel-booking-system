package it.unibo.distributedbooking.common.service;

import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;

public interface BookingService {

    BookingResponse createBooking(final BookingRequest request);

    BookingResponse cancelBooking(final String requestId, final String bookingId, BookingRequest request);
}

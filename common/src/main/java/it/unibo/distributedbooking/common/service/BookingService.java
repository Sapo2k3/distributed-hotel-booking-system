package it.unibo.distributedbooking.common.service;

import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;

public interface BookingService {

    BookingResponse createBooking(final BookingRequest request);

    public BookingResponse cancelBooking(final BookingCancellationRequest request);

    BookingResponse modifyBooking(BookingModificationRequest request);
}

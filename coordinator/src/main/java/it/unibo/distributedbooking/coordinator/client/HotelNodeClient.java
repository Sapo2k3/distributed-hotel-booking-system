package it.unibo.distributedbooking.coordinator.client;

import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;

public interface HotelNodeClient {

    BookingResponse createBooking(final String baseUrl, final BookingRequest request);

    BookingResponse cancelBooking(final String baseUrl, final BookingCancellationRequest request);

    BookingResponse modifyBooking(final String baseUrl, final BookingModificationRequest request);

    boolean isHealthy(final String baseUrl);
}

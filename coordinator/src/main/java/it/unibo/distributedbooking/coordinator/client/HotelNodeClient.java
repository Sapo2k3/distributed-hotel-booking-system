package it.unibo.distributedbooking.coordinator.client;

import it.unibo.distributedbooking.common.model.*;

public interface HotelNodeClient {

    BookingResponse createBooking(final String baseUrl, final BookingRequest request);

    BookingResponse cancelBooking(final String baseUrl, final BookingCancellationRequest request);

    BookingResponse modifyBooking(final String baseUrl, final BookingModificationRequest request);

    ReplicaBookingResponse replicateBooking(final String baseUrl, final ReplicaBookingRequest request);

    boolean isHealthy(final String baseUrl);
}

package it.unibo.distributedbooking.hotelnode.repository;

import it.unibo.distributedbooking.common.model.BookingResponse;

import java.util.Optional;

public interface ProcessedRequestRepository {

    void save(final String requestId, final BookingResponse response);

    Optional<BookingResponse> findByRequestId(final String requestId);
}
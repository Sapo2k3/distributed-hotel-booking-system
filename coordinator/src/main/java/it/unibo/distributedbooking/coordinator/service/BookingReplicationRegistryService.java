package it.unibo.distributedbooking.coordinator.service;

import java.util.Optional;

public interface BookingReplicationRegistryService {

    void registerReplication(final String bookingId,
                             final String primaryHotelId,
                             final String replicaHotelId);

    Optional<String> findPrimaryHotelId(final String bookingId);

    Optional<String> findReplicaHotelId(final String bookingId);
}

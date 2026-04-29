package it.unibo.distributedbooking.coordinator.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookingReplicationRegistryService implements BookingReplicationRegistryService {

    private final Map<String, ReplicationInfo> replicationByBookingId = new ConcurrentHashMap<>();

    @Override
    public void registerReplication(final String bookingId,
                                    final String primaryHotelId,
                                    final String replicaHotelId) {
        if (bookingId == null || bookingId.isBlank()) {
            return;
        }
        replicationByBookingId.put(bookingId, new ReplicationInfo(primaryHotelId, replicaHotelId));
    }

    @Override
    public Optional<String> findPrimaryHotelId(final String bookingId) {
        return Optional.ofNullable(replicationByBookingId.get(bookingId)).map(ReplicationInfo::primaryHotelId);
    }

    @Override
    public Optional<String> findReplicaHotelId(final String bookingId) {
        return Optional.ofNullable(replicationByBookingId.get(bookingId))
                .map(ReplicationInfo::replicaHotelId);
    }

    private record ReplicationInfo(String primaryHotelId, String replicaHotelId) {
    }
}

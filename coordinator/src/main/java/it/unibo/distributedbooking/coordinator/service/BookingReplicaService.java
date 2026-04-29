package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.model.ReplicaBookingRequest;
import it.unibo.distributedbooking.common.model.ReplicaBookingResponse;
import it.unibo.distributedbooking.coordinator.client.HotelNodeClient;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;

public class BookingReplicaService {

    private final HotelNodeClient hotelNodeClient;
    private final ReplicationTargetSelector replicationTargetSelector;
    private final BookingReplicationRegistryService bookingReplicationRegistryService;

    public BookingReplicaService(final HotelNodeClient hotelNodeClient,
                                 final ReplicationTargetSelector replicationTargetSelector,
                                 final BookingReplicationRegistryService bookingReplicationRegistryService) {
        this.hotelNodeClient = hotelNodeClient;
        this.replicationTargetSelector = replicationTargetSelector;
        this.bookingReplicationRegistryService = bookingReplicationRegistryService;
    }

    public void replicateCreate(final BookingRequest request, final BookingResponse primaryResponse) {
        if (primaryResponse == null || !primaryResponse.success() || primaryResponse.booking() == null) {
            return;
        }
        replicationTargetSelector.findReplicaTarget(request.hotelId())
                .ifPresent(replicaTarget -> {
                    final String replicaBaseUrl = buildBaseUrl(replicaTarget);
                    if (!hotelNodeClient.isHealthy(replicaBaseUrl)) {
                        System.out.println("Replica target " + replicaTarget.getHotelId()
                                + " is not healthy. Skipping create replication.");
                        return;
                    }
                    final Booking replicaBooking = primaryResponse.booking();
                    final ReplicaBookingRequest replicaRequest = new ReplicaBookingRequest(
                            request.requestId() + "-replica-" + replicaTarget.getHotelId(),
                            replicaBooking
                    );
                    System.out.println("Replicating create for booking " + replicaBooking.bookingId()
                            + " to " + replicaTarget.getHotelId() + " at " + replicaBaseUrl);
                    final ReplicaBookingResponse replicaResponse =
                            hotelNodeClient.replicateBooking(replicaBaseUrl, replicaRequest);
                    if (replicaResponse.success()) {
                        bookingReplicationRegistryService.registerReplication(
                                replicaBooking.bookingId(),
                                request.hotelId(),
                                replicaTarget.getHotelId()
                        );
                        System.out.println("Replication metadata registered for booking "
                                + replicaBooking.bookingId());
                    } else {
                        System.out.println("Create replication failed for booking "
                                + replicaBooking.bookingId() + " to " + replicaTarget.getHotelId()
                                + ": " + replicaResponse.message());
                    }
                });
    }

    public void replicateCancel(final BookingCancellationRequest request, final Booking bookingBeforeCancellation) {
        if (bookingBeforeCancellation == null) {
            return;
        }
        System.out.println("Cancel replication not implemented yet for booking "
                + bookingBeforeCancellation.bookingId());
    }

    public void replicateModify(final BookingModificationRequest request, final BookingResponse primaryResponse) {
        if (primaryResponse == null || primaryResponse.booking() == null) {
            return;
        }
        System.out.println("Modify replication not implemented yet for booking "
                + primaryResponse.booking().bookingId());
    }

    private String buildBaseUrl(final HotelNodeInfo hotelNodeInfo) {
        return "http://" + hotelNodeInfo.getHost() + ":" + hotelNodeInfo.getPort();
    }
}
package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.*;
import it.unibo.distributedbooking.coordinator.client.HotelNodeClient;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;

public class BookingReplicaService {

    private final HotelNodeClient hotelNodeClient;
    private final ReplicationTargetSelector replicationTargetSelector;

    public BookingReplicaService(final HotelNodeClient hotelNodeClient, final ReplicationTargetSelector replicationTargetSelector) {
        this.hotelNodeClient = hotelNodeClient;
        this.replicationTargetSelector = replicationTargetSelector;
    }

    public void replicateCreate(final BookingRequest request, final BookingResponse primaryResponse) {
        if (primaryResponse == null || !primaryResponse.success() || primaryResponse.booking() == null) {
            return;
        }
        replicationTargetSelector.findReplicaTarget(request.hotelId())
                .ifPresent(replicaTarget -> {
                    final String replicaBaseUrl = buildBaseUrl(replicaTarget);
                    if (!hotelNodeClient.isHealthy(replicaBaseUrl)) {
                        System.out.println("Replica target " + replicaTarget.getHotelId() + " is not healthy. Skipping create replication.");
                        return;
                    }
                    final Booking replicaBooking = primaryResponse.booking();
                    final BookingRequest replicaRequest = new BookingRequest(
                            request.requestId() + "-replica-" + replicaTarget.getHotelId(),
                            replicaBooking.hotelId(),
                            replicaBooking.roomId(),
                            replicaBooking.customerId(),
                            replicaBooking.checkInDate(),
                            replicaBooking.checkOutDate()
                    );
                    System.out.println("Replicating create for booking " + replicaBooking.bookingId()
                            + " to " + replicaTarget.getHotelId() + " at " + replicaBaseUrl);
                    hotelNodeClient.createBooking(replicaBaseUrl, replicaRequest);
                });
    }

    public void replicateCancel(final BookingCancellationRequest request, final Booking bookingBeforeCancellation) {
        if (bookingBeforeCancellation == null) {
            return;
        }
        replicationTargetSelector.findReplicaTarget(bookingBeforeCancellation.hotelId())
                .ifPresent(replicaTarget -> {
                    final String replicaBaseUrl = buildBaseUrl(replicaTarget);
                    if (!hotelNodeClient.isHealthy(replicaBaseUrl)) {
                        System.out.println("Replica target " + replicaTarget.getHotelId() + " is not healthy. Skipping cancel replication.");
                        return;
                    }
                    final BookingCancellationRequest replicaRequest = new BookingCancellationRequest(
                            request.requestId() + "-replica-" + replicaTarget.getHotelId(),
                            request.bookingId()
                    );
                    System.out.println("Replicating cancel for booking " + request.bookingId()
                            + " to " + replicaTarget.getHotelId() + " at " + replicaBaseUrl);

                    hotelNodeClient.cancelBooking(replicaBaseUrl, replicaRequest);
                });
    }

    public void replicateModify(final BookingModificationRequest request, final BookingResponse primaryResponse) {
        if (primaryResponse == null || !primaryResponse.success() || primaryResponse.booking() == null) {
            return;
        }
        replicationTargetSelector.findReplicaTarget(primaryResponse.booking().hotelId())
                .ifPresent(replicaTarget -> {
                    final String replicaBaseUrl = buildBaseUrl(replicaTarget);
                    if (!hotelNodeClient.isHealthy(replicaBaseUrl)) {
                        System.out.println("Replica target " + replicaTarget.getHotelId() + " is not healthy. Skipping modify replication.");
                        return;
                    }
                    final Booking updatedBooking = primaryResponse.booking();
                    final BookingModificationRequest replicaRequest = new BookingModificationRequest(
                            request.requestId() + "-replica-" + replicaTarget.getHotelId(),
                            updatedBooking.bookingId(),
                            updatedBooking.hotelId(),
                            updatedBooking.roomId(),
                            updatedBooking.customerId(),
                            updatedBooking.checkInDate(),
                            updatedBooking.checkOutDate()
                    );
                    System.out.println("Replicating modify for booking " + updatedBooking.bookingId()
                            + " to " + replicaTarget.getHotelId() + " at " + replicaBaseUrl);
                    hotelNodeClient.modifyBooking(replicaBaseUrl, replicaRequest);
                });
    }

    private String buildBaseUrl(final HotelNodeInfo hotelNodeInfo) {
        return "http://" + hotelNodeInfo.getHost() + ":" + hotelNodeInfo.getPort();
    }
}

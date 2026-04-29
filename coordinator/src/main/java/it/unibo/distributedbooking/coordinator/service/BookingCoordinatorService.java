package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.model.BookingStatus;
import it.unibo.distributedbooking.coordinator.client.HotelNodeClient;
import it.unibo.distributedbooking.coordinator.client.HttpHotelNodeClient;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;

import java.util.Optional;

public class BookingCoordinatorService {

    private final HotelRegistryService hotelRegistryService;
    private final HotelNodeClient hotelNodeClient;
    private final BookingLocatorService bookingLocatorService;
    private final BookingReplicaService bookingReplicaService;

    public BookingCoordinatorService(final HotelRegistryService hotelRegistryService) {
        this.hotelRegistryService = hotelRegistryService;
        this.hotelNodeClient = new HttpHotelNodeClient();
        this.bookingLocatorService = new InMemoryBookingLocatorService();
        this.bookingReplicaService = new BookingReplicaService(
                this.hotelNodeClient,
                new InMemoryReplicationTargetSelector(hotelRegistryService),
                new InMemoryBookingReplicationRegistryService()
        );
    }

    public BookingCoordinatorService(final HotelRegistryService hotelRegistryService,
                                     final HotelNodeClient hotelNodeClient,
                                     final BookingLocatorService bookingLocatorService,
                                     final BookingReplicaService bookingReplicaService) {
        this.hotelRegistryService = hotelRegistryService;
        this.hotelNodeClient = hotelNodeClient;
        this.bookingLocatorService = bookingLocatorService;
        this.bookingReplicaService = bookingReplicaService;
    }

    public BookingCoordinatorService(final HotelRegistryService hotelRegistryService,
                                     final HotelNodeClient hotelNodeClient,
                                     final BookingLocatorService bookingLocatorService) {
        this.hotelRegistryService = hotelRegistryService;
        this.hotelNodeClient = hotelNodeClient;
        this.bookingLocatorService = bookingLocatorService;
        this.bookingReplicaService = new BookingReplicaService(
                hotelNodeClient,
                new InMemoryReplicationTargetSelector(hotelRegistryService),
                new InMemoryBookingReplicationRegistryService()
        );
    }

    public BookingResponse coordinateBooking(final BookingRequest request) {
        final Optional<HotelNodeInfo> hotelNode = hotelRegistryService.findHotelById(request.hotelId());
        if (hotelNode.isEmpty()) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Hotel node not found: " + request.hotelId(),
                    null
            );
        }
        final String baseUrl = "http://" + hotelNode.get().getHost() + ":" + hotelNode.get().getPort();
        if (!hotelNodeClient.isHealthy(baseUrl)) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Hotel node " + hotelNode.get().getHotelId() + " is not healthy",
                    null
            );
        }
        System.out.println("Routing booking to " + hotelNode.get().getHotelId() + " at " + baseUrl);
        final BookingResponse response = hotelNodeClient.createBooking(baseUrl, request);
        if (response.success() && response.booking() != null) {
            bookingLocatorService.updateBooking(response.booking());
            bookingReplicaService.replicateCreate(request, response);
        }
        return response;
    }

    public BookingResponse coordinateCancellation(final BookingCancellationRequest request) {
        final Optional<Booking> booking = bookingLocatorService.findByBookingId(request.bookingId());
        if (booking.isEmpty()) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Booking not found: " + request.bookingId(),
                    null
            );
        }
        if (booking.get().status() == BookingStatus.CANCELLED) {
            return new BookingResponse(
                    request.requestId(),
                    true,
                    "Booking already cancelled.",
                    booking.get()
            );
        }
        final Booking bookingBeforeCancellation = booking.get();
        final Optional<HotelNodeInfo> hotelNode =
                hotelRegistryService.findHotelById(bookingBeforeCancellation.hotelId());
        if (hotelNode.isEmpty()) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Hotel node not found for booking: " + request.bookingId(),
                    null
            );
        }
        final String baseUrl = "http://" + hotelNode.get().getHost() + ":" + hotelNode.get().getPort();
        if (!hotelNodeClient.isHealthy(baseUrl)) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Hotel node " + hotelNode.get().getHotelId() + " is not healthy",
                    null
            );
        }
        System.out.println("Routing cancellation to " + hotelNode.get().getHotelId() + " at " + baseUrl);
        final BookingResponse response = hotelNodeClient.cancelBooking(baseUrl, request);
        if (response.success()) {
            bookingLocatorService.markCancelled(request.bookingId());
            bookingReplicaService.replicateCancel(request, bookingBeforeCancellation);
            final Booking cancelledBooking =
                    bookingLocatorService.findByBookingId(request.bookingId()).orElse(null);
            return new BookingResponse(
                    response.requestId(),
                    true,
                    response.message(),
                    cancelledBooking
            );
        }
        return response;
    }

    public BookingResponse coordinateModification(final BookingModificationRequest request) {
        final Optional<Booking> booking = bookingLocatorService.findByBookingId(request.bookingId());
        if (booking.isEmpty()) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Booking not found: " + request.bookingId(),
                    null
            );
        }
        if (booking.get().status() == BookingStatus.CANCELLED) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Cannot modify a cancelled booking: " + request.bookingId(),
                    booking.get()
            );
        }
        final Optional<HotelNodeInfo> hotelNode =
                hotelRegistryService.findHotelById(booking.get().hotelId());
        if (hotelNode.isEmpty()) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Hotel node not found for booking: " + request.bookingId(),
                    null
            );
        }
        final String baseUrl = "http://" + hotelNode.get().getHost() + ":" + hotelNode.get().getPort();
        if (!hotelNodeClient.isHealthy(baseUrl)) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Hotel node " + hotelNode.get().getHotelId() + " is not healthy",
                    null
            );
        }
        System.out.println("Routing modification to " + hotelNode.get().getHotelId() + " at " + baseUrl);
        final BookingResponse response = hotelNodeClient.modifyBooking(baseUrl, request);
        if (response.success() && response.booking() != null) {
            bookingLocatorService.updateBooking(response.booking());
            bookingReplicaService.replicateModify(request, response);
        }
        return response;
    }
}

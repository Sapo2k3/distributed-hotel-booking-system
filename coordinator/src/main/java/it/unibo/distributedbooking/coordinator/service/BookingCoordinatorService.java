package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.coordinator.client.HotelNodeClient;
import it.unibo.distributedbooking.coordinator.client.HttpHotelNodeClient;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;

import java.util.Optional;

public class BookingCoordinatorService {

    private final HotelRegistryService hotelRegistryService;
    private final HotelNodeClient hotelNodeClient;
    private final BookingLocatorService bookingLocatorService;

    public BookingCoordinatorService(final HotelRegistryService hotelRegistryService) {
        this.hotelRegistryService = hotelRegistryService;
        this.hotelNodeClient = new HttpHotelNodeClient();
        this.bookingLocatorService = new InMemoryBookingLocatorService();
    }

    public BookingCoordinatorService(final HotelRegistryService hotelRegistryService,
                                     final HotelNodeClient hotelNodeClient,
                                     final BookingLocatorService bookingLocatorService) {
        this.hotelRegistryService = hotelRegistryService;
        this.hotelNodeClient = hotelNodeClient;
        this.bookingLocatorService = bookingLocatorService;
    }

    public BookingResponse coordinateBooking(final BookingRequest request) {
        Optional<HotelNodeInfo> hotelNode = hotelRegistryService.findHotelById(request.hotelId());

        if (hotelNode.isEmpty()) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Hotel node not found: " + request.hotelId(),
                    null
            );
        }
        String baseUrl = "http://" + hotelNode.get().getHost() + ":" + hotelNode.get().getPort();
        if (!hotelNodeClient.isHealthy(baseUrl)) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Hotel node " + hotelNode.get().getHotelId() + " is not healthy",
                    null
            );
        }
        System.out.println("Routing booking to " + hotelNode.get().getHotelId() + " at " + baseUrl);
        BookingResponse response = hotelNodeClient.createBooking(baseUrl, request);
        if (response.success() && response.booking() != null) {
            bookingLocatorService.registerBooking(
                    response.booking().bookingId(),
                    request.hotelId(),
                    request.roomId(),
                    request.customerId(),
                    request.checkInDate(),
                    request.checkOutDate()
            );
        }
        return response;
    }

    public BookingResponse coordinateCancellation(final BookingCancellationRequest request) {
        Optional<Booking> booking = bookingLocatorService.findByBookingId(request.bookingId());
        if (booking.isEmpty()) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Booking not found: " + request.bookingId(),
                    null
            );
        }
        Optional<HotelNodeInfo> hotelNode = hotelRegistryService.findHotelById(booking.get().hotelId());
        if (hotelNode.isEmpty()) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Hotel node not found for booking: " + request.bookingId(),
                    null
            );
        }
        String baseUrl = "http://" + hotelNode.get().getHost() + ":" + hotelNode.get().getPort();
        if (!hotelNodeClient.isHealthy(baseUrl)) {
            return new BookingResponse(
                    request.requestId(),
                    false,
                    "Hotel node " + hotelNode.get().getHotelId() + " is not healthy",
                    null
            );
        }
        System.out.println("Routing cancellation to " + hotelNode.get().getHotelId() + " at " + baseUrl);
        return hotelNodeClient.cancelBooking(baseUrl, request);
    }

    public BookingResponse coordinateModification(final BookingModificationRequest request) {
        Optional<Booking> booking = bookingLocatorService.findByBookingId(request.getBookingId());
        if (booking.isEmpty()) {
            return new BookingResponse(
                    request.getRequestId(),
                    false,
                    "Booking not found: " + request.getBookingId(),
                    null
            );
        }
        Optional<HotelNodeInfo> hotelNode = hotelRegistryService.findHotelById(booking.get().hotelId());
        if (hotelNode.isEmpty()) {
            return new BookingResponse(
                    request.getRequestId(),
                    false,
                    "Hotel node not found for booking: " + request.getBookingId(),
                    null
            );
        }
        String baseUrl = "http://" + hotelNode.get().getHost() + ":" + hotelNode.get().getPort();
        if (!hotelNodeClient.isHealthy(baseUrl)) {
            return new BookingResponse(
                    request.getRequestId(),
                    false,
                    "Hotel node " + hotelNode.get().getHotelId() + " is not healthy",
                    null
            );
        }
        System.out.println("Routing modification to " + hotelNode.get().getHotelId() + " at " + baseUrl);
        return hotelNodeClient.modifyBooking(baseUrl, request);
    }
}
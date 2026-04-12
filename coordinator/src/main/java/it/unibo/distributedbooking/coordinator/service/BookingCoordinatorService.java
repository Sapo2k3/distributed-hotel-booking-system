package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.coordinator.client.HotelNodeClient;
import it.unibo.distributedbooking.coordinator.client.HttpHotelNodeClient;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;

import java.util.Optional;

public class BookingCoordinatorService {

    private final HotelRegistryService hotelRegistryService;
    private final HotelNodeClient hotelNodeClient;

    public BookingCoordinatorService(final HotelRegistryService hotelRegistryService) {
        this.hotelRegistryService = hotelRegistryService;
        this.hotelNodeClient = new HttpHotelNodeClient();
    }

    public BookingCoordinatorService(final HotelRegistryService hotelRegistryService, final HotelNodeClient hotelNodeClient) {
        this.hotelRegistryService = hotelRegistryService;
        this.hotelNodeClient = hotelNodeClient;
    }

    public BookingResponse coordinateBooking(BookingRequest request) {
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
        System.out.println("Routing booking to " + hotelNode.get().getHotelId() +
                " at " + baseUrl);
        return hotelNodeClient.createBooking(baseUrl, request);
    }
}
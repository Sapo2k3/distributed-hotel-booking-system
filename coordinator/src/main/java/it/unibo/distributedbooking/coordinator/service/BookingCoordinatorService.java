package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.service.BookingService;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;

import java.util.Optional;

public class BookingCoordinatorService {

    private final HotelRegistryService hotelRegistryService;

    public BookingCoordinatorService(final HotelRegistryService hotelRegistryService) {
        this.hotelRegistryService = hotelRegistryService;
    }

    public BookingResponse coordinateBooking(BookingRequest request) {
        Optional<HotelNodeInfo> hotelNode = hotelRegistryService.findHotelById(request.getHotelId());
        if(hotelNode.isEmpty()) {
            return new BookingResponse(
                    request.getRequestId(),
                    false,
                    "Hotel node not found: " + request.getHotelId(),
                    null
            );
        }
        // TODO: implementare chiamata remota al nodo hotel
        // Per ora si restituisce un placeholder che simula il flusso
        System.out.println("Routing booking to " + hotelNode.get().getHotelId() +
                " at " + hotelNode.get().getHost() + ":" + hotelNode.get().getPort());

        // Simulazione: in futuro qui chiameremo il BookingService del nodo hotel
        BookingService hotelBookingService = simulateHotelBookingService(hotelNode.get());
        return hotelBookingService.createBooking(request);
    }

    // Placeholder per la logica futura di invio richiesta al nodo hotel
    private BookingService simulateHotelBookingService(HotelNodeInfo hotelNode) {
        // TODO: implementare stub/proxy per chiamare il nodo hotel
        // Per ora restituisce sempre successo per verificare il flusso di routing
        return new BookingService() {

            @Override
            public BookingResponse createBooking(final BookingRequest request) {
                return new BookingResponse(
                        request.getRequestId(),
                        true,
                        "Booking accepted by " + hotelNode.getHotelId(),
                        null
                );
            }

            @Override
            public BookingResponse cancelBooking(final String requestId, final String bookingId, final BookingRequest request) {
                return null;
            }

            @Override
            public BookingResponse modifyBooking(final BookingModificationRequest request) {
                return null;
            }
        };
    }
}

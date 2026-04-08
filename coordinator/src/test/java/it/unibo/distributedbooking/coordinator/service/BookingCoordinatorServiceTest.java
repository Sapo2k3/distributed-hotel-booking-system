package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BookingCoordinatorServiceTest {

    @Test
    void shouldReturnFailureWhenHotelNodeIsNotRegistered() {
        HotelRegistryService registryService = new InMemoryHotelRegistryService();
        BookingCoordinatorService coordinatorService = new BookingCoordinatorService(registryService);

        BookingRequest request = new BookingRequest(
                "req-1",
                "hotel-404",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );

        BookingResponse response = coordinatorService.coordinateBooking(request);

        assertFalse(response.isSuccess());
        assertEquals("Hotel node not found: hotel-404", response.getMessage());
        assertNull(response.getBooking());
    }

    @Test
    void shouldRouteBookingToRegisteredHotelNode() {
        HotelRegistryService registryService = new InMemoryHotelRegistryService();
        registryService.registerHotel(new HotelNodeInfo("hotel-1", "localhost", 8081));

        BookingCoordinatorService coordinatorService = new BookingCoordinatorService(registryService);

        BookingRequest request = new BookingRequest(
                "req-2",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );

        BookingResponse response = coordinatorService.coordinateBooking(request);

        assertTrue(response.isSuccess());
        assertEquals("Booking accepted by hotel-1", response.getMessage());
        assertNull(response.getBooking());
    }
}
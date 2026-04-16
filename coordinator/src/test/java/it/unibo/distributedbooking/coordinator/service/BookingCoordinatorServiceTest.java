package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.coordinator.client.HotelNodeClient;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCoordinatorServiceTest {

    @Mock
    private HotelRegistryService hotelRegistryService;

    @Mock
    private HotelNodeClient hotelNodeClient;

    @Mock
    private BookingLocatorService locatorService;

    private BookingCoordinatorService coordinatorService;

    @BeforeEach
    void setUp() {
        coordinatorService = new BookingCoordinatorService(hotelRegistryService, hotelNodeClient, locatorService);
    }

    @Test
    void shouldFailWhenHotelNotFound() {
        BookingRequest request = new BookingRequest(
                "req-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );
        when(hotelRegistryService.findHotelById("hotel-1")).thenReturn(Optional.empty());
        BookingResponse response = coordinatorService.coordinateBooking(request);
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Hotel node not found: hotel-1");
        verifyNoInteractions(hotelNodeClient);
    }
    @Test
    void shouldFailWhenHotelNotHealthy() {
        BookingRequest request = new BookingRequest(
                "req-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );
        HotelNodeInfo hotelNode = new HotelNodeInfo("hotel-1", "localhost", 8081);
        when(hotelRegistryService.findHotelById("hotel-1")).thenReturn(Optional.of(hotelNode));
        when(hotelNodeClient.isHealthy("http://localhost:8081")).thenReturn(false);
        BookingResponse response = coordinatorService.coordinateBooking(request);
        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("is not healthy");
        verify(hotelNodeClient).isHealthy("http://localhost:8081");
        verifyNoMoreInteractions(hotelNodeClient);
    }

    @Test
    void shouldRouteBookingToHealthyHotel() {
        BookingRequest request = new BookingRequest(
                "req-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );
        HotelNodeInfo hotelNode = new HotelNodeInfo("hotel-1", "localhost", 8081);
        when(hotelRegistryService.findHotelById("hotel-1")).thenReturn(Optional.of(hotelNode));
        when(hotelNodeClient.isHealthy("http://localhost:8081")).thenReturn(true);
        BookingResponse expectedResponse = new BookingResponse(
                "req-1",
                true,
                "Booking created successfully",
                null
        );
        when(hotelNodeClient.createBooking("http://localhost:8081", request))
                .thenReturn(expectedResponse);
        BookingResponse response = coordinatorService.coordinateBooking(request);
        assertThat(response).isEqualTo(expectedResponse);
        verify(hotelNodeClient).isHealthy("http://localhost:8081");
        verify(hotelNodeClient).createBooking("http://localhost:8081", request);
        verifyNoMoreInteractions(hotelNodeClient);
    }

    @Test
    void shouldFailCancellationWhenBookingNotFound() {
        BookingCancellationRequest request = new BookingCancellationRequest(
                "req-cancel-1",
                "booking-1"
        );
        when(locatorService.findByBookingId("booking-1")).thenReturn(Optional.empty());
        BookingResponse response = coordinatorService.coordinateCancellation(request);
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Booking not found: booking-1");
        verifyNoInteractions(hotelNodeClient);
        verifyNoMoreInteractions(locatorService);
    }

    @Test
    void shouldRouteCancellationToHealthyHotel() {
        BookingCancellationRequest request = new BookingCancellationRequest(
                "req-cancel-1",
                "booking-1"
        );
        Booking booking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                null
        );
        HotelNodeInfo hotelNode = new HotelNodeInfo("hotel-1", "localhost", 8081);
        when(locatorService.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(hotelRegistryService.findHotelById("hotel-1")).thenReturn(Optional.of(hotelNode));
        when(hotelNodeClient.isHealthy("http://localhost:8081")).thenReturn(true);
        BookingResponse expectedResponse = new BookingResponse(
                "req-cancel-1",
                true,
                "Booking cancelled successfully",
                null
        );
        when(hotelNodeClient.cancelBooking("http://localhost:8081", request)).thenReturn(expectedResponse);
        BookingResponse response = coordinatorService.coordinateCancellation(request);
        assertThat(response).isEqualTo(expectedResponse);
        verify(locatorService).findByBookingId("booking-1");
        verify(hotelNodeClient).isHealthy("http://localhost:8081");
        verify(hotelNodeClient).cancelBooking("http://localhost:8081", request);
        verifyNoMoreInteractions(hotelNodeClient, locatorService, hotelRegistryService);
    }

    @Test
    void shouldFailModificationWhenBookingNotFound() {
        BookingModificationRequest request = new BookingModificationRequest(
                "req-mod-1",
                "booking-1",
                "hotel-1",
                "room-202",
                "customer-1",
                LocalDate.of(2026, 4, 11),
                LocalDate.of(2026, 4, 13)
        );

        when(locatorService.findByBookingId("booking-1")).thenReturn(Optional.empty());

        BookingResponse response = coordinatorService.coordinateModification(request);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Booking not found: booking-1");
        verifyNoInteractions(hotelNodeClient);
        verifyNoMoreInteractions(locatorService);
    }

    @Test
    void shouldRouteModificationToHealthyHotel() {
        BookingModificationRequest request = new BookingModificationRequest(
                "req-mod-1",
                "booking-1",
                "hotel-1",
                "room-202",
                "customer-1",
                LocalDate.of(2026, 4, 11),
                LocalDate.of(2026, 4, 13)
        );
        Booking booking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                null
        );
        HotelNodeInfo hotelNode = new HotelNodeInfo("hotel-1", "localhost", 8081);
        when(locatorService.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(hotelRegistryService.findHotelById("hotel-1")).thenReturn(Optional.of(hotelNode));
        when(hotelNodeClient.isHealthy("http://localhost:8081")).thenReturn(true);
        BookingResponse expectedResponse = new BookingResponse(
                "req-mod-1",
                true,
                "Booking modified successfully",
                null
        );
        when(hotelNodeClient.modifyBooking("http://localhost:8081", request)).thenReturn(expectedResponse);
        BookingResponse response = coordinatorService.coordinateModification(request);
        assertThat(response).isEqualTo(expectedResponse);
        verify(locatorService).findByBookingId("booking-1");
        verify(hotelNodeClient).isHealthy("http://localhost:8081");
        verify(hotelNodeClient).modifyBooking("http://localhost:8081", request);
        verifyNoMoreInteractions(hotelNodeClient, locatorService, hotelRegistryService);
    }
}
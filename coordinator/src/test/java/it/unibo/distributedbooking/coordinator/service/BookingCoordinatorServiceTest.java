package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.*;
import it.unibo.distributedbooking.coordinator.client.HotelNodeClient;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
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

    @Mock
    private BookingReplicaService bookingReplicaService;

    private BookingCoordinatorService coordinatorService;

    @BeforeEach
    void setUp() {
        coordinatorService = new BookingCoordinatorService(
                hotelRegistryService,
                hotelNodeClient,
                locatorService,
                bookingReplicaService
        );
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
        verify(hotelRegistryService).findHotelById("hotel-1");
        verifyNoInteractions(hotelNodeClient, locatorService, bookingReplicaService);
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
        assertThat(response.message()).isEqualTo("Hotel node hotel-1 is not healthy");
        verify(hotelRegistryService).findHotelById("hotel-1");
        verify(hotelNodeClient).isHealthy("http://localhost:8081");
        verifyNoMoreInteractions(hotelRegistryService, hotelNodeClient, locatorService, bookingReplicaService);
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
        Booking createdBooking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                BookingStatus.CONFIRMED
        );
        BookingResponse expectedResponse = new BookingResponse(
                "req-1",
                true,
                "Booking created successfully",
                createdBooking
        );
        when(hotelRegistryService.findHotelById("hotel-1")).thenReturn(Optional.of(hotelNode));
        when(hotelNodeClient.isHealthy("http://localhost:8081")).thenReturn(true);
        when(hotelNodeClient.createBooking("http://localhost:8081", request))
                .thenReturn(expectedResponse);
        BookingResponse response = coordinatorService.coordinateBooking(request);
        assertThat(response).isEqualTo(expectedResponse);
        verify(hotelNodeClient).createBooking("http://localhost:8081", request);
        verify(locatorService).updateBooking(createdBooking);
        verify(bookingReplicaService).replicateCreate(request, expectedResponse);
    }

    @Test
    void shouldReplicateBookingWhenCreateIsSuccessful() {
        BookingRequest request = new BookingRequest(
                "req-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );
        Booking createdBooking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                BookingStatus.CONFIRMED
        );
        BookingResponse primaryResponse = new BookingResponse("req-1", true, "Success", createdBooking);
        HotelNodeInfo primaryNode = new HotelNodeInfo("hotel-1", "localhost", 8081);
        when(hotelRegistryService.findHotelById("hotel-1")).thenReturn(Optional.of(primaryNode));
        when(hotelNodeClient.isHealthy("http://localhost:8081")).thenReturn(true);
        when(hotelNodeClient.createBooking("http://localhost:8081", request)).thenReturn(primaryResponse);
        BookingResponse response = coordinatorService.coordinateBooking(request);
        assertThat(response.success()).isTrue();
        verify(hotelNodeClient).createBooking("http://localhost:8081", request);
        verify(bookingReplicaService).replicateCreate(request, primaryResponse);
        verify(locatorService).updateBooking(createdBooking);
    }

    @Test
    void shouldFailCancellationWhenBookingNotFound() {
        BookingCancellationRequest request = new BookingCancellationRequest("req-cancel-1", "booking-1");
        when(locatorService.findByBookingId("booking-1")).thenReturn(Optional.empty());
        BookingResponse response = coordinatorService.coordinateCancellation(request);
        assertThat(response.success()).isFalse();
        verifyNoInteractions(hotelNodeClient, hotelRegistryService, bookingReplicaService);
    }

    @Test
    void shouldReturnSuccessWhenBookingIsAlreadyCancelled() {
        BookingCancellationRequest request = new BookingCancellationRequest("req-cancel-2", "booking-1");
        Booking cancelledBooking = new Booking(
                "booking-1", "hotel-1", "room-101", "customer-1",
                LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 12), BookingStatus.CANCELLED
        );
        when(locatorService.findByBookingId("booking-1")).thenReturn(Optional.of(cancelledBooking));
        BookingResponse response = coordinatorService.coordinateCancellation(request);
        assertThat(response.success()).isTrue();
        verifyNoInteractions(hotelNodeClient, hotelRegistryService, bookingReplicaService);
    }

    @Test
    void shouldRouteCancellationToHealthyHotel() {
        BookingCancellationRequest request = new BookingCancellationRequest("req-cancel-3", "booking-1");
        Booking activeBooking = new Booking(
                "booking-1", "hotel-1", "room-101", "customer-1",
                LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 12), BookingStatus.CONFIRMED
        );
        HotelNodeInfo hotelNode = new HotelNodeInfo("hotel-1", "localhost", 8081);
        when(locatorService.findByBookingId("booking-1")).thenReturn(Optional.of(activeBooking));
        when(hotelRegistryService.findHotelById("hotel-1")).thenReturn(Optional.of(hotelNode));
        when(hotelNodeClient.isHealthy("http://localhost:8081")).thenReturn(true);
        when(hotelNodeClient.cancelBooking("http://localhost:8081", request))
                .thenReturn(new BookingResponse("req-cancel-3", true, "OK", activeBooking));
        coordinatorService.coordinateCancellation(request);
        verify(hotelNodeClient).cancelBooking("http://localhost:8081", request);
        verify(bookingReplicaService).replicateCancel(request, activeBooking);
    }
}
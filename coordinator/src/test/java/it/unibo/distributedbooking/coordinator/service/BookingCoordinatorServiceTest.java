package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.model.BookingStatus;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
        verify(hotelRegistryService).findHotelById("hotel-1");
        verifyNoInteractions(hotelNodeClient, locatorService);
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
        verifyNoMoreInteractions(hotelRegistryService, hotelNodeClient, locatorService);
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
        verify(hotelRegistryService).findHotelById("hotel-1");
        verify(hotelNodeClient).isHealthy("http://localhost:8081");
        verify(hotelNodeClient).createBooking("http://localhost:8081", request);
        verify(locatorService).updateBooking(createdBooking);
        verifyNoMoreInteractions(hotelRegistryService, hotelNodeClient, locatorService);
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
        verify(locatorService).findByBookingId("booking-1");
        verifyNoInteractions(hotelNodeClient, hotelRegistryService);
        verifyNoMoreInteractions(locatorService);
    }

    @Test
    void shouldReturnSuccessWhenBookingIsAlreadyCancelled() {
        BookingCancellationRequest request = new BookingCancellationRequest(
                "req-cancel-2",
                "booking-1"
        );
        Booking cancelledBooking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                BookingStatus.CANCELLED
        );
        when(locatorService.findByBookingId("booking-1")).thenReturn(Optional.of(cancelledBooking));
        BookingResponse response = coordinatorService.coordinateCancellation(request);
        assertThat(response).isEqualTo(new BookingResponse(
                "req-cancel-2",
                true,
                "Booking already cancelled.",
                cancelledBooking
        ));
        verify(locatorService).findByBookingId("booking-1");
        verifyNoInteractions(hotelNodeClient, hotelRegistryService);
        verifyNoMoreInteractions(locatorService);
    }

    @Test
    void shouldRouteCancellationToHealthyHotel() {
        BookingCancellationRequest request = new BookingCancellationRequest(
                "req-cancel-3",
                "booking-1"
        );
        Booking activeBooking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                BookingStatus.CONFIRMED
        );
        Booking cancelledBooking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                BookingStatus.CANCELLED
        );
        HotelNodeInfo hotelNode = new HotelNodeInfo("hotel-1", "localhost", 8081);
        when(locatorService.findByBookingId("booking-1"))
                .thenReturn(Optional.of(activeBooking), Optional.of(cancelledBooking));
        when(hotelRegistryService.findHotelById("hotel-1")).thenReturn(Optional.of(hotelNode));
        when(hotelNodeClient.isHealthy("http://localhost:8081")).thenReturn(true);
        BookingResponse nodeResponse = new BookingResponse(
                "req-cancel-3",
                true,
                "Booking cancelled successfully",
                activeBooking
        );
        when(hotelNodeClient.cancelBooking("http://localhost:8081", request))
                .thenReturn(nodeResponse);
        BookingResponse response = coordinatorService.coordinateCancellation(request);
        assertThat(response).isEqualTo(new BookingResponse(
                "req-cancel-3",
                true,
                "Booking cancelled successfully",
                cancelledBooking
        ));
        verify(locatorService, times(2)).findByBookingId("booking-1");
        verify(locatorService).markCancelled("booking-1");
        verify(hotelRegistryService).findHotelById("hotel-1");
        verify(hotelNodeClient).isHealthy("http://localhost:8081");
        verify(hotelNodeClient).cancelBooking("http://localhost:8081", request);
        verifyNoMoreInteractions(locatorService, hotelRegistryService, hotelNodeClient);
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
        verify(locatorService).findByBookingId("booking-1");
        verifyNoInteractions(hotelNodeClient, hotelRegistryService);
        verifyNoMoreInteractions(locatorService);
    }

    @Test
    void shouldReturnErrorWhenCancelledBookingIsModified() {
        BookingModificationRequest request = new BookingModificationRequest(
                "req-mod-2",
                "booking-1",
                "hotel-1",
                "room-202",
                "customer-1",
                LocalDate.of(2026, 4, 11),
                LocalDate.of(2026, 4, 13)
        );
        Booking cancelledBooking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                BookingStatus.CANCELLED
        );
        when(locatorService.findByBookingId("booking-1")).thenReturn(Optional.of(cancelledBooking));
        BookingResponse response = coordinatorService.coordinateModification(request);
        assertThat(response).isEqualTo(new BookingResponse(
                "req-mod-2",
                false,
                "Cannot modify a cancelled booking: booking-1",
                cancelledBooking
        ));
        verify(locatorService).findByBookingId("booking-1");
        verifyNoInteractions(hotelNodeClient, hotelRegistryService);
        verifyNoMoreInteractions(locatorService);
    }

    @Test
    void shouldRouteModificationToHealthyHotelAndUpdateLocator() {
        BookingModificationRequest request = new BookingModificationRequest(
                "req-mod-3",
                "booking-1",
                "hotel-1",
                "room-202",
                "customer-1",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 17)
        );
        Booking activeBooking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                BookingStatus.CONFIRMED
        );
        Booking modifiedBooking = new Booking(
                "booking-1",
                "hotel-1",
                "room-202",
                "customer-1",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 17),
                BookingStatus.CONFIRMED
        );
        HotelNodeInfo hotelNode = new HotelNodeInfo("hotel-1", "localhost", 8081);
        when(locatorService.findByBookingId("booking-1")).thenReturn(Optional.of(activeBooking));
        when(hotelRegistryService.findHotelById("hotel-1")).thenReturn(Optional.of(hotelNode));
        when(hotelNodeClient.isHealthy("http://localhost:8081")).thenReturn(true);
        BookingResponse nodeResponse = new BookingResponse(
                "req-mod-3",
                true,
                "Booking modified successfully",
                modifiedBooking
        );
        when(hotelNodeClient.modifyBooking("http://localhost:8081", request)).thenReturn(nodeResponse);
        BookingResponse response = coordinatorService.coordinateModification(request);
        assertThat(response).isEqualTo(nodeResponse);
        verify(locatorService).findByBookingId("booking-1");
        verify(hotelRegistryService).findHotelById("hotel-1");
        verify(hotelNodeClient).isHealthy("http://localhost:8081");
        verify(hotelNodeClient).modifyBooking("http://localhost:8081", request);
        verify(locatorService).updateBooking(modifiedBooking);
        verifyNoMoreInteractions(locatorService, hotelRegistryService, hotelNodeClient);
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
        HotelNodeInfo primaryNode = new HotelNodeInfo("hotel-1", "localhost", 8081);
        HotelNodeInfo replicaNode = new HotelNodeInfo("hotel-2", "localhost", 8082);
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
        when(hotelRegistryService.findHotelById("hotel-1")).thenReturn(Optional.of(primaryNode));
        when(hotelNodeClient.isHealthy("http://localhost:8081")).thenReturn(true);
        when(hotelNodeClient.createBooking("http://localhost:8081", request)).thenReturn(primaryResponse);
        when(hotelRegistryService.findAllHotels()).thenReturn(java.util.List.of(primaryNode, replicaNode));
        when(hotelNodeClient.isHealthy("http://localhost:8082")).thenReturn(true);
        BookingResponse response = coordinatorService.coordinateBooking(request);
        assertThat(response.success()).isTrue();
        verify(hotelNodeClient).createBooking("http://localhost:8081", request);
        verify(hotelNodeClient).replicateBooking(
                org.mockito.ArgumentMatchers.eq("http://localhost:8082"),
                org.mockito.ArgumentMatchers.argThat(r -> r.booking().bookingId().equals("booking-1"))
        );
        verify(locatorService).updateBooking(createdBooking);
    }
}
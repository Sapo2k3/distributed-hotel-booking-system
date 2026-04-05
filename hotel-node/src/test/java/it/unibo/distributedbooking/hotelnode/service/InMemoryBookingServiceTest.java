package it.unibo.distributedbooking.hotelnode.service;

import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.model.BookingStatus;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryBookingServiceTest {

    @Test
    void shouldCreateBookingWhenRoomIsAvailable() {
        InMemoryBookingService bookingService = new InMemoryBookingService();
        BookingRequest request = new BookingRequest(
                "request-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );
        BookingResponse response = bookingService.createBooking(request);
        assertTrue(response.isSuccess());
        assertNotNull(response.getBooking());
        assertEquals("request-1", response.getRequestId());
        assertEquals(BookingStatus.CONFIRMED, response.getBooking().getStatus());
    }

    @Test
    void shouldRejectBookingWhenRoomIsAlreadyBookedForOverlappingDates(){
        InMemoryBookingService bookingService = new InMemoryBookingService();
        BookingRequest firstRequest = new BookingRequest(
                "request-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );
        BookingRequest secondRequest = new BookingRequest(
                "req-2",
                "hotel-1",
                "room-101",
                "customer-2",
                LocalDate.of(2026, 4, 11),
                LocalDate.of(2026, 4, 13)
        );
        BookingResponse firstResponse = bookingService.createBooking(firstRequest);
        BookingResponse secondResponse = bookingService.createBooking(secondRequest);;
        assertTrue(firstResponse.isSuccess());
        assertFalse(secondResponse.isSuccess());
        assertNull(secondResponse.getBooking());
    }

    @Test
    void shouldReturnSameResponseWhenSameRequestIdIsSubmittedTwice(){
        InMemoryBookingService bookingService = new InMemoryBookingService();
        BookingRequest request = new BookingRequest(
                "request-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );
        BookingResponse firstResponse = bookingService.createBooking(request);
        BookingResponse secondResponse = bookingService.createBooking(request);
        assertEquals(firstResponse.isSuccess(), secondResponse.isSuccess());
        assertEquals(firstResponse.getRequestId(), secondResponse.getRequestId());
        assertEquals(firstResponse.getMessage(), secondResponse.getMessage());
        assertNotNull(firstResponse.getBooking());
        assertNotNull(secondResponse.getBooking());
        assertEquals(firstResponse.getBooking().getId(), secondResponse.getBooking().getId());
    }

    @Test
    void shouldModifyBookingWhenNewDatesDoNotConflict(){
        InMemoryBookingService bookingService = new InMemoryBookingService();
        BookingRequest createRequest = new BookingRequest(
                "request-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );
        BookingResponse createResponse = bookingService.createBooking(createRequest);
        BookingModificationRequest modificationRequest = new BookingModificationRequest(
                "request-2",
                createResponse.getBooking().getId(),
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 13),
                LocalDate.of(2026, 4, 15)
        );
        BookingResponse modificationResponse = bookingService.modifyBooking(modificationRequest);
        assertTrue(modificationResponse.isSuccess());
        assertNotNull(modificationResponse.getBooking());
        assertEquals(BookingStatus.MODIFIED, modificationResponse.getBooking().getStatus());
        assertEquals(LocalDate.of(2026, 4, 13), modificationResponse.getBooking().getCheckInDate());
        assertEquals(LocalDate.of(2026, 4, 15), modificationResponse.getBooking().getCheckOutDate());
    }

    @Test
    void shouldRejectModificationWhenNewDatesConflictWithAnotherBooking(){
        InMemoryBookingService bookingService = new InMemoryBookingService();
        BookingRequest firstBookingRequest = new BookingRequest(
                "request-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );
        BookingRequest secondBookingRequest = new BookingRequest(
                "request2",
                "hotel-1",
                "room-102",
                "customer-2",
                LocalDate.of(2026, 4, 11),
                LocalDate.of(2026, 4, 13)
        );
        BookingResponse firstBookingResponse = bookingService.createBooking(firstBookingRequest);
        bookingService.createBooking(secondBookingRequest);
        BookingModificationRequest modificationRequest = new BookingModificationRequest(
                "request-3",
                firstBookingResponse.getBooking().getId(),
                "hotel-1",
                "room-102",
                "customer-1",
                LocalDate.of(2026, 4, 11),
                LocalDate.of(2026, 4, 13)
        );
        BookingResponse modificationResponse = bookingService.modifyBooking(modificationRequest);
        assertFalse(modificationResponse.isSuccess());
        assertNull(modificationResponse.getBooking());
    }
}

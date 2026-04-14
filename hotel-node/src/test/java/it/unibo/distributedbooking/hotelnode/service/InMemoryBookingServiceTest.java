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
        assertTrue(response.success());
        assertNotNull(response.booking());
        assertEquals("request-1", response.requestId());
        assertEquals(BookingStatus.CONFIRMED, response.booking().status());
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
        assertTrue(firstResponse.success());
        assertFalse(secondResponse.success());
        assertNull(secondResponse.booking());
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
        assertEquals(firstResponse.success(), secondResponse.success());
        assertEquals(firstResponse.requestId(), secondResponse.requestId());
        assertEquals(firstResponse.message(), secondResponse.message());
        assertNotNull(firstResponse.booking());
        assertNotNull(secondResponse.booking());
        assertEquals(firstResponse.booking().bookingId(), secondResponse.booking().bookingId());
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
                createResponse.booking().bookingId(),
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 13),
                LocalDate.of(2026, 4, 15)
        );
        BookingResponse modificationResponse = bookingService.modifyBooking(modificationRequest);
        assertTrue(modificationResponse.success());
        assertNotNull(modificationResponse.booking());
        assertEquals(BookingStatus.MODIFIED, modificationResponse.booking().status());
        assertEquals(LocalDate.of(2026, 4, 13), modificationResponse.booking().checkInDate());
        assertEquals(LocalDate.of(2026, 4, 15), modificationResponse.booking().checkOutDate());
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
                firstBookingResponse.booking().bookingId(),
                "hotel-1",
                "room-102",
                "customer-1",
                LocalDate.of(2026, 4, 11),
                LocalDate.of(2026, 4, 13)
        );
        BookingResponse modificationResponse = bookingService.modifyBooking(modificationRequest);
        assertFalse(modificationResponse.success());
        assertNull(modificationResponse.booking());
    }
}

package it.unibo.distributedbooking.hotelnode.service;

import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.model.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryBookingServiceTest {

    @Test
    void shouldCreateBookingWhenRoomIsAvaiable() {
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
}

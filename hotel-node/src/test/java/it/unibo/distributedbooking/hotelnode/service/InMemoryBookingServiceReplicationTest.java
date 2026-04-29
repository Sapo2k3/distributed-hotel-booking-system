package it.unibo.distributedbooking.hotelnode.service;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingStatus;
import it.unibo.distributedbooking.common.model.ReplicaBookingRequest;
import it.unibo.distributedbooking.common.model.ReplicaBookingResponse;
import it.unibo.distributedbooking.hotelnode.repository.InMemoryBookingRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryBookingServiceReplicationTest {

    @Test
    void shouldReturnAlreadyReplicatedWhenBookingAlreadyExists() {
        final InMemoryBookingRepository bookingRepository = new InMemoryBookingRepository();
        final InMemoryBookingService bookingService = new InMemoryBookingService(bookingRepository);

        final Booking booking = new Booking(
                "booking-1",
                "hotel-2",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 12),
                BookingStatus.CONFIRMED
        );

        final ReplicaBookingRequest request = new ReplicaBookingRequest("replica-request-1", booking);

        final ReplicaBookingResponse firstResponse = bookingService.replicateBooking(request);
        assertTrue(firstResponse.success());
        assertEquals("Booking replicated successfully", firstResponse.message());

        final ReplicaBookingResponse secondResponse = bookingService.replicateBooking(request);
        assertTrue(secondResponse.success());
        assertEquals("Booking already replicated", secondResponse.message());

        final long count = bookingRepository.findAll().stream()
                .filter(savedBooking -> savedBooking.bookingId().equals("booking-1"))
                .count();

        assertEquals(1L, count);
    }
}

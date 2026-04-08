package it.unibo.distributedbooking.hotelnode.repository;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class H2BookingRepositoryTest {

    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() throws SQLException {
        bookingRepository = new H2BookingRepository("jdbc:h2:mem:testdb-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
    }

    @Test
    void shouldSaveAndFindBookingById() {
        Booking booking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                BookingStatus.CONFIRMED
        );

        bookingRepository.save(booking);

        Optional<Booking> result = bookingRepository.findById("booking-1");

        assertTrue(result.isPresent());
        assertEquals("booking-1", result.get().getId());
        assertEquals("hotel-1", result.get().getHotelId());
        assertEquals(BookingStatus.CONFIRMED, result.get().getStatus());
    }

    @Test
    void shouldReturnAllSavedBookings() {
        Booking firstBooking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                BookingStatus.CONFIRMED
        );

        Booking secondBooking = new Booking(
                "booking-2",
                "hotel-1",
                "room-102",
                "customer-2",
                LocalDate.of(2026, 4, 13),
                LocalDate.of(2026, 4, 15),
                BookingStatus.CANCELLED
        );

        bookingRepository.save(firstBooking);
        bookingRepository.save(secondBooking);

        List<Booking> bookings = bookingRepository.findAll();

        assertEquals(2, bookings.size());
    }

    @Test
    void shouldUpdateExistingBooking() {
        Booking booking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                BookingStatus.CONFIRMED
        );

        bookingRepository.save(booking);

        Booking updatedBooking = new Booking(
                "booking-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 11),
                LocalDate.of(2026, 4, 14),
                BookingStatus.MODIFIED
        );

        bookingRepository.update(updatedBooking);

        Optional<Booking> result = bookingRepository.findById("booking-1");

        assertTrue(result.isPresent());
        assertEquals(LocalDate.of(2026, 4, 11), result.get().getCheckInDate());
        assertEquals(LocalDate.of(2026, 4, 14), result.get().getCheckOutDate());
        assertEquals(BookingStatus.MODIFIED, result.get().getStatus());
    }
}
package it.unibo.distributedbooking.hotelnode.repository;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.model.BookingStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class H2ProcessedRequestRepositoryTest {

    private static final String TEST_DB_DIR = "./target/test-db-processed";
    private static final String TEST_JDBC_URL = "jdbc:h2:file:" + TEST_DB_DIR + "/test-processed-repo";
    private ProcessedRequestRepository repository;

    @BeforeAll
    static void setupTestDir() throws Exception {
        Files.createDirectories(Path.of(TEST_DB_DIR));
    }

    @AfterAll
    static void cleanupTestDir() throws Exception {
        Files.walk(Path.of(TEST_DB_DIR))
                .sorted((p1, p2) -> -p1.compareTo(p2))
                .map(Path::toFile)
                .forEach(java.io.File::delete);
    }

    @BeforeEach
    void setup() {
        this.repository = new H2ProcessedRequestRepository(TEST_JDBC_URL);
    }

    @Test
    void saveAndFindByRequestId_successResponse() {
        final String requestId = UUID.randomUUID().toString();
        final Booking booking = new Booking(
                UUID.randomUUID().toString(),
                "hotel-1",
                "room-101",
                "customer-123",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 3),
                BookingStatus.CONFIRMED
        );
        final BookingResponse response = new BookingResponse(
                requestId,
                true,
                "Booking created successfully",
                booking
        );
        repository.save(requestId, response);
        final Optional<BookingResponse> found = repository.findByRequestId(requestId);
        assertTrue(found.isPresent());
        final BookingResponse foundResponse = found.get();
        assertEquals(requestId, foundResponse.requestId());
        assertTrue(foundResponse.success());
        assertEquals("Booking created successfully", foundResponse.message());
        assertNotNull(foundResponse.booking());
        assertEquals(booking.bookingId(), foundResponse.booking().bookingId());
        assertEquals("room-101", foundResponse.booking().roomId());
        assertEquals(BookingStatus.CONFIRMED, foundResponse.booking().status());
    }

    @Test
    void saveAndFindByRequestId_errorResponse() {
        final String requestId = UUID.randomUUID().toString();
        final BookingResponse response = new BookingResponse(
                requestId,
                false,
                "Room is not available for the selected dates",
                null
        );
        repository.save(requestId, response);
        final Optional<BookingResponse> found = repository.findByRequestId(requestId);
        assertTrue(found.isPresent());
        final BookingResponse foundResponse = found.get();
        assertFalse(foundResponse.success());
        assertEquals("Room is not available for the selected dates", foundResponse.message());
        assertNull(foundResponse.booking());
    }

    @Test
    void findByRequestId_notFound_returnsEmpty() {
        final Optional<BookingResponse> found = repository.findByRequestId("non-existent-id");
        assertTrue(found.isEmpty());
    }

    @Test
    void saveOverwrite_sameRequestId() {
        final String requestId = UUID.randomUUID().toString();
        final Booking booking1 = new Booking(
                UUID.randomUUID().toString(),
                "hotel-1",
                "room-101",
                "customer-123",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 3),
                BookingStatus.CONFIRMED
        );
        final BookingResponse response1 = new BookingResponse(
                requestId,
                true,
                "First save",
                booking1
        );
        final Booking booking2 = new Booking(
                booking1.bookingId(),
                "hotel-1",
                "room-101",
                "customer-123",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 12),
                BookingStatus.MODIFIED
        );
        final BookingResponse response2 = new BookingResponse(
                requestId,
                true,
                "Second save",
                booking2
        );
        repository.save(requestId, response1);
        repository.save(requestId, response2);
        final Optional<BookingResponse> found = repository.findByRequestId(requestId);
        assertTrue(found.isPresent());
        final BookingResponse foundResponse = found.get();
        assertEquals("Second save", foundResponse.message());
        assertNotNull(foundResponse.booking());
        assertEquals(LocalDate.of(2026, 5, 10), foundResponse.booking().checkInDate());
        assertEquals(BookingStatus.MODIFIED, foundResponse.booking().status());
    }

    @Test
    void persistenceAcrossInstances_restartSimulation() {
        final String requestId = UUID.randomUUID().toString();
        final Booking booking = new Booking(
                UUID.randomUUID().toString(),
                "hotel-1",
                "room-101",
                "customer-123",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 3),
                BookingStatus.CONFIRMED
        );
        final BookingResponse response = new BookingResponse(
                requestId,
                true,
                "Persisted across restart",
                booking
        );
        final ProcessedRequestRepository repo1 = new H2ProcessedRequestRepository(TEST_JDBC_URL);
        repo1.save(requestId, response);
        final ProcessedRequestRepository repo2 = new H2ProcessedRequestRepository(TEST_JDBC_URL);
        final Optional<BookingResponse> foundAfterRestart = repo2.findByRequestId(requestId);
        assertTrue(foundAfterRestart.isPresent());
        final BookingResponse foundResponse = foundAfterRestart.get();
        assertEquals(requestId, foundResponse.requestId());
        assertTrue(foundResponse.success());
        assertEquals("Persisted across restart", foundResponse.message());
        assertEquals(booking, foundResponse.booking());
    }
}
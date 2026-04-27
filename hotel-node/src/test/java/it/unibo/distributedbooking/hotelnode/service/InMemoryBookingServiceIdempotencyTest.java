package it.unibo.distributedbooking.hotelnode.service;

import it.unibo.distributedbooking.common.model.*;
import it.unibo.distributedbooking.hotelnode.repository.H2BookingRepository;
import it.unibo.distributedbooking.hotelnode.repository.H2ProcessedRequestRepository;
import it.unibo.distributedbooking.hotelnode.repository.ProcessedRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBookingServiceIdempotencyTest {

    private static final String TEST_DB_BASE = "./target/test-db-service-idemp";
    private String testJdbcUrl;
    private InMemoryBookingService service;
    private H2BookingRepository bookingRepo;
    private ProcessedRequestRepository processedRepo;

    @BeforeEach
    void setup() throws Exception {
        final String testId = UUID.randomUUID().toString().substring(0, 8);
        testJdbcUrl = "jdbc:h2:file:" + TEST_DB_BASE + "-" + testId;
        Files.createDirectories(Path.of(TEST_DB_BASE).getParent());
        bookingRepo = new H2BookingRepository(testJdbcUrl);
        processedRepo = new H2ProcessedRequestRepository(testJdbcUrl);
        service = new InMemoryBookingService(bookingRepo, processedRepo);
    }

    @AfterEach
    void cleanupDb() throws Exception {
        if (testJdbcUrl != null) {
            Files.walk(Path.of(TEST_DB_BASE).getParent())
                    .filter(p -> p.toString().contains(testJdbcUrl.substring(testJdbcUrl.lastIndexOf('/') + 1)))
                    .sorted((p1, p2) -> -p1.compareTo(p2))
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        }
    }

    @Test
    void createBooking_idempotentAfterRestart() {
        final BookingRequest request = new BookingRequest(
                "idemp-test-create",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 3)
        );
        final BookingResponse firstResponse = service.createBooking(request);
        assertTrue(firstResponse.success());
        assertNotNull(firstResponse.booking());
        final String bookingId = firstResponse.booking().bookingId();
        final InMemoryBookingService restartedService = new InMemoryBookingService(bookingRepo, processedRepo);
        final BookingResponse secondResponse = restartedService.createBooking(request);
        assertEquals(firstResponse.requestId(), secondResponse.requestId());
        assertEquals(firstResponse.success(), secondResponse.success());
        assertEquals(firstResponse.message(), secondResponse.message());
        assertEquals(bookingId, secondResponse.booking().bookingId());
        assertEquals(1, bookingRepo.findAll().size());
    }

    @Test
    void cancelBooking_idempotentAfterRestart() {
        final BookingRequest createRequest = new BookingRequest(
                "create-cancel",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 3)
        );
        final BookingResponse createResponse = service.createBooking(createRequest);
        assertTrue(createResponse.success());
        final String bookingId = createResponse.booking().bookingId();
        final BookingCancellationRequest cancelRequest = new BookingCancellationRequest("idemp-cancel", bookingId);
        final BookingResponse firstCancel = service.cancelBooking(cancelRequest);
        assertTrue(firstCancel.success());
        final InMemoryBookingService restartedService = new InMemoryBookingService(bookingRepo, processedRepo);
        final BookingResponse secondCancel = restartedService.cancelBooking(cancelRequest);
        assertEquals(firstCancel.requestId(), secondCancel.requestId());
        assertEquals(firstCancel.success(), secondCancel.success());
        assertEquals(firstCancel.message(), secondCancel.message());
        assertEquals(bookingId, secondCancel.booking().bookingId());
        assertEquals(BookingStatus.CANCELLED, secondCancel.booking().status());
        assertEquals(1, bookingRepo.findAll().size());
    }

    @Test
    void modifyBooking_idempotentAfterRestart() {
        final BookingRequest createRequest = new BookingRequest(
                "create-modify",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 3)
        );
        final BookingResponse createResponse = service.createBooking(createRequest);
        assertTrue(createResponse.success());
        final String bookingId = createResponse.booking().bookingId();
        final BookingModificationRequest modifyRequest = new BookingModificationRequest(
                "idemp-modify",
                bookingId,
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 12)
        );
        final BookingResponse firstModify = service.modifyBooking(modifyRequest);
        assertTrue(firstModify.success());
        final InMemoryBookingService restartedService = new InMemoryBookingService(bookingRepo, processedRepo);
        final BookingResponse secondModify = restartedService.modifyBooking(modifyRequest);
        assertEquals(firstModify.requestId(), secondModify.requestId());
        assertEquals(firstModify.success(), secondModify.success());
        assertEquals(firstModify.message(), secondModify.message());
        assertEquals(1, bookingRepo.findAll().size());
    }

    @Test
    void idempotentErrorResponse_roomBusy() {
        final BookingRequest busyRequest = new BookingRequest(
                "busy-room",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 3)
        );
        service.createBooking(busyRequest);
        final BookingRequest conflictRequest = new BookingRequest(
                "idemp-error",
                "hotel-1",
                "room-101",
                "customer-2",
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 4)
        );
        final BookingResponse firstResponse = service.createBooking(conflictRequest);
        assertFalse(firstResponse.success());
        assertNull(firstResponse.booking());
        final BookingResponse secondResponse = service.createBooking(conflictRequest);
        assertEquals(firstResponse.message(), secondResponse.message());
        assertEquals(1, bookingRepo.findAll().size());
    }
}
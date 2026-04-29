package it.unibo.distributedbooking.hotelnode.service;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.model.BookingStatus;
import it.unibo.distributedbooking.common.model.ReplicaBookingRequest;
import it.unibo.distributedbooking.common.model.ReplicaBookingResponse;
import it.unibo.distributedbooking.common.service.BookingService;
import it.unibo.distributedbooking.hotelnode.repository.BookingRepository;
import it.unibo.distributedbooking.hotelnode.repository.InMemoryBookingRepository;
import it.unibo.distributedbooking.hotelnode.repository.ProcessedRequestRepository;

import java.util.UUID;

public class InMemoryBookingService implements BookingService {

    private final BookingRepository bookingRepository;
    private final ProcessedRequestRepository processedRequestRepository;

    public InMemoryBookingService() {
        this.bookingRepository = new InMemoryBookingRepository();
        this.processedRequestRepository = null;
    }

    public InMemoryBookingService(final BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
        this.processedRequestRepository = null;
    }

    public InMemoryBookingService(final BookingRepository bookingRepository,
                                  final ProcessedRequestRepository processedRequestRepository) {
        this.bookingRepository = bookingRepository;
        this.processedRequestRepository = processedRequestRepository;
    }

    @Override
    public BookingResponse createBooking(final BookingRequest request) {
        final BookingResponse existingResponse = loadExistingResponse(request.requestId());
        if (existingResponse != null) {
            return existingResponse;
        }
        final boolean roomAlreadyBooked = bookingRepository.findAll().stream()
                .filter(booking -> booking.roomId().equals(request.roomId()))
                .filter(booking -> booking.hotelId().equals(request.hotelId()))
                .filter(booking -> booking.status() == BookingStatus.CONFIRMED)
                .anyMatch(booking ->
                        request.checkInDate().isBefore(booking.checkOutDate())
                                && request.checkOutDate().isAfter(booking.checkInDate())
                );
        final BookingResponse response;
        if (roomAlreadyBooked) {
            response = new BookingResponse(
                    request.requestId(),
                    false,
                    "Room is not avaiable for the selected dates",
                    null
            );
        } else {
            final Booking booking = new Booking(
                    UUID.randomUUID().toString(),
                    request.hotelId(),
                    request.roomId(),
                    request.customerId(),
                    request.checkInDate(),
                    request.checkOutDate(),
                    BookingStatus.CONFIRMED
            );
            bookingRepository.save(booking);
            response = new BookingResponse(
                    request.requestId(),
                    true,
                    "Booking created successfully",
                    booking
            );
        }
        saveResponse(request.requestId(), response);
        return response;
    }

    @Override
    public BookingResponse cancelBooking(final BookingCancellationRequest request) {
        final BookingResponse existingResponse = loadExistingResponse(request.requestId());
        if (existingResponse != null) {
            return existingResponse;
        }
        final BookingResponse response = bookingRepository.findById(request.bookingId())
                .map(booking -> {
                    if (booking.status() == BookingStatus.CANCELLED) {
                        return new BookingResponse(
                                request.requestId(),
                                true,
                                "Booking already cancelled.",
                                booking
                        );
                    }
                    final Booking cancelledBooking = new Booking(
                            booking.bookingId(),
                            booking.hotelId(),
                            booking.roomId(),
                            booking.customerId(),
                            booking.checkInDate(),
                            booking.checkOutDate(),
                            BookingStatus.CANCELLED
                    );
                    bookingRepository.update(cancelledBooking);
                    return new BookingResponse(
                            request.requestId(),
                            true,
                            "Booking cancelled successfully",
                            cancelledBooking
                    );
                })
                .orElseGet(() -> new BookingResponse(
                        request.requestId(),
                        false,
                        "Booking not found",
                        null
                ));
        saveResponse(request.requestId(), response);
        return response;
    }

    @Override
    public BookingResponse modifyBooking(final BookingModificationRequest request) {
        final BookingResponse existingResponse = loadExistingResponse(request.requestId());
        if (existingResponse != null) {
            return existingResponse;
        }
        final BookingResponse response = bookingRepository.findById(request.bookingId())
                .map(existingBooking -> {
                    if (existingBooking.status() != BookingStatus.CONFIRMED) {
                        return new BookingResponse(
                                request.requestId(),
                                false,
                                "Only confirmed bookings can be modified",
                                null
                        );
                    }
                    final boolean roomAlreadyBooked = bookingRepository.findAll().stream()
                            .filter(booking -> !booking.bookingId().equals(request.bookingId()))
                            .filter(booking -> booking.roomId().equals(request.roomId()))
                            .filter(booking -> booking.hotelId().equals(request.hotelId()))
                            .filter(booking -> booking.status() == BookingStatus.CONFIRMED)
                            .anyMatch(booking ->
                                    request.checkInDate().isBefore(booking.checkOutDate())
                                            && request.checkOutDate().isAfter(booking.checkInDate())
                            );
                    if (roomAlreadyBooked) {
                        return new BookingResponse(
                                request.requestId(),
                                false,
                                "Room is not available for the selected dates",
                                null
                        );
                    }
                    final Booking modifiedBooking = new Booking(
                            existingBooking.bookingId(),
                            request.hotelId(),
                            request.roomId(),
                            request.customerId(),
                            request.checkInDate(),
                            request.checkOutDate(),
                            BookingStatus.MODIFIED
                    );
                    bookingRepository.update(modifiedBooking);
                    return new BookingResponse(
                            request.requestId(),
                            true,
                            "Booking modified successfully",
                            modifiedBooking
                    );
                })
                .orElseGet(() -> new BookingResponse(
                        request.requestId(),
                        false,
                        "Booking not found",
                        null
                ));
        saveResponse(request.requestId(), response);
        return response;
    }

    public ReplicaBookingResponse replicateBooking(final ReplicaBookingRequest request) {
        final Booking booking = request.booking();
        if (booking == null) {
            return new ReplicaBookingResponse(
                    request.requestId(),
                    false,
                    "Replica booking payload is missing",
                    null
            );
        }
        final Booking existingBooking = bookingRepository.findById(booking.bookingId()).orElse(null);
        if (existingBooking != null) {
            return new ReplicaBookingResponse(
                    request.requestId(),
                    true,
                    "Booking already replicated",
                    existingBooking
            );
        }
        bookingRepository.save(booking);
        return new ReplicaBookingResponse(
                request.requestId(),
                true,
                "Booking replicated successfully",
                booking
        );
    }

    private BookingResponse loadExistingResponse(final String requestId) {
        if (processedRequestRepository == null || requestId == null || requestId.isBlank()) {
            return null;
        }
        return processedRequestRepository.findByRequestId(requestId).orElse(null);
    }

    private void saveResponse(final String requestId, final BookingResponse response) {
        if (processedRequestRepository == null || requestId == null || requestId.isBlank()) {
            return;
        }
        processedRequestRepository.save(requestId, response);
    }
}
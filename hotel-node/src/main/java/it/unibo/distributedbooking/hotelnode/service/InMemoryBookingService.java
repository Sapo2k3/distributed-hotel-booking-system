package it.unibo.distributedbooking.hotelnode.service;

import it.unibo.distributedbooking.common.model.*;
import it.unibo.distributedbooking.common.service.BookingService;
import it.unibo.distributedbooking.hotelnode.repository.BookingRepository;
import it.unibo.distributedbooking.hotelnode.repository.InMemoryBookingRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookingService implements BookingService {

    private final BookingRepository bookingRepository;
    private final Map<String, BookingResponse> responsesByRequestId  = new ConcurrentHashMap<>();

    public InMemoryBookingService() {
        this.bookingRepository = new InMemoryBookingRepository();
    }

    public InMemoryBookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public BookingResponse createBooking(final BookingRequest request) {
        BookingResponse existingResponse = responsesByRequestId.get(request.requestId());
        if(existingResponse != null){
            return existingResponse;
        }
        boolean roomAlreadyBooked = bookingRepository.findAll().stream()
                .filter(booking -> booking.roomId().equals(request.roomId()))
                .filter(booking -> booking.hotelId().equals(request.hotelId()))
                .filter(booking -> booking.status() == BookingStatus.CONFIRMED)
                .anyMatch(booking ->
                        request.checkInDate().isBefore(booking.checkOutDate()) &&
                                request.checkOutDate().isAfter(booking.checkInDate())
                );
        BookingResponse response;
        if(roomAlreadyBooked) {
            response = new BookingResponse(
                    request.requestId(),
                    false,
                    "Room is not avaiable for the selected dates",
                    null
            );
        } else {
            Booking booking = new Booking(
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
        responsesByRequestId.put(request.requestId(), response);
        return response;
    }

    @Override
    public BookingResponse cancelBooking(final BookingCancellationRequest request) {
        BookingResponse existingResponse = responsesByRequestId.get(request.requestId());
        if (existingResponse != null) {
            return existingResponse;
        }

        BookingResponse response = bookingRepository.findById(request.bookingId())
                .map(booking -> {
                    Booking cancelledBooking = new Booking(
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
                        request.bookingId(),
                        false,
                        "Booking not found",
                        null
                ));

        responsesByRequestId.put(request.requestId(), response);
        return response;
    }

    @Override
    public BookingResponse modifyBooking(final BookingModificationRequest request) {
        BookingResponse existingResponse = responsesByRequestId.get(request.getRequestId());
        if(existingResponse != null){
            return existingResponse;
        }
        BookingResponse response = bookingRepository.findById(request.getBookingId())
                .map(existingBooking -> {
                    if (existingBooking.status() != BookingStatus.CONFIRMED) {
                        return new BookingResponse(
                                request.getRequestId(),
                                false,
                                "Only confirmed bookings can be modified",
                                null
                        );
                    }
                    boolean roomAlreadyBooked = bookingRepository.findAll().stream()
                            .filter(booking -> !booking.bookingId().equals(request.getBookingId()))
                            .filter(booking -> booking.roomId().equals(request.getRoomId()))
                            .filter(booking -> booking.hotelId().equals(request.getHotelId()))
                            .filter(booking -> booking.status() == BookingStatus.CONFIRMED)
                            .anyMatch(booking ->
                                    request.getCheckInDate().isBefore(booking.checkOutDate()) &&
                                            request.getCheckOutDate().isAfter(booking.checkInDate())
                            );
                    if (roomAlreadyBooked) {
                        return new BookingResponse(
                                request.getRequestId(),
                                false,
                                "Room is not available for the selected dates",
                                null
                        );
                    }
                    Booking modifiedBooking = new Booking(
                            existingBooking.bookingId(),
                            request.getHotelId(),
                            request.getRoomId(),
                            request.getCustomerId(),
                            request.getCheckInDate(),
                            request.getCheckOutDate(),
                            BookingStatus.MODIFIED
                    );
                    bookingRepository.update(modifiedBooking);
                    return new BookingResponse(
                            request.getRequestId(),
                            true,
                            "Booking modified successfully",
                            modifiedBooking
                    );
                })
                .orElseGet(() -> new BookingResponse(
                        request.getRequestId(),
                        false,
                        "Booking not found",
                        null
                ));
        responsesByRequestId.put(request.getRequestId(), response);
        return response;
    }
}

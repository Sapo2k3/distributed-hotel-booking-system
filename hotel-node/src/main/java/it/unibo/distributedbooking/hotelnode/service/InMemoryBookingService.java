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
        BookingResponse existingResponse = responsesByRequestId.get(request.getRequestId());
        if(existingResponse != null){
            return existingResponse;
        }
        boolean roomAlreadyBooked = bookingRepository.findAll().stream()
                .filter(booking -> booking.getRoomId().equals(request.getRoomId()))
                .filter(booking -> booking.getHotelId().equals(request.getHotelId()))
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .anyMatch(booking ->
                        request.getCheckInDate().isBefore(booking.getCheckOutDate()) &&
                                request.getCheckOutDate().isAfter(booking.getCheckInDate())
                );
        BookingResponse response;
        if(roomAlreadyBooked) {
            response = new BookingResponse(
                    request.getRequestId(),
                    false,
                    "Room is not avaiable for the selected dates",
                    null
            );
        } else {
            Booking booking = new Booking(
                    UUID.randomUUID().toString(),
                    request.getHotelId(),
                    request.getRoomId(),
                    request.getCustomerId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate(),
                    BookingStatus.CONFIRMED
            );
            bookingRepository.save(booking);
            response = new BookingResponse(
                    request.getRequestId(),
                    true,
                    "Booking created successfully",
                    booking
            );
        }
        responsesByRequestId.put(request.getRequestId(), response);
        return response;
    }

    @Override
    public BookingResponse cancelBooking(final BookingCancellationRequest request) {
        BookingResponse existingResponse = responsesByRequestId.get(request.getRequestId());
        if (existingResponse != null) {
            return existingResponse;
        }

        BookingResponse response = bookingRepository.findById(request.getBookingId())
                .map(booking -> {
                    Booking cancelledBooking = new Booking(
                            booking.getId(),
                            booking.getHotelId(),
                            booking.getRoomId(),
                            booking.getCustomerId(),
                            booking.getCheckInDate(),
                            booking.getCheckOutDate(),
                            BookingStatus.CANCELLED
                    );

                    bookingRepository.update(cancelledBooking);

                    return new BookingResponse(
                            request.getRequestId(),
                            true,
                            "Booking cancelled successfully",
                            cancelledBooking
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

    @Override
    public BookingResponse modifyBooking(final BookingModificationRequest request) {
        BookingResponse existingResponse = responsesByRequestId.get(request.getRequestId());
        if(existingResponse != null){
            return existingResponse;
        }
        BookingResponse response = bookingRepository.findById(request.getBookingId())
                .map(existingBooking -> {
                    if (existingBooking.getStatus() != BookingStatus.CONFIRMED) {
                        return new BookingResponse(
                                request.getRequestId(),
                                false,
                                "Only confirmed bookings can be modified",
                                null
                        );
                    }
                    boolean roomAlreadyBooked = bookingRepository.findAll().stream()
                            .filter(booking -> !booking.getId().equals(request.getBookingId()))
                            .filter(booking -> booking.getRoomId().equals(request.getRoomId()))
                            .filter(booking -> booking.getHotelId().equals(request.getHotelId()))
                            .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                            .anyMatch(booking ->
                                    request.getCheckInDate().isBefore(booking.getCheckOutDate()) &&
                                            request.getCheckOutDate().isAfter(booking.getCheckInDate())
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
                            existingBooking.getId(),
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

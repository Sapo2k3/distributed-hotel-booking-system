package it.unibo.distributedbooking.hotelnode.service;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.common.model.BookingStatus;
import it.unibo.distributedbooking.common.service.BookingService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookingService implements BookingService {

    private final Map<String, Booking> bookingsById = new ConcurrentHashMap<>();
    private final Map<String, BookingResponse> responsesByRequestId  = new ConcurrentHashMap<>();

    @Override
    public BookingResponse createBooking(final BookingRequest request) {
        BookingResponse existingResponse = responsesByRequestId.get(request.getRequestId());
        if(existingResponse != null){
            return existingResponse;
        }
        boolean roomAlreadyBooked = bookingsById.values().stream()
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
            bookingsById.put(booking.getId(), booking);
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
    public BookingResponse cancelBooking(final String requestId, final String bookingId, BookingRequest request) {
        BookingResponse existingResponse = responsesByRequestId.get(requestId);
        if (existingResponse != null){
            return existingResponse;
        }
        Booking booking = bookingsById.get(bookingId);
        BookingResponse response;
        if(booking == null){
            response = new BookingResponse(
                    requestId,
                    false,
                    "Booing not found",
                    null
            );
        } else {
            Booking cancelledBooking = new Booking(
                    booking.getId(),
                    booking.getHotelId(),
                    booking.getRoomId(),
                    booking.getCustomerId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    BookingStatus.CANCELLED
            );
            bookingsById.put(bookingId, cancelledBooking);
            response = new BookingResponse(
                    requestId,
                    true,
                    "Booking cancelled successfully",
                    cancelledBooking
            );
        }
        responsesByRequestId.put(requestId, response);;
        return response;
    }

    @Override
    public BookingResponse modifyBooking(String requestId, BookingRequest request) {
        return new BookingResponse(
                requestId,
                false,
                "Modify booking not implemented yet",
                null
        );
    }
}

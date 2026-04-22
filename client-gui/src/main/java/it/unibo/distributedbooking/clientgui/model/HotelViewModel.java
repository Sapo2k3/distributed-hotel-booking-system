package it.unibo.distributedbooking.clientgui.model;

public class HotelViewModel {

    private final String hotelId;
    private final String status;

    public HotelViewModel(final String hotelId, final String status) {
        this.hotelId = hotelId;
        this.status = status;
    }

    public String getHotelId() {
        return hotelId;
    }

    public String getStatus() {
        return status;
    }
}
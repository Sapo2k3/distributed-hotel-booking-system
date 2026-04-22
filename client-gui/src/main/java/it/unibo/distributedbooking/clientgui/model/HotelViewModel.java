package it.unibo.distributedbooking.clientgui.model;

public class HotelViewModel {

    private final String hotelId;
    private final String host;
    private final String port;
    private final String status;

    public HotelViewModel(final String hotelId,
                          final String host,
                          final String port,
                          final String status) {
        this.hotelId = hotelId;
        this.host = host;
        this.port = port;
        this.status = status;
    }

    public String getHotelId() {
        return hotelId;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getStatus() {
        return status;
    }
}
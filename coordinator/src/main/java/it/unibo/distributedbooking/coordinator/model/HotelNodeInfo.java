package it.unibo.distributedbooking.coordinator.model;

public class HotelNodeInfo {

    private final String hotelId;
    private final String host;
    private final int port;

    public HotelNodeInfo(final String hotelId, final String host, final int port) {
        this.hotelId = hotelId;
        this.host = host;
        this.port = port;
    }

    public String getHotelId() {
        return hotelId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}

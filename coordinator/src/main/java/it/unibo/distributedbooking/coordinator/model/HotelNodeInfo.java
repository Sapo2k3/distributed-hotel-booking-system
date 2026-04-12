package it.unibo.distributedbooking.coordinator.model;

public class HotelNodeInfo {

    private final String hotelId;
    private final String host;
    private final int port;
    private boolean healthy = true;

    public HotelNodeInfo(final String hotelId, final String host, final int port) {
        this.hotelId = hotelId;
        this.host = host;
        this.port = port;
    }

    public String getHotelId() {
        return this.hotelId;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public boolean isHealthy() {
        return this.healthy;
    }

    public void markUp() {
        this.healthy = true;
    }

    public boolean isUp() {
        return this.healthy;
    }

    public void markDown() {
        this.healthy = false;
    }
}

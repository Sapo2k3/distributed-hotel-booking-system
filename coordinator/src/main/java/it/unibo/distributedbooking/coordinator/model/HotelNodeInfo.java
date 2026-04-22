package it.unibo.distributedbooking.coordinator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HotelNodeInfo {

    private String hotelId;
    private String host;
    private int port;
    private boolean healthy = true;

    public HotelNodeInfo() {
    }

    public HotelNodeInfo(final String hotelId, final String host, final int port) {
        this.hotelId = hotelId;
        this.host = host;
        this.port = port;
    }

    public String getHotelId() {
        return this.hotelId;
    }

    public void setHotelId(final String hotelId) {
        this.hotelId = hotelId;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public boolean isHealthy() {
        return this.healthy;
    }

    public void setHealthy(final boolean healthy) {
        this.healthy = healthy;
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
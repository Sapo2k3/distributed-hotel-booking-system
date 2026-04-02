package it.unibo.distributedbooking.common.model;

import java.math.BigDecimal;

public class Room {

    private final String id;
    private final String hotelId;
    private final String name;
    private final int capacity;
    private final BigDecimal pricePerNight;

    public Room(final String id,
                final String hotelId,
                final String name,
                final int capacity,
                BigDecimal pricePerNight) {
        this.id = id;
        this.hotelId = hotelId;
        this.name = name;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
    }

    public String getId() {
        return this.id;
    }

    public String getHotelId(){
        return this.hotelId;
    }

    public String getName(){
        return this.name;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public BigDecimal getPricePerNight() {
        return this.pricePerNight;
    }
}

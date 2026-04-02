package it.unibo.distributedbooking.common.model;

public class Hotel {

    private final String id;
    private final String name;
    private final String city;
    private final String address;
    private final boolean isActive;

    public Hotel(final String id,
                 final String name,
                 final String city,
                 final String address) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.address = address;
        this.isActive = true;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getCity() {
        return this.city;
    }

    public String getAddress() {
        return this.address;
    }

    public boolean isActive() {
        return this.isActive;
    }
}

package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryHotelRegistryService implements HotelRegistryService{

    private final ConcurrentMap<String, HotelNodeInfo> hotelsById = new ConcurrentHashMap<>();

    @Override
    public void registerHotel(HotelNodeInfo hotelNodeInfo) {
        hotelsById.put(hotelNodeInfo.getHotelId(), hotelNodeInfo);
    }

    @Override
    public Optional<HotelNodeInfo> findHotelById(String hotelId) {
        return Optional.ofNullable(hotelsById.get(hotelId));
    }

    @Override
    public List<HotelNodeInfo> findAllHotels() {
        return new ArrayList<>(hotelsById.values());
    }
}

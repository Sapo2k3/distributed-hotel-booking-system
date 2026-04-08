package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;

import java.util.List;
import java.util.Optional;

public interface HotelRegistryService {

    void registerHotel(final HotelNodeInfo hotelNodeInfo);

    Optional<HotelNodeInfo> findHotelById(final String hotelId);

    List<HotelNodeInfo> findAllHotels();
}

package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;

import java.util.Comparator;
import java.util.Optional;

public class InMemoryReplicationTargetSelector implements ReplicationTargetSelector{

    private final HotelRegistryService hotelRegistryService;

    public InMemoryReplicationTargetSelector(final HotelRegistryService hotelRegistryService) {
        this.hotelRegistryService = hotelRegistryService;
    }

    @Override
    public Optional<HotelNodeInfo> findReplicaTarget(String primaryHotelId) {
        return hotelRegistryService.findAllHotels().stream()
                .filter(HotelNodeInfo::isUp)
                .filter(hotel -> !hotel.getHotelId().equals(primaryHotelId))
                .sorted(Comparator.comparing(HotelNodeInfo::getHotelId))
                .findFirst();
    }
}

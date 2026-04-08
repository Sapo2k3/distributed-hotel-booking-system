package it.unibo.distributedbooking.coordinator;

import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;
import it.unibo.distributedbooking.coordinator.service.HotelRegistryService;
import it.unibo.distributedbooking.coordinator.service.InMemoryHotelRegistryService;

public class CoordinatorApplication {

    public static void main(String[] args) {
        HotelRegistryService registryService = new InMemoryHotelRegistryService();

        registryService.registerHotel(new HotelNodeInfo("hotel-1", "localhost", 8081));
        registryService.registerHotel(new HotelNodeInfo("hotel-2", "localhost", 8082));

        registryService.findAllHotels().forEach(hotel ->
                System.out.println(hotel.getHotelId() + " -> " + hotel.getHost() + ":" + hotel.getPort())
        );
    }
}

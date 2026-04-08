package it.unibo.distributedbooking.coordinator;

import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;
import it.unibo.distributedbooking.coordinator.service.HotelRegistryService;
import it.unibo.distributedbooking.coordinator.service.BookingCoordinatorService;
import it.unibo.distributedbooking.coordinator.service.InMemoryHotelRegistryService;

import java.time.LocalDate;

public class CoordinatorApplication {

    public static void main(String[] args) {
        HotelRegistryService registryService = new InMemoryHotelRegistryService();

        registryService.registerHotel(new HotelNodeInfo("hotel-1", "localhost", 8081));
        registryService.registerHotel(new HotelNodeInfo("hotel-2", "localhost", 8082));

        BookingCoordinatorService coordinator = new BookingCoordinatorService(registryService);

        BookingRequest request = new BookingRequest(
                "req-demo-1",
                "hotel-1",
                "room-101",
                "customer-1",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12)
        );

        var response = coordinator.coordinateBooking(request);
        System.out.println("Coordinator response: " + response.isSuccess() + " - " + response.getMessage());

    }
}

package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;
import it.unibo.distributedbooking.coordinator.service.HotelRegistryService;
import it.unibo.distributedbooking.coordinator.service.InMemoryHotelRegistryService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryHotelRegistryServiceTest {

    @Test
    void shouldRegisterHotel() {
        HotelRegistryService registryService = new InMemoryHotelRegistryService();

        HotelNodeInfo hotelNodeInfo = new HotelNodeInfo("hotel-1", "localhost", 8081);

        registryService.registerHotel(hotelNodeInfo);

        Optional<HotelNodeInfo> result = registryService.findHotelById("hotel-1");

        assertTrue(result.isPresent());
        assertEquals("hotel-1", result.get().getHotelId());
        assertEquals("localhost", result.get().getHost());
        assertEquals(8081, result.get().getPort());
    }

    @Test
    void shouldFindAllRegisteredHotels() {
        HotelRegistryService registryService = new InMemoryHotelRegistryService();

        registryService.registerHotel(new HotelNodeInfo("hotel-1", "localhost", 8081));
        registryService.registerHotel(new HotelNodeInfo("hotel-2", "localhost", 8082));

        List<HotelNodeInfo> hotels = registryService.findAllHotels();

        assertEquals(2, hotels.size());
        assertEquals("hotel-1", hotels.get(0).getHotelId());
        assertEquals("hotel-2", hotels.get(1).getHotelId());
    }

    @Test
    void shouldReturnEmptyOptionalWhenHotelNotFound() {
        HotelRegistryService registryService = new InMemoryHotelRegistryService();

        Optional<HotelNodeInfo> result = registryService.findHotelById("hotel-999");

        assertFalse(result.isPresent());
    }

    @Test
    void shouldOverwriteExistingHotelRegistration() {
        HotelRegistryService registryService = new InMemoryHotelRegistryService();

        registryService.registerHotel(new HotelNodeInfo("hotel-1", "localhost", 8081));
        registryService.registerHotel(new HotelNodeInfo("hotel-1", "localhost", 8082));

        Optional<HotelNodeInfo> result = registryService.findHotelById("hotel-1");

        assertTrue(result.isPresent());
        assertEquals(8082, result.get().getPort());
    }
}

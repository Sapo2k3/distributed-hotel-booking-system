package it.unibo.distributedbooking.coordinator.heartbeat;

import it.unibo.distributedbooking.coordinator.client.HotelNodeClient;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;
import it.unibo.distributedbooking.coordinator.service.HotelRegistryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HeartbeatServiceTest {

    @Mock
    private HotelRegistryService hotelRegistryService;

    @Mock
    private HotelNodeClient hotelNodeClient;

    @Test
    void shouldMarkDownAndUpAccordingToHealthCheck() {
        HotelNodeInfo hotel1 = new HotelNodeInfo("hotel-1", "hotel-node-1", 8081);
        HotelNodeInfo hotel2 = new HotelNodeInfo("hotel-2", "hotel-node-2", 8082);
        when(hotelRegistryService.findAllHotels()).thenReturn(List.of(hotel1, hotel2));
        when(hotelNodeClient.isHealthy("http://hotel-node-1:8081")).thenReturn(false);
        when(hotelNodeClient.isHealthy("http://hotel-node-2:8082")).thenReturn(true);
        HeartbeatService heartbeatService = new HeartbeatService(hotelRegistryService, hotelNodeClient);
        heartbeatService.performHeartbeatCheckForTest();
        assertThat(hotel1.isUp()).isFalse();
        verify(hotelNodeClient).isHealthy("http://hotel-node-1:8081");
    }
}

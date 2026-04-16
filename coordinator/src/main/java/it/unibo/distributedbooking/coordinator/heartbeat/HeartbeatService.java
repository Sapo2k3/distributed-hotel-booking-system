package it.unibo.distributedbooking.coordinator.heartbeat;

import it.unibo.distributedbooking.coordinator.client.HotelNodeClient;
import it.unibo.distributedbooking.coordinator.service.HotelRegistryService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HeartbeatService {

    private static final int HEARTBEAT_INTERVAL_SECONDS = 10;

    private final HotelRegistryService hotelRegistryService;
    private final HotelNodeClient hotelNodeClient;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);



    public HeartbeatService(final HotelRegistryService hotelRegistryService,
                            final HotelNodeClient hotelNodeClient) {
        this.hotelRegistryService = hotelRegistryService;
        this.hotelNodeClient = hotelNodeClient;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(
                    this::performHeartbeatCheck,
                    0,
                    HEARTBEAT_INTERVAL_SECONDS,
                    TimeUnit.SECONDS
            );
            System.out.println("Heartbeat service started");
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            scheduler.shutdown();
            System.out.println("Heartbeat service stopped");
        }
    }

    private void performHeartbeatCheck() {
        hotelRegistryService.findAllHotels().forEach(hotelNode -> {
            String baseUrl = "http://" + hotelNode.getHost() + ":" + hotelNode.getPort();
            boolean healthy = hotelNodeClient.isHealthy(baseUrl);
            if (healthy){
                if (!hotelNode.isUp()) {
                    System.out.println("Hotel " + hotelNode.getHotelId() + " recovered");
                }
                hotelNode.markUp();
            } else {
                if (hotelNode.isUp()) {
                    System.out.println("Hotel " + hotelNode.getHotelId() + " marked down");
                }
                hotelNode.markDown();
            }
        });
    }
}

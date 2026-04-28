package it.unibo.distributedbooking.coordinator.service;

import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;

import java.util.Optional;

public interface ReplicationTargetSelector {

    Optional<HotelNodeInfo> findReplicaTarget(String primaryHotelId);
}

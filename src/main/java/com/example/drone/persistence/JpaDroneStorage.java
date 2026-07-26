package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaDroneStorage implements DroneStorage {

    private final DroneJpaRepository repository;

    public JpaDroneStorage(DroneJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByIdentifier(String identifier) {
        return repository.existsByIdentifier(identifier);
    }

    @Override
    public List<DroneEntity> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    public Optional<DroneEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<DroneEntity> findByStatus(DroneStatus status) {
        return repository.findByStatusOrderByIdAsc(status);
    }

    @Override
    public List<DroneEntity> findRechargeQueue() {
        return repository.findByStatusOrderByRechargeQueuedAtAscIdAsc(DroneStatus.CHARGING);
    }

    @Override
    public DroneEntity save(DroneEntity drone) {
        return repository.save(drone);
    }
}

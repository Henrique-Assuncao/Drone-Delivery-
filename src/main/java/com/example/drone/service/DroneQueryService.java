package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DroneQueryService {

    private final DroneStorage storage;

    public DroneQueryService(DroneStorage storage) {
        this.storage = storage;
    }

    public List<DroneEntity> findAll() {
        return storage.findAll();
    }

    public DroneEntity findById(Long id) {
        return storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("drone not found"));
    }

    public List<DroneEntity> findAvailable() {
        return storage.findByStatus(DroneStatus.AVAILABLE);
    }

    public List<DroneEntity> findByStatus(DroneStatus status) {
        return storage.findByStatus(status);
    }
}

package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripQueryService {

    private final TripStorage storage;

    public TripQueryService(TripStorage storage) {
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<TripEntity> findAll() {
        return storage.findAll();
    }

    @Transactional(readOnly = true)
    public List<TripEntity> findByStatus(TripStatus status) {
        return storage.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public TripEntity findById(Long id) {
        return storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("trip not found"));
    }
}

package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaTripStorage implements TripStorage {

    private final TripJpaRepository repository;

    public JpaTripStorage(TripJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TripEntity> findAll() {
        return repository.findAllByOrderByIdAsc();
    }

    @Override
    public List<TripEntity> findByStatus(TripStatus status) {
        return repository.findByStatusOrderByIdAsc(status);
    }

    @Override
    public Optional<TripEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public TripEntity save(TripEntity trip) {
        return repository.save(trip);
    }

    @Override
    public boolean existsByDroneId(Long droneId) {
        return repository.existsByDroneId(droneId);
    }
}

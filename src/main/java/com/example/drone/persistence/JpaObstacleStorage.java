package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaObstacleStorage implements ObstacleStorage {

    private final ObstacleJpaRepository repository;

    public JpaObstacleStorage(ObstacleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ObstacleEntity> findAll() {
        return repository.findAllByOrderByIdAsc();
    }

    @Override
    public List<ObstacleEntity> findActive() {
        return repository.findByActiveTrueOrderByIdAsc();
    }

    @Override
    public Optional<ObstacleEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public ObstacleEntity save(ObstacleEntity obstacle) {
        return repository.save(obstacle);
    }
}

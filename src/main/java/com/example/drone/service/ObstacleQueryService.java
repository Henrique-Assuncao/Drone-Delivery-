package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ObstacleQueryService {

    private final ObstacleStorage storage;

    public ObstacleQueryService(ObstacleStorage storage) {
        this.storage = storage;
    }

    public List<ObstacleEntity> findAll() {
        return storage.findAll();
    }

    public List<ObstacleEntity> findActive() {
        return storage.findActive();
    }
}

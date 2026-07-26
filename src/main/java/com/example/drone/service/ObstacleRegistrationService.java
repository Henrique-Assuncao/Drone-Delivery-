package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObstacleRegistrationService {

    private final ObstacleStorage storage;

    public ObstacleRegistrationService(ObstacleStorage storage) {
        this.storage = storage;
    }

    @Transactional
    public ObstacleEntity register(Coordinate center, double radius) {
        Obstacle obstacle = new Obstacle(center, radius);
        ObstacleEntity entity = new ObstacleEntity(
                null,
                obstacle.center().x(),
                obstacle.center().y(),
                obstacle.radius(),
                obstacle.active()
        );

        return storage.save(entity);
    }
}

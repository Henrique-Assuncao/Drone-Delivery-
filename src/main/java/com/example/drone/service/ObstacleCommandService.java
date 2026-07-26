package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObstacleCommandService {

    private final ObstacleStorage storage;

    public ObstacleCommandService(ObstacleStorage storage) {
        this.storage = storage;
    }

    @Transactional
    public ObstacleEntity deactivate(Long id) {
        ObstacleEntity obstacle = storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("obstacle not found"));

        obstacle.deactivate();

        return obstacle;
    }
}

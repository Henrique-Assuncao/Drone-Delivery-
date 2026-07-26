package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import java.util.List;
import java.util.Optional;

public interface ObstacleStorage {

    List<ObstacleEntity> findAll();

    List<ObstacleEntity> findActive();

    Optional<ObstacleEntity> findById(Long id);

    ObstacleEntity save(ObstacleEntity obstacle);
}

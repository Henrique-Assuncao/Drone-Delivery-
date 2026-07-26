package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ObstacleJpaRepository extends JpaRepository<ObstacleEntity, Long> {

    List<ObstacleEntity> findAllByOrderByIdAsc();

    List<ObstacleEntity> findByActiveTrueOrderByIdAsc();
}

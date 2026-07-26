package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface DroneJpaRepository extends JpaRepository<DroneEntity, Long> {

    boolean existsByIdentifier(String identifier);

    List<DroneEntity> findByStatusOrderByIdAsc(DroneStatus status);

    List<DroneEntity> findByStatusOrderByRechargeQueuedAtAscIdAsc(DroneStatus status);
}

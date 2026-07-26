package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface TripJpaRepository extends JpaRepository<TripEntity, Long> {

    @EntityGraph(attributePaths = {"drone", "tripOrders", "tripOrders.order"})
    List<TripEntity> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"drone", "tripOrders", "tripOrders.order"})
    List<TripEntity> findByStatusOrderByIdAsc(TripStatus status);

    @EntityGraph(attributePaths = {"drone", "tripOrders", "tripOrders.order"})
    Optional<TripEntity> findById(Long id);
}

package com.example.drone.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface MonthlyProductivityReportJpaRepository extends JpaRepository<MonthlyProductivityReportEntity, Long> {

    @EntityGraph(attributePaths = "droneProductivity")
    Optional<MonthlyProductivityReportEntity> findByMonthKey(String monthKey);

    @EntityGraph(attributePaths = "droneProductivity")
    List<MonthlyProductivityReportEntity> findAllByOrderByMonthKeyDesc();
}

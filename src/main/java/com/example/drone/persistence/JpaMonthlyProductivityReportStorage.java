package com.example.drone.persistence;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaMonthlyProductivityReportStorage {

    private final MonthlyProductivityReportJpaRepository repository;

    public JpaMonthlyProductivityReportStorage(MonthlyProductivityReportJpaRepository repository) {
        this.repository = repository;
    }

    public Optional<MonthlyProductivityReportEntity> findByMonthKey(String monthKey) {
        return repository.findByMonthKey(monthKey);
    }

    public List<MonthlyProductivityReportEntity> findAll() {
        return repository.findAllByOrderByMonthKeyDesc();
    }

    public MonthlyProductivityReportEntity save(MonthlyProductivityReportEntity report) {
        return repository.save(report);
    }
}

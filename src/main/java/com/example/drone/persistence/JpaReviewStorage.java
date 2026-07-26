package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaReviewStorage implements ReviewStorage {

    private final ReviewJpaRepository repository;

    public JpaReviewStorage(ReviewJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ReviewEntity> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    public Optional<ReviewEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public ReviewEntity save(ReviewEntity review) {
        return repository.save(review);
    }
}

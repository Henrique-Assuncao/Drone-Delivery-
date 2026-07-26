package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import java.util.List;
import java.util.Optional;

public interface ReviewStorage {

    List<ReviewEntity> findAll();

    Optional<ReviewEntity> findById(Long id);

    ReviewEntity save(ReviewEntity review);
}

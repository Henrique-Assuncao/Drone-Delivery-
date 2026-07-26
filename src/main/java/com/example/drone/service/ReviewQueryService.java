package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewQueryService {

    private final ReviewStorage storage;

    public ReviewQueryService(ReviewStorage storage) {
        this.storage = storage;
    }

    public List<ReviewEntity> findAll() {
        return storage.findAll();
    }

    public ReviewEntity findById(Long id) {
        return storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("review not found"));
    }
}

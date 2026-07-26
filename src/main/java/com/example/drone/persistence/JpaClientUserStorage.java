package com.example.drone.persistence;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaClientUserStorage implements ClientUserStorage {

    private final ClientUserJpaRepository repository;

    public JpaClientUserStorage(ClientUserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Optional<ClientUserEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<ClientUserEntity> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public ClientUserEntity save(ClientUserEntity user) {
        return repository.save(user);
    }
}

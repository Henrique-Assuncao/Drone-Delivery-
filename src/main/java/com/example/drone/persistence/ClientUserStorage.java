package com.example.drone.persistence;

import java.util.Optional;

public interface ClientUserStorage {

    boolean existsByEmail(String email);

    Optional<ClientUserEntity> findById(Long id);

    Optional<ClientUserEntity> findByEmail(String email);

    ClientUserEntity save(ClientUserEntity user);
}

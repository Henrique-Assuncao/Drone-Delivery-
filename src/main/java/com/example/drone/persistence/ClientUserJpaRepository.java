package com.example.drone.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface ClientUserJpaRepository extends JpaRepository<ClientUserEntity, Long> {

    boolean existsByEmail(String email);

    Optional<ClientUserEntity> findByEmail(String email);
}

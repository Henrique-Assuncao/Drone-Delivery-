package com.example.drone.service;

import com.example.drone.persistence.*;

public record ClientAuthentication(ClientUserEntity user, String token) {
}

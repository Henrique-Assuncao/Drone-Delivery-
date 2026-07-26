ALTER TABLE trips
    ADD COLUMN simulation_current_x DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    ADD COLUMN simulation_current_y DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    ADD COLUMN simulation_travelled_distance DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    ADD COLUMN simulation_updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE trips
    ADD CONSTRAINT trips_simulation_travelled_distance_non_negative
    CHECK (simulation_travelled_distance >= 0);

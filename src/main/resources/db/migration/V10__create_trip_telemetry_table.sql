CREATE TABLE trip_telemetry (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    battery_level DOUBLE PRECISION NOT NULL,
    reported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT trip_telemetry_battery_level_range CHECK (battery_level >= 0 AND battery_level <= 100)
);

CREATE INDEX trip_telemetry_trip_reported_at_idx ON trip_telemetry (trip_id, reported_at, id);

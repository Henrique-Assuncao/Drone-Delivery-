ALTER TABLE drones
    ADD COLUMN battery_level DOUBLE PRECISION NOT NULL DEFAULT 100.0,
    ADD COLUMN battery_consumption_per_distance_unit DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    ADD COLUMN minimum_return_battery DOUBLE PRECISION NOT NULL DEFAULT 20.0,
    ADD COLUMN speed DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    ADD COLUMN charging_rate DOUBLE PRECISION NOT NULL DEFAULT 10.0;

ALTER TABLE drones
    ADD CONSTRAINT drones_battery_level_between_0_and_100 CHECK (battery_level >= 0 AND battery_level <= 100),
    ADD CONSTRAINT drones_battery_consumption_positive CHECK (battery_consumption_per_distance_unit > 0),
    ADD CONSTRAINT drones_minimum_return_battery_between_0_and_100 CHECK (minimum_return_battery >= 0 AND minimum_return_battery <= 100),
    ADD CONSTRAINT drones_speed_positive CHECK (speed > 0),
    ADD CONSTRAINT drones_charging_rate_positive CHECK (charging_rate > 0);

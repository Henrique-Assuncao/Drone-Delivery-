UPDATE drones
SET speed = speed * 60
WHERE speed > 0;

ALTER TABLE drones
    ALTER COLUMN speed SET DEFAULT 60.0;

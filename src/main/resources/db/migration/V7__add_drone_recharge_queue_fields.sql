ALTER TABLE drones
    ADD COLUMN recharge_queued_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN recharge_reason VARCHAR(255);

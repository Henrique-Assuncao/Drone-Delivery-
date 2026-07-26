ALTER TABLE trips
    ADD COLUMN planned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN ended_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE;

UPDATE trips
SET started_at = planned_at
WHERE status IN ('IN_ROUTE', 'RETURNED_EARLY', 'COMPLETED');

UPDATE trips
SET ended_at = COALESCE(simulation_updated_at, planned_at)
WHERE status IN ('RETURNED_EARLY', 'COMPLETED');

UPDATE trips
SET cancelled_at = planned_at
WHERE status = 'CANCELLED';

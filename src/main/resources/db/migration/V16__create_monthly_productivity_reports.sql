CREATE TABLE monthly_productivity_reports (
    id BIGSERIAL PRIMARY KEY,
    month_key VARCHAR(7) NOT NULL UNIQUE,
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    order_entries INTEGER NOT NULL,
    orders_sent INTEGER NOT NULL,
    orders_delivered INTEGER NOT NULL,
    orders_cancelled INTEGER NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT monthly_productivity_counts_non_negative
        CHECK (
            order_entries >= 0
            AND orders_sent >= 0
            AND orders_delivered >= 0
            AND orders_cancelled >= 0
        )
);

CREATE TABLE monthly_drone_productivity_reports (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES monthly_productivity_reports(id) ON DELETE CASCADE,
    drone_id BIGINT NOT NULL,
    drone_identifier VARCHAR(255) NOT NULL,
    orders_delivered INTEGER NOT NULL,
    trips_started INTEGER NOT NULL,
    trips_completed INTEGER NOT NULL,
    trips_cancelled INTEGER NOT NULL,
    trips_returned_early INTEGER NOT NULL,
    CONSTRAINT monthly_drone_productivity_counts_non_negative
        CHECK (
            orders_delivered >= 0
            AND trips_started >= 0
            AND trips_completed >= 0
            AND trips_cancelled >= 0
            AND trips_returned_early >= 0
        )
);

CREATE INDEX idx_monthly_drone_productivity_report_id
    ON monthly_drone_productivity_reports(report_id);

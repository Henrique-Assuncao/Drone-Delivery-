CREATE TABLE client_users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE orders
    ADD COLUMN client_user_id BIGINT;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_client_user
        FOREIGN KEY (client_user_id)
        REFERENCES client_users (id);

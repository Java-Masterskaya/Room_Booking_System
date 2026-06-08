CREATE TABLE outbox_event (
id BIGSERIAL PRIMARY KEY,
aggregate_type VARCHAR(255) NOT NULL,
aggregate_id VARCHAR(255) NOT NULL,
event_type VARCHAR(255) NOT NULL,
payload JSONB NOT NULL,
status VARCHAR(50) NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_status_created ON outbox_event(status, created_at);
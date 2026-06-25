CREATE TABLE IF NOT EXISTS rooms
(
    id BIGSERIAL PRIMARY KEY,
    name      VARCHAR(255) NOT NULL UNIQUE,
    capacity  INTEGER      NOT NULL CHECK (capacity > 0),
    equipment TEXT[] DEFAULT '{}'
);

-- Индекс для поиска комнаты по названию
CREATE INDEX IF NOT EXISTS idx_rooms_name ON rooms(name);
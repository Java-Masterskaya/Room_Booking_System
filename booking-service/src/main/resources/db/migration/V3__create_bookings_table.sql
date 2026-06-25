CREATE TABLE IF NOT EXISTS bookings
(
    id BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    room_id    BIGINT    NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time   TIMESTAMP NOT NULL,

    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT check_booking_time CHECK (end_time > start_time)
);

-- Основные индексы для поиска по ID пользователя и ID комнаты
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_room_id ON bookings(room_id);

-- Индекс для поиска броней на конкретную дату
CREATE INDEX IF NOT EXISTS idx_bookings_date ON bookings(DATE (start_time));
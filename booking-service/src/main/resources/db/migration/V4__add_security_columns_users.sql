-- Для пользователей добавляем поле для хранения хеша пароля и поле для хранения роли
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR (255) NOT NULL,
    ADD COLUMN IF NOT EXISTS role VARCHAR (20) NOT NULL DEFAULT 'USER';
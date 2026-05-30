CREATE TABLE IF NOT EXISTS users (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username   VARCHAR(50) UNIQUE NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO users (username, password, role) VALUES
    ('admin', '$2a$10$U.aQ1Q28tY.w5g1aV7h5OubrTpH.9lBOnfXh5aX.2R3EwD/N21pDq', 'ADMIN')
ON CONFLICT (username) DO NOTHING;
-- Create tables if not exist (Hibernate would normally handle this, but including for completeness)
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    profile_image VARCHAR(255),
    phone_number VARCHAR(20),
    is_enabled BOOLEAN DEFAULT TRUE,
    is_logged_in BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Insert roles if they don't exist
INSERT INTO roles (name, created_at)
SELECT 'ADMIN', NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

INSERT INTO roles (name, created_at)
SELECT 'LANDLORD', NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'LANDLORD');

INSERT INTO roles (name, created_at)
SELECT 'TENANT', NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'TENANT'); 
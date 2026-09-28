-- One user has exactly one profile: profiles.id is both the primary key
-- and the foreign key to users.id (shared primary key).
CREATE TABLE profiles (
    id              BIGINT       NOT NULL,
    bio             TEXT,
    phone_number    VARCHAR(15),
    date_of_birth   DATE,
    loyality_points INT UNSIGNED DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (id) REFERENCES users (id)
);

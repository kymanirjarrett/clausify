-- Accounts are identified by email (FR-1), so emails must be unique. The lab's V1 had no constraint.
-- The default collation (utf8mb4_0900_ai_ci) is case-insensitive, so Maria@x.com and maria@x.com collide;
-- the app also stores emails lowercased.
-- Existing rows get the migration time as created_at.
ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email),
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

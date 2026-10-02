-- An uploaded contract (FR-3). The PDF itself is never stored, only its extracted text (ADR 0005).
-- Deleting a user deletes their contracts; later tables (analyses) cascade from here.
CREATE TABLE contracts (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size_bytes   BIGINT       NOT NULL,
    page_count        INT          NOT NULL,
    extracted_text    MEDIUMTEXT   NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    failure_reason    VARCHAR(500),
    uploaded_at       DATETIME(6)  NOT NULL,
    analyzed_at       DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_contracts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- "My contracts, newest first" (FR-4) reads this index instead of sorting the table.
    INDEX idx_contracts_user_uploaded (user_id, uploaded_at)
);

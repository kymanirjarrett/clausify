-- AI analysis results (FR-5, FR-6). One analysis per contract; many findings per analysis.
-- Deleting a contract deletes its analysis, and deleting an analysis deletes its findings (FR-8).
CREATE TABLE analyses (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    contract_id    BIGINT       NOT NULL,
    overall_score  INT          NOT NULL,
    risk_level     VARCHAR(20)  NOT NULL,
    contract_type  VARCHAR(100),
    summary        TEXT,
    model_name     VARCHAR(100) NOT NULL,
    duration_ms    BIGINT       NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    -- UNIQUE makes the relationship one-to-one and doubles as the index for "analysis of contract X".
    CONSTRAINT uk_analyses_contract UNIQUE (contract_id),
    CONSTRAINT fk_analyses_contract FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
);

CREATE TABLE clause_findings (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    analysis_id        BIGINT       NOT NULL,
    category           VARCHAR(30)  NOT NULL,
    -- The model's 0 to 100 score for this clause; the overall score is computed from these in Java.
    risk_score         INT          NOT NULL,
    risk_level         VARCHAR(20)  NOT NULL,
    favorable          BOOLEAN      NOT NULL,
    title              VARCHAR(255) NOT NULL,
    section_ref        VARCHAR(100),
    original_text      TEXT         NOT NULL,
    explanation        TEXT         NOT NULL,
    suggested_revision TEXT,
    -- all-MiniLM-L6-v2 vector (384 floats) for FR-9 similarity; filled in later, so nullable (ADR 0003).
    embedding          JSON,
    PRIMARY KEY (id),
    CONSTRAINT fk_clause_findings_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE,
    -- FR-9 compares findings with standard clauses in the same category.
    INDEX idx_clause_findings_category (category)
);

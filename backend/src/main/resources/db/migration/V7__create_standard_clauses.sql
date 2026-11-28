-- FR-9: the standard clause library that findings are compared against. Shared reference data, not owned by a user.
-- Rows are synced at startup from src/main/resources/standard-clauses.json (StandardClauseLoader), so the team can
-- edit clause wording without a new migration. embedding is a 384-number JSON array (ADR 0003); embedding_model
-- records which model produced it, so a model change re-embeds every clause.
CREATE TABLE standard_clauses (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    clause_key      VARCHAR(100) NOT NULL,
    category        VARCHAR(30)  NOT NULL,
    title           VARCHAR(255) NOT NULL,
    text            TEXT         NOT NULL,
    favorable       TINYINT(1)   NOT NULL,
    notes           TEXT,
    embedding       JSON,
    embedding_model VARCHAR(100),
    PRIMARY KEY (id),
    UNIQUE KEY uk_standard_clauses_key (clause_key),
    KEY idx_standard_clauses_category (category)
);

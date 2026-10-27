CREATE TABLE analyses (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          contract_id BIGINT NOT NULL UNIQUE,
                          overall_score INT NULL,
                          overall_risk_level VARCHAR(50) NULL,
                          contract_type VARCHAR(100) NULL,
                          summary TEXT NULL,
                          model_name VARCHAR(100) NULL,
                          duration_ms BIGINT NULL,
                          created_at DATETIME(6) NOT NULL,
                          updated_at DATETIME(6) NOT NULL,
                          CONSTRAINT fk_analyses_contract_id FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE clause_findings (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 analysis_id BIGINT NOT NULL,
                                 title VARCHAR(255) NOT NULL,
                                 section_ref VARCHAR(100) NULL,
                                 risk_category VARCHAR(50) NOT NULL,
                                 risk_level VARCHAR(50) NOT NULL,
                                 favorable TINYINT(1) NOT NULL DEFAULT 0,
                                 description TEXT NULL,
                                 suggested_remediation TEXT NULL,
                                 embedding JSON NULL,
                                 created_at DATETIME(6) NOT NULL,
                                 updated_at DATETIME(6) NOT NULL,
                                 CONSTRAINT fk_clause_findings_analysis_id FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

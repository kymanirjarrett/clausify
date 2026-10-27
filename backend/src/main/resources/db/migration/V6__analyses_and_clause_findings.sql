-- Create the main analyses tracking table
CREATE TABLE analyses (
                          id BINARY(16) NOT NULL,
                          contract_id BINARY(16) NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          overall_score INT NULL,
                          overall_risk_level VARCHAR(50) NULL,
                          error_reason TEXT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create the individual clause findings table
CREATE TABLE clause_findings (
                                 id BINARY(16) NOT NULL,
                                 analysis_id BINARY(16) NOT NULL,
                                 category VARCHAR(100) NOT NULL,
                                 risk_score INT NOT NULL,
                                 risk_level VARCHAR(50) NOT NULL,
                                 original_text TEXT NOT NULL,
                                 explanation TEXT NOT NULL,
                                 suggested_revision TEXT NULL,
                                 is_favorable TINBYINT(1) NOT NULL DEFAULT 0,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (id),
                                 CONSTRAINT fk_findings_analysis_id FOREIGN KEY (analysis_id)
                                     REFERENCES analyses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add index on analysis_id for rapid retrieval when displaying the freelancer dashboard
CREATE INDEX idx_clause_findings_analysis ON clause_findings(analysis_id);

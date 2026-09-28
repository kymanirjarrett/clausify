-- ============================================
-- Clausify Database Schema
-- ============================================
-- Run this in Supabase SQL Editor
-- This will create all necessary tables and indexes

-- Enable pgvector extension for semantic search
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================
-- CONTRACTS TABLE
-- Stores uploaded contract metadata and analysis status
-- ============================================
CREATE TABLE IF NOT EXISTS contracts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  title TEXT NOT NULL,
  file_path TEXT NOT NULL, -- Path in Supabase Storage
  file_size INTEGER NOT NULL,
  contract_type TEXT, -- 'NDA', 'Service Agreement', 'Employment Contract', etc.
  upload_date TIMESTAMPTZ DEFAULT NOW(),
  analysis_status TEXT DEFAULT 'pending', -- 'pending', 'processing', 'completed', 'failed'
  analysis_data JSONB, -- Full analysis results stored as JSON
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Add index for faster user queries
CREATE INDEX IF NOT EXISTS contracts_user_id_idx ON contracts(user_id);
CREATE INDEX IF NOT EXISTS contracts_status_idx ON contracts(analysis_status);
CREATE INDEX IF NOT EXISTS contracts_created_at_idx ON contracts(created_at DESC);

-- ============================================
-- CLAUSES TABLE
-- Stores individual clauses extracted from contracts
-- ============================================
CREATE TABLE IF NOT EXISTS clauses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  contract_id UUID REFERENCES contracts(id) ON DELETE CASCADE NOT NULL,
  clause_text TEXT NOT NULL,
  clause_type TEXT NOT NULL, -- 'termination', 'payment', 'liability', etc.
  risk_level TEXT NOT NULL, -- 'high', 'medium', 'low'
  risk_score DECIMAL(5,2) CHECK (risk_score >= 0 AND risk_score <= 100),
  explanation TEXT NOT NULL,
  suggestions TEXT[], -- Array of suggested improvements
  position_in_doc INTEGER, -- Paragraph/section number
  embedding vector(1536), -- Vector embedding for semantic search
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Add indexes for common queries
CREATE INDEX IF NOT EXISTS clauses_contract_id_idx ON clauses(contract_id);
CREATE INDEX IF NOT EXISTS clauses_risk_level_idx ON clauses(risk_level);
CREATE INDEX IF NOT EXISTS clauses_clause_type_idx ON clauses(clause_type);

-- Create vector similarity search index (IVFFlat)
-- This enables fast nearest neighbor search for clause comparison
CREATE INDEX IF NOT EXISTS clauses_embedding_idx ON clauses 
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- ============================================
-- STANDARD_CLAUSES TABLE
-- Library of industry-standard clauses for comparison
-- ============================================
CREATE TABLE IF NOT EXISTS standard_clauses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  clause_text TEXT NOT NULL,
  clause_type TEXT NOT NULL,
  contract_type TEXT NOT NULL, -- Which type of contract this applies to
  is_favorable BOOLEAN DEFAULT true, -- Is this favorable to freelancers?
  industry TEXT, -- 'general', 'tech', 'creative', etc.
  explanation TEXT NOT NULL, -- Why this is standard/favorable
  embedding vector(1536), -- Vector embedding for semantic search
  usage_count INTEGER DEFAULT 0, -- Track how often it's referenced
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Add indexes for standard clause queries
CREATE INDEX IF NOT EXISTS standard_clauses_type_idx ON standard_clauses(clause_type);
CREATE INDEX IF NOT EXISTS standard_clauses_contract_type_idx ON standard_clauses(contract_type);
CREATE INDEX IF NOT EXISTS standard_clauses_favorable_idx ON standard_clauses(is_favorable);

-- Create vector similarity search index
CREATE INDEX IF NOT EXISTS standard_clauses_embedding_idx ON standard_clauses 
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- ============================================
-- USER_PREFERENCES TABLE (Optional - for future features)
-- Store user settings and preferences
-- ============================================
CREATE TABLE IF NOT EXISTS user_preferences (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL UNIQUE,
  default_contract_type TEXT,
  notification_enabled BOOLEAN DEFAULT true,
  theme TEXT DEFAULT 'light', -- 'light' or 'dark'
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS user_preferences_user_id_idx ON user_preferences(user_id);

-- ============================================
-- FUNCTIONS
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Add trigger to contracts table
DROP TRIGGER IF EXISTS update_contracts_updated_at ON contracts;
CREATE TRIGGER update_contracts_updated_at
    BEFORE UPDATE ON contracts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add trigger to user_preferences table
DROP TRIGGER IF EXISTS update_user_preferences_updated_at ON user_preferences;
CREATE TRIGGER update_user_preferences_updated_at
    BEFORE UPDATE ON user_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- ROW LEVEL SECURITY (RLS)
-- Ensure users can only access their own data
-- ============================================

-- Enable RLS on all tables
ALTER TABLE contracts ENABLE ROW LEVEL SECURITY;
ALTER TABLE clauses ENABLE ROW LEVEL SECURITY;
ALTER TABLE standard_clauses ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;

-- Contracts policies
CREATE POLICY "Users can view their own contracts"
  ON contracts FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own contracts"
  ON contracts FOR INSERT
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own contracts"
  ON contracts FOR UPDATE
  USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own contracts"
  ON contracts FOR DELETE
  USING (auth.uid() = user_id);

-- Clauses policies (access through contracts)
CREATE POLICY "Users can view clauses from their contracts"
  ON clauses FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM contracts
      WHERE contracts.id = clauses.contract_id
      AND contracts.user_id = auth.uid()
    )
  );

CREATE POLICY "Users can insert clauses to their contracts"
  ON clauses FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM contracts
      WHERE contracts.id = contract_id
      AND contracts.user_id = auth.uid()
    )
  );

-- Standard clauses are readable by everyone
CREATE POLICY "Standard clauses are viewable by all authenticated users"
  ON standard_clauses FOR SELECT
  TO authenticated
  USING (true);

-- User preferences policies
CREATE POLICY "Users can view their own preferences"
  ON user_preferences FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own preferences"
  ON user_preferences FOR INSERT
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own preferences"
  ON user_preferences FOR UPDATE
  USING (auth.uid() = user_id);

-- ============================================
-- STORAGE BUCKET
-- Create bucket for contract files
-- ============================================

-- Note: Run this in Supabase Dashboard > Storage, not SQL Editor
-- Or use Supabase API/Dashboard to create bucket named 'contracts'
-- Bucket settings:
-- - Name: contracts
-- - Public: false
-- - File size limit: 10MB
-- - Allowed MIME types: application/pdf

-- ============================================
-- SEED DATA - Sample Standard Clauses
-- ============================================

-- Insert some basic standard clauses for testing
INSERT INTO standard_clauses (clause_text, clause_type, contract_type, is_favorable, industry, explanation) VALUES
  (
    'Either party may terminate this agreement with 30 days written notice.',
    'termination',
    'Service Agreement',
    true,
    'general',
    'This is a standard and fair termination clause that allows both parties reasonable time to end the relationship.'
  ),
  (
    'Payment is due within 15 days of invoice date.',
    'payment',
    'Freelance Agreement',
    true,
    'general',
    'Standard payment terms that protect the freelancer with a reasonable payment window.'
  ),
  (
    'The contractor retains all intellectual property rights unless explicitly transferred in writing.',
    'ip_rights',
    'Freelance Agreement',
    true,
    'creative',
    'This protects the freelancer by ensuring they retain IP rights unless specifically agreed otherwise.'
  ),
  (
    'Liability is limited to the amount paid under this agreement.',
    'liability',
    'Service Agreement',
    true,
    'general',
    'Standard limitation of liability that caps exposure to the contract value.'
  ),
  (
    'Confidential information must be kept private for 2 years after contract termination.',
    'confidentiality',
    'NDA',
    true,
    'general',
    'Reasonable confidentiality period that balances both parties needs.'
  ),
  (
    'Non-compete restrictions apply only within 50 miles for 6 months.',
    'non_compete',
    'Employment Contract',
    true,
    'general',
    'Reasonable geographic and time limitations on non-compete that are typically enforceable.'
  ),
  (
    'Disputes shall be resolved through arbitration in accordance with AAA rules.',
    'jurisdiction',
    'Service Agreement',
    true,
    'general',
    'Standard arbitration clause that often provides faster, less expensive dispute resolution.'
  ),
  (
    'Client may terminate immediately without cause or payment.',
    'termination',
    'Service Agreement',
    false,
    'general',
    'Unfavorable clause that allows client to end agreement without notice or compensation.'
  ),
  (
    'Contractor is liable for any and all damages arising from the work.',
    'liability',
    'Freelance Agreement',
    false,
    'general',
    'Unlimited liability clause that exposes the contractor to excessive risk.'
  ),
  (
    'All work product is automatically work-for-hire and owned by the client.',
    'ip_rights',
    'Freelance Agreement',
    false,
    'creative',
    'Broad IP transfer that may undervalue the contractor work and limit portfolio use.'
  )
ON CONFLICT DO NOTHING;

-- ============================================
-- HELPER VIEWS
-- ============================================

-- View to get contract summaries with clause counts
CREATE OR REPLACE VIEW contract_summaries AS
SELECT 
  c.id,
  c.user_id,
  c.title,
  c.contract_type,
  c.analysis_status,
  c.upload_date,
  c.created_at,
  (c.analysis_data->>'overall_risk')::TEXT as overall_risk,
  (c.analysis_data->>'risk_score')::NUMERIC as risk_score,
  COUNT(cl.id) as clause_count,
  COUNT(cl.id) FILTER (WHERE cl.risk_level = 'high') as high_risk_count,
  COUNT(cl.id) FILTER (WHERE cl.risk_level = 'medium') as medium_risk_count,
  COUNT(cl.id) FILTER (WHERE cl.risk_level = 'low') as low_risk_count
FROM contracts c
LEFT JOIN clauses cl ON c.id = cl.contract_id
GROUP BY c.id;

-- ============================================
-- COMPLETION MESSAGE
-- ============================================
-- Schema created successfully!
-- Next steps:
-- 1. Create storage bucket named 'contracts' in Supabase Dashboard
-- 2. Set up storage policies for the bucket
-- 3. Configure RLS policies as needed for your use case
-- 4. Optionally: Generate embeddings for the seeded standard clauses
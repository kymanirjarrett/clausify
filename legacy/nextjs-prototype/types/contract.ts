export type ContractType = 
  | 'NDA'
  | 'Service Agreement'
  | 'Employment Contract'
  | 'Freelance Agreement'
  | 'Other'

export type RiskLevel = 'high' | 'medium' | 'low'

export type ClauseType =
  | 'termination'
  | 'payment'
  | 'liability'
  | 'ip_rights'
  | 'confidentiality'
  | 'non_compete'
  | 'jurisdiction'
  | 'other'

export interface Contract {
  id: string
  user_id: string
  title: string
  file_path: string
  file_size: number
  contract_type: ContractType | null
  upload_date: string
  analysis_status: 'pending' | 'processing' | 'completed' | 'failed'
  analysis_data: ContractAnalysis | null
  created_at: string
}

export interface Clause {
  id: string
  contract_id: string
  clause_text: string
  clause_type: ClauseType
  risk_level: RiskLevel
  risk_score: number
  explanation: string
  suggestions: string[]
  position_in_doc: number
  created_at: string
}

export interface ContractAnalysis {
  contract_type: ContractType
  overall_risk: RiskLevel
  risk_score: number
  summary: string
  flagged_clauses: FlaggedClause[]
  positive_points: string[]
  negotiation_priorities: string[]
}

export interface FlaggedClause {
  clause_text: string
  clause_type: ClauseType
  risk_level: RiskLevel
  explanation: string
  suggestion: string
  position: number
}
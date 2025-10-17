import type { ContractType, ContractAnalysis, ClauseType } from './contract'

export interface AnalysisRequest {
  contractText: string
  contractType?: ContractType
  fileName: string
}

export interface AnalysisResponse {
  success: boolean
  analysis?: ContractAnalysis
  error?: string
}

export interface EmbeddingRequest {
  text: string
}

export interface EmbeddingResponse {
  embedding: number[]
}

export interface SimilarClause {
  id: string
  clause_text: string
  clause_type: ClauseType
  is_favorable: boolean
  explanation: string
  similarity: number
}
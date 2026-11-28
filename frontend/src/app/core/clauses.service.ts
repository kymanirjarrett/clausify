import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RiskCategory, SimilarClauses, StandardClause } from './api.models';

/** FR-9: the standard clause library and similar-clause lookup. */
@Injectable({ providedIn: 'root' })
export class ClausesService {
  private readonly http = inject(HttpClient);

  similar(findingId: number): Observable<SimilarClauses> {
    return this.http.get<SimilarClauses>(`/api/clauses/${findingId}/similar`);
  }

  library(category?: RiskCategory): Observable<StandardClause[]> {
    return this.http.get<StandardClause[]>('/api/standard-clauses', category ? { params: { category } } : {});
  }
}

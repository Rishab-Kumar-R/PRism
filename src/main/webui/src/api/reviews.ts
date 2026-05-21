import { api } from './client'
import type { ReviewRecord, ReviewStats, ReviewSummary } from './client'

export const reviewsApi = {
  list: (page = 0, size = 20) =>
    api.get<ReviewSummary[]>(`/reviews?page=${page}&size=${size}`),

  byId: (id: number) =>
    api.get<ReviewRecord>(`/reviews/${id}`),

  byRepo: (repoName: string, page = 0, size = 20) =>
    api.get<ReviewSummary[]>(`/reviews/repo/${encodeURIComponent(repoName)}?page=${page}&size=${size}`),

  stats: () =>
    api.get<ReviewStats>('/reviews/stats'),
}

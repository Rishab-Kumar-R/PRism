const API_KEY = import.meta.env.VITE_API_KEY ?? ''

async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    ...options,
    headers: {
      'API-Key': API_KEY,
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })
  if (!res.ok) {
    throw new Error(`${res.status} ${res.statusText}`)
  }
  return res.json() as Promise<T>
}

export const api = {
  get: <T>(path: string) => apiFetch<T>(path),
  patch: <T>(path: string, body: unknown) =>
    apiFetch<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),
}

export interface ReviewSummary {
  id: number
  repoName: string
  prNumber: number
  prTitle: string
  commitSha: string
  severity: string
  score: number
  bugCount: number
  securityCount: number
  performanceCount: number
  codeQualityCount: number
  recommendation: string
  reviewedAt: string
}

export interface ReviewRecord extends ReviewSummary {
  reviewComment: string
  githubReviewId: number | null
  issueCommentId: number | null
}

export interface ReviewStats {
  totalReviews: number
  approved: number
  needsWork: number
  approvalRate: string
  averageScore: number
  totalBugs: number
  totalSecurityIssues: number
  totalPerformanceIssues: number
  mostCommonIssue: string
  mostReviewedRepo: string
}

export interface InstallationSummary {
  installationId: number
  accountLogin: string
  tier: 'FREE' | 'PRO' | 'ENTERPRISE'
  active: boolean
  customMonthlyReviewLimit: number | null
  customDailyReviewLimit: number | null
}

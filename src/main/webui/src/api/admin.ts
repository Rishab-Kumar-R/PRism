import { api } from './client'
import type { InstallationSummary } from './client'

export const adminApi = {
  listInstallations: (active?: boolean) => {
    const query = active !== undefined ? `?active=${active}` : ''
    return api.get<InstallationSummary[]>(`/admin/installations${query}`)
  },

  updateTier: (installationId: number, tier: string) =>
    api.patch<InstallationSummary>(`/admin/installations/${installationId}/tier`, { tier }),

  updateLimits: (installationId: number, customMonthlyReviewLimit: number | null, customDailyReviewLimit: number | null) =>
    api.patch<InstallationSummary>(`/admin/installations/${installationId}/limits`, {
      customMonthlyReviewLimit,
      customDailyReviewLimit,
    }),
}

import { useQuery } from '@tanstack/react-query'
import { reviewsApi } from '../api/reviews'
import { Badge, severityVariant, severityLabel } from '../components/Badge'
import type { ReviewSummary, ReviewStats } from '../api/client'
import { TrendingUp, GitPullRequest, CheckCircle, BarChart2 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

function StatCard({ label, value, icon: Icon, sub }: {
  label: string
  value: string
  icon: React.ElementType
  sub?: string
}) {
  return (
    <div style={{ background: '#18181b', border: '1px solid #3f3f46', borderRadius: 12, padding: '20px 24px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <span style={{ fontSize: 11, color: '#71717a', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 500 }}>{label}</span>
        <Icon size={14} color="#52525b" />
      </div>
      <p style={{ fontSize: 28, fontWeight: 600, color: '#f4f4f5', letterSpacing: '-0.5px', margin: 0 }}>{value}</p>
      {sub && <p style={{ fontSize: 12, color: '#52525b', marginTop: 4, marginBottom: 0 }}>{sub}</p>}
    </div>
  )
}

function timeAgo(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  return `${Math.floor(hrs / 24)}d ago`
}

export function Dashboard() {
  const navigate = useNavigate()

  const { data: stats, isLoading: statsLoading } = useQuery<ReviewStats>({
    queryKey: ['stats'],
    queryFn: reviewsApi.stats,
  })

  const { data: recent, isLoading: recentLoading } = useQuery<ReviewSummary[]>({
    queryKey: ['reviews', 0, 8],
    queryFn: () => reviewsApi.list(0, 8),
  })

  return (
    <div style={{ padding: '40px 40px', boxSizing: 'border-box', width: '100%' }}>
      {/* Header */}
      <div style={{ marginBottom: 32 }}>
        <h1 style={{ fontSize: 20, fontWeight: 600, color: '#f4f4f5', margin: 0, letterSpacing: '-0.3px' }}>Overview</h1>
        <p style={{ fontSize: 13, color: '#71717a', margin: '4px 0 0' }}>AI-powered PR review activity at a glance</p>
      </div>

      {/* Stat cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 32 }}>
        {statsLoading ? (
          Array.from({ length: 4 }).map((_, i) => (
            <div key={i} style={{ background: '#18181b', border: '1px solid #3f3f46', borderRadius: 12, height: 96 }} />
          ))
        ) : (
          <>
            <StatCard label="Total Reviews" value={stats?.totalReviews?.toLocaleString() ?? '—'} icon={GitPullRequest} />
            <StatCard label="Approval Rate" value={stats?.approvalRate ?? '—'} icon={CheckCircle} />
            <StatCard label="Avg Score" value={stats?.averageScore != null ? `${stats.averageScore}` : '—'} icon={BarChart2} sub="out of 10" />
            <StatCard label="Top Issue" value={stats?.mostCommonIssue ?? '—'} icon={TrendingUp} />
          </>
        )}
      </div>

      {/* Recent Reviews */}
      <div style={{ background: '#18181b', border: '1px solid #3f3f46', borderRadius: 12, overflow: 'hidden' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 20px', borderBottom: '1px solid #27272a' }}>
          <span style={{ fontSize: 13, fontWeight: 500, color: '#e4e4e7' }}>Recent Reviews</span>
          <button
            onClick={() => navigate('/reviews')}
            style={{ fontSize: 12, color: '#71717a', background: 'none', border: 'none', cursor: 'pointer' }}
            onMouseEnter={e => (e.currentTarget.style.color = '#d4d4d8')}
            onMouseLeave={e => (e.currentTarget.style.color = '#71717a')}
          >
            View all →
          </button>
        </div>

        {recentLoading ? (
          Array.from({ length: 5 }).map((_, i) => (
            <div key={i} style={{ padding: '14px 20px', borderBottom: '1px solid #27272a', display: 'flex', gap: 12 }}>
              <div style={{ height: 14, background: '#27272a', borderRadius: 4, width: 160 }} />
            </div>
          ))
        ) : !recent?.length ? (
          <div style={{ padding: '48px 20px', textAlign: 'center', fontSize: 13, color: '#52525b' }}>No reviews yet</div>
        ) : (
          recent.map((r, idx) => (
            <div
              key={r.id}
              onClick={() => navigate(`/reviews/${r.id}`)}
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr auto auto auto auto',
                alignItems: 'center',
                gap: 16,
                padding: '12px 20px',
                borderBottom: idx < recent.length - 1 ? '1px solid #27272a' : 'none',
                cursor: 'pointer',
              }}
              onMouseEnter={e => (e.currentTarget.style.background = 'rgba(39,39,42,0.5)')}
              onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
            >
              <div style={{ minWidth: 0 }}>
                <p style={{ fontSize: 13, fontWeight: 500, color: '#d4d4d8', margin: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {r.repoName}
                </p>
                <p style={{ fontSize: 11, color: '#52525b', margin: '2px 0 0' }}>{r.prTitle || `PR #${r.prNumber}`}</p>
              </div>
              <span style={{ fontSize: 11, color: '#52525b', whiteSpace: 'nowrap' }}>#{r.prNumber}</span>
              <Badge variant={severityVariant(r.severity)} label={severityLabel(r.severity)} />
              <span style={{ fontSize: 13, fontWeight: 600, color: '#a1a1aa', width: 20, textAlign: 'right' }}>{r.score}</span>
              <span style={{ fontSize: 11, color: '#52525b', width: 56, textAlign: 'right' }}>{timeAgo(r.reviewedAt)}</span>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

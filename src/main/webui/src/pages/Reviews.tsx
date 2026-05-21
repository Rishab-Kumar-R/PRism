import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { reviewsApi } from '../api/reviews'
import type { ReviewRecord, ReviewSummary } from '../api/client'
import { Badge, severityVariant, severityLabel } from '../components/Badge'
import { ArrowLeft, ChevronLeft, ChevronRight } from 'lucide-react'
import ReactMarkdown from 'react-markdown'

function timeAgo(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  return `${Math.floor(hrs / 24)}d ago`
}

const cell: React.CSSProperties = { padding: '12px 16px', fontSize: 13, verticalAlign: 'middle' }
const headCell: React.CSSProperties = { padding: '10px 16px', fontSize: 11, fontWeight: 500, color: '#71717a', textTransform: 'uppercase', letterSpacing: '0.07em', textAlign: 'left', borderBottom: '1px solid #27272a' }

export function ReviewDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: review, isLoading } = useQuery<ReviewRecord>({
    queryKey: ['review', id],
    queryFn: () => reviewsApi.byId(Number(id)),
    enabled: !!id,
  })

  if (isLoading) {
    return (
      <div style={{ padding: '40px' }}>
        {[160, 240, 500].map((w, i) => (
          <div key={i} style={{ height: 16, background: '#27272a', borderRadius: 4, width: w, marginBottom: 16 }} />
        ))}
      </div>
    )
  }

  if (!review) return null

  return (
    <div style={{ padding: '40px', width: '100%', maxWidth: 860, boxSizing: 'border-box' }}>
      <button
        onClick={() => navigate('/reviews')}
        style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, color: '#71717a', background: 'none', border: 'none', cursor: 'pointer', marginBottom: 28, padding: 0 }}
        onMouseEnter={e => (e.currentTarget.style.color = '#d4d4d8')}
        onMouseLeave={e => (e.currentTarget.style.color = '#71717a')}
      >
        <ArrowLeft size={14} /> Back to Reviews
      </button>

      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 8 }}>
        <div>
          <h1 style={{ fontSize: 18, fontWeight: 600, color: '#f4f4f5', margin: 0 }}>{review.repoName}</h1>
          <p style={{ fontSize: 13, color: '#71717a', margin: '4px 0 0' }}>
            PR #{review.prNumber}{review.prTitle ? ` · ${review.prTitle}` : ''} · {timeAgo(review.reviewedAt)}
          </p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 2 }}>
          <Badge variant={severityVariant(review.severity)} label={severityLabel(review.severity)} />
          <span style={{ fontSize: 22, fontWeight: 700, color: '#f4f4f5' }}>
            {review.score}<span style={{ fontSize: 13, color: '#71717a', fontWeight: 400 }}>/10</span>
          </span>
        </div>
      </div>

      {review.recommendation && (
        <p style={{ fontSize: 13, color: '#a1a1aa', margin: '12px 0 20px', lineHeight: 1.6 }}>{review.recommendation}</p>
      )}

      {(review.bugCount > 0 || review.securityCount > 0 || review.performanceCount > 0 || review.codeQualityCount > 0) && (
        <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
          {review.bugCount > 0 && <span style={{ fontSize: 11, color: '#71717a', background: '#18181b', border: '1px solid #3f3f46', padding: '4px 10px', borderRadius: 6 }}>{review.bugCount} bug{review.bugCount !== 1 ? 's' : ''}</span>}
          {review.securityCount > 0 && <span style={{ fontSize: 11, color: '#71717a', background: '#18181b', border: '1px solid #3f3f46', padding: '4px 10px', borderRadius: 6 }}>{review.securityCount} security</span>}
          {review.performanceCount > 0 && <span style={{ fontSize: 11, color: '#71717a', background: '#18181b', border: '1px solid #3f3f46', padding: '4px 10px', borderRadius: 6 }}>{review.performanceCount} performance</span>}
          {review.codeQualityCount > 0 && <span style={{ fontSize: 11, color: '#71717a', background: '#18181b', border: '1px solid #3f3f46', padding: '4px 10px', borderRadius: 6 }}>{review.codeQualityCount} quality</span>}
        </div>
      )}

      <div style={{ background: '#18181b', border: '1px solid #3f3f46', borderRadius: 12, padding: '24px 28px' }} className="prose prose-invert prose-sm max-w-none prose-pre:bg-zinc-800 prose-pre:border prose-pre:border-zinc-700">
        <ReactMarkdown>{review.reviewComment}</ReactMarkdown>
      </div>
    </div>
  )
}

export function Reviews() {
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  const PAGE_SIZE = 20

  const { data: reviews, isLoading } = useQuery<ReviewSummary[]>({
    queryKey: ['reviews', page, PAGE_SIZE],
    queryFn: () => reviewsApi.list(page, PAGE_SIZE),
  })

  return (
    <div style={{ padding: '40px', width: '100%', boxSizing: 'border-box' }}>
      <div style={{ marginBottom: 32 }}>
        <h1 style={{ fontSize: 20, fontWeight: 600, color: '#f4f4f5', margin: 0, letterSpacing: '-0.3px' }}>Reviews</h1>
        <p style={{ fontSize: 13, color: '#71717a', margin: '4px 0 0' }}>All AI code review history</p>
      </div>

      <div style={{ background: '#18181b', border: '1px solid #3f3f46', borderRadius: 12, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th style={headCell}>Repository</th>
              <th style={{ ...headCell, width: 60 }}>PR</th>
              <th style={{ ...headCell, width: 120 }}>Outcome</th>
              <th style={headCell}>Summary</th>
              <th style={{ ...headCell, width: 60, textAlign: 'right' }}>Score</th>
              <th style={{ ...headCell, width: 80, textAlign: 'right', paddingRight: 20 }}>When</th>
            </tr>
          </thead>
          <tbody>
            {isLoading
              ? Array.from({ length: 8 }).map((_, i) => (
                <tr key={i} style={{ borderBottom: '1px solid #27272a' }}>
                  <td style={cell}><div style={{ height: 14, background: '#27272a', borderRadius: 4, width: 140 }} /></td>
                  <td style={cell}><div style={{ height: 14, background: '#27272a', borderRadius: 4, width: 36 }} /></td>
                  <td style={cell}><div style={{ height: 20, background: '#27272a', borderRadius: 4, width: 80 }} /></td>
                  <td style={cell}><div style={{ height: 14, background: '#27272a', borderRadius: 4, width: 200 }} /></td>
                  <td style={cell}><div style={{ height: 14, background: '#27272a', borderRadius: 4, width: 24, marginLeft: 'auto' }} /></td>
                  <td style={cell}><div style={{ height: 14, background: '#27272a', borderRadius: 4, width: 48, marginLeft: 'auto' }} /></td>
                </tr>
              ))
              : reviews?.map((r, idx) => (
                <tr
                  key={r.id}
                  onClick={() => navigate(`/reviews/${r.id}`)}
                  style={{ borderBottom: idx < reviews.length - 1 ? '1px solid #27272a' : 'none', cursor: 'pointer' }}
                  onMouseEnter={e => (e.currentTarget.style.background = 'rgba(39,39,42,0.5)')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                >
                  <td style={cell}>
                    <p style={{ margin: 0, fontWeight: 500, color: '#d4d4d8' }}>{r.repoName}</p>
                    {r.prTitle && <p style={{ margin: '2px 0 0', fontSize: 11, color: '#52525b' }}>{r.prTitle}</p>}
                  </td>
                  <td style={{ ...cell, color: '#71717a' }}>#{r.prNumber}</td>
                  <td style={cell}><Badge variant={severityVariant(r.severity)} label={severityLabel(r.severity)} /></td>
                  <td style={{ ...cell, color: '#71717a', maxWidth: 340 }}>
                    <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.recommendation}</span>
                  </td>
                  <td style={{ ...cell, color: '#a1a1aa', fontWeight: 600, textAlign: 'right' }}>{r.score}</td>
                  <td style={{ ...cell, color: '#52525b', fontSize: 11, textAlign: 'right', paddingRight: 20 }}>{timeAgo(r.reviewedAt)}</td>
                </tr>
              ))
            }
          </tbody>
        </table>

        {!isLoading && !reviews?.length && (
          <div style={{ padding: '64px 20px', textAlign: 'center', fontSize: 13, color: '#52525b' }}>No reviews found</div>
        )}

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 20px', borderTop: '1px solid #27272a' }}>
          <span style={{ fontSize: 12, color: '#52525b' }}>Page {page + 1}</span>
          <div style={{ display: 'flex', gap: 4 }}>
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
              style={{ padding: 6, background: 'none', border: 'none', cursor: page === 0 ? 'not-allowed' : 'pointer', color: page === 0 ? '#3f3f46' : '#71717a', borderRadius: 6 }}>
              <ChevronLeft size={14} />
            </button>
            <button onClick={() => setPage(p => p + 1)} disabled={(reviews?.length ?? 0) < PAGE_SIZE}
              style={{ padding: 6, background: 'none', border: 'none', cursor: (reviews?.length ?? 0) < PAGE_SIZE ? 'not-allowed' : 'pointer', color: (reviews?.length ?? 0) < PAGE_SIZE ? '#3f3f46' : '#71717a', borderRadius: 6 }}>
              <ChevronRight size={14} />
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

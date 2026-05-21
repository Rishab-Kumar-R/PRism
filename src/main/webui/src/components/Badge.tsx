import React from 'react'

type Variant = 'approved' | 'needs-work' | 'free' | 'pro' | 'enterprise' | 'active' | 'inactive'

const styles: Record<Variant, React.CSSProperties> = {
  'approved':   { background: '#052e16', color: '#4ade80', border: '1px solid #166534' },
  'needs-work': { background: '#450a0a', color: '#f87171', border: '1px solid #7f1d1d' },
  'free':       { background: '#27272a', color: '#a1a1aa', border: '1px solid #3f3f46' },
  'pro':        { background: '#0c1a3a', color: '#60a5fa', border: '1px solid #1e3a8a' },
  'enterprise': { background: '#1e0a3a', color: '#c084fc', border: '1px solid #581c87' },
  'active':     { background: '#052e16', color: '#4ade80', border: '1px solid #166534' },
  'inactive':   { background: '#27272a', color: '#71717a', border: '1px solid #3f3f46' },
}

export function Badge({ variant, label }: { variant: Variant; label: string }) {
  return (
    <span style={{
      ...styles[variant],
      display: 'inline-flex',
      alignItems: 'center',
      padding: '3px 10px',
      borderRadius: 6,
      fontSize: 11,
      fontWeight: 500,
      whiteSpace: 'nowrap',
      flexShrink: 0,
    }}>
      {label}
    </span>
  )
}

export function severityVariant(sev: string): Variant {
  const s = sev?.toUpperCase()
  if (s === 'APPROVED' || s === 'APPROVE') return 'approved'
  if (s === 'NEEDS_WORK' || s === 'REQUEST_CHANGES' || s === 'CHANGES_REQUESTED') return 'needs-work'
  return 'needs-work'
}

export function severityLabel(sev: string): string {
  const s = sev?.toUpperCase()
  if (s === 'APPROVED' || s === 'APPROVE') return 'Approved'
  if (s === 'NEEDS_WORK' || s === 'REQUEST_CHANGES') return 'Needs Work'
  return sev ?? '—'
}

export function tierVariant(tier: string): Variant {
  if (tier === 'PRO') return 'pro'
  if (tier === 'ENTERPRISE') return 'enterprise'
  return 'free'
}

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { adminApi } from '../api/admin'
import type { InstallationSummary } from '../api/client'
import { Badge, tierVariant } from '../components/Badge'
import { Pencil, Check, X } from 'lucide-react'

const TIERS = ['FREE', 'PRO', 'ENTERPRISE'] as const

const cell: React.CSSProperties = { padding: '12px 16px', fontSize: 13, verticalAlign: 'middle' }
const headCell: React.CSSProperties = {
  padding: '10px 16px', fontSize: 11, fontWeight: 500, color: '#71717a',
  textTransform: 'uppercase', letterSpacing: '0.07em', textAlign: 'left',
  borderBottom: '1px solid #27272a',
}

function TierSelect({ value, onChange, onCancel }: {
  value: string
  onChange: (tier: string) => void
  onCancel: () => void
}) {
  const [selected, setSelected] = useState(value)
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <select
        value={selected}
        onChange={e => setSelected(e.target.value)}
        style={{ background: '#27272a', border: '1px solid #3f3f46', color: '#e4e4e7', fontSize: 12, borderRadius: 6, padding: '3px 8px', outline: 'none' }}
      >
        {TIERS.map(t => <option key={t} value={t}>{t}</option>)}
      </select>
      <button onClick={() => onChange(selected)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#22c55e', padding: 2 }}><Check size={13} /></button>
      <button onClick={onCancel} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#71717a', padding: 2 }}><X size={13} /></button>
    </div>
  )
}

export function Admin() {
  const queryClient = useQueryClient()
  const [editingTier, setEditingTier] = useState<number | null>(null)

  const { data: installations, isLoading } = useQuery<InstallationSummary[]>({
    queryKey: ['installations'],
    queryFn: () => adminApi.listInstallations(),
  })

  const updateTier = useMutation({
    mutationFn: ({ id, tier }: { id: number; tier: string }) => adminApi.updateTier(id, tier),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['installations'] })
      setEditingTier(null)
    },
  })

  return (
    <div style={{ padding: '40px', width: '100%', boxSizing: 'border-box' }}>
      <div style={{ marginBottom: 32 }}>
        <h1 style={{ fontSize: 20, fontWeight: 600, color: '#f4f4f5', margin: 0, letterSpacing: '-0.3px' }}>Admin</h1>
        <p style={{ fontSize: 13, color: '#71717a', margin: '4px 0 0' }}>Manage GitHub App installations and tiers</p>
      </div>

      <div style={{ background: '#18181b', border: '1px solid #3f3f46', borderRadius: 12, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th style={headCell}>Account</th>
              <th style={{ ...headCell, width: 160 }}>Installation ID</th>
              <th style={{ ...headCell, width: 140 }}>Tier</th>
              <th style={{ ...headCell, width: 100 }}>Status</th>
              <th style={{ ...headCell, width: 140 }}>Monthly Limit</th>
              <th style={{ ...headCell, width: 120, paddingRight: 20 }}>Daily Limit</th>
            </tr>
          </thead>
          <tbody>
            {isLoading
              ? Array.from({ length: 4 }).map((_, i) => (
                <tr key={i} style={{ borderBottom: '1px solid #27272a' }}>
                  <td style={cell}><div style={{ height: 14, background: '#27272a', borderRadius: 4, width: 120 }} /></td>
                  <td style={cell}><div style={{ height: 14, background: '#27272a', borderRadius: 4, width: 80 }} /></td>
                  <td style={cell}><div style={{ height: 20, background: '#27272a', borderRadius: 4, width: 60 }} /></td>
                  <td style={cell}><div style={{ height: 20, background: '#27272a', borderRadius: 4, width: 60 }} /></td>
                  <td style={cell}><div style={{ height: 14, background: '#27272a', borderRadius: 4, width: 60 }} /></td>
                  <td style={cell}><div style={{ height: 14, background: '#27272a', borderRadius: 4, width: 50 }} /></td>
                </tr>
              ))
              : installations?.map((inst, idx) => (
                <tr
                  key={inst.installationId}
                  style={{ borderBottom: idx < installations.length - 1 ? '1px solid #27272a' : 'none' }}
                  onMouseEnter={e => (e.currentTarget.style.background = 'rgba(39,39,42,0.5)')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                >
                  <td style={{ ...cell, fontWeight: 500, color: '#d4d4d8' }}>{inst.accountLogin}</td>
                  <td style={{ ...cell, color: '#71717a', fontFamily: 'monospace', fontSize: 12 }}>{inst.installationId}</td>
                  <td style={cell}>
                    {editingTier === inst.installationId ? (
                      <TierSelect
                        value={inst.tier}
                        onChange={tier => updateTier.mutate({ id: inst.installationId, tier })}
                        onCancel={() => setEditingTier(null)}
                      />
                    ) : (
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <Badge variant={tierVariant(inst.tier)} label={inst.tier} />
                        <button
                          onClick={() => setEditingTier(inst.installationId)}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#52525b', padding: 2, lineHeight: 1 }}
                          onMouseEnter={e => (e.currentTarget.style.color = '#a1a1aa')}
                          onMouseLeave={e => (e.currentTarget.style.color = '#52525b')}
                        >
                          <Pencil size={11} />
                        </button>
                      </div>
                    )}
                  </td>
                  <td style={cell}>
                    <Badge variant={inst.active ? 'active' : 'inactive'} label={inst.active ? 'Active' : 'Inactive'} />
                  </td>
                  <td style={{ ...cell, color: inst.customMonthlyReviewLimit != null ? '#a1a1aa' : '#52525b', fontSize: 12 }}>
                    {inst.customMonthlyReviewLimit != null ? inst.customMonthlyReviewLimit : 'default'}
                  </td>
                  <td style={{ ...cell, color: inst.customDailyReviewLimit != null ? '#a1a1aa' : '#52525b', fontSize: 12, paddingRight: 20 }}>
                    {inst.customDailyReviewLimit != null ? inst.customDailyReviewLimit : 'default'}
                  </td>
                </tr>
              ))
            }
          </tbody>
        </table>

        {!isLoading && !installations?.length && (
          <div style={{ padding: '64px 20px', textAlign: 'center', fontSize: 13, color: '#52525b' }}>No installations found</div>
        )}
      </div>
    </div>
  )
}

import { NavLink } from 'react-router-dom'

function DashboardIcon({ color }: { color: string }) {
  return (
    <svg width="15" height="15" viewBox="0 0 22 17" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="0" y="0" width="22" height="5" rx="1.5" stroke={color} strokeWidth="1.4"/>
      <rect x="0" y="7.5" width="22" height="3.5" rx="1.5" stroke={color} strokeWidth="1.4" opacity="0.6"/>
      <rect x="0" y="13" width="13" height="3.5" rx="1.5" stroke={color} strokeWidth="1.4" opacity="0.35"/>
    </svg>
  )
}

function ReviewsIcon({ color }: { color: string }) {
  return (
    <svg width="15" height="15" viewBox="0 0 20 13" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="0" y="0" width="8" height="2.5" rx="1" fill={color}/>
      <rect x="0" y="5" width="6" height="2.5" rx="1" fill={color} opacity="0.5"/>
      <rect x="0" y="10" width="8" height="2.5" rx="1" fill={color} opacity="0.3"/>
      <line x1="10.5" y1="0" x2="10.5" y2="13" stroke={color} strokeWidth="0.8" opacity="0.2" strokeDasharray="1.5 1.5"/>
      <rect x="12" y="0" width="8" height="2.5" rx="1" fill={color} opacity="0.7"/>
      <rect x="12" y="5" width="5" height="2.5" rx="1" fill={color} opacity="0.4"/>
      <rect x="12" y="10" width="7" height="2.5" rx="1" fill={color} opacity="0.25"/>
    </svg>
  )
}

function AdminIcon({ color }: { color: string }) {
  return (
    <svg width="15" height="15" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M1 16 L1 8 L6 13 L10 3 L14 13 L19 8 L19 16 Z" stroke={color} strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round"/>
      <line x1="1" y1="19" x2="19" y2="19" stroke={color} strokeWidth="1.35" strokeLinecap="round"/>
    </svg>
  )
}

const nav = [
  { to: '/', label: 'Dashboard', Icon: DashboardIcon, exact: true },
  { to: '/reviews', label: 'Reviews', Icon: ReviewsIcon, exact: false },
  { to: '/admin', label: 'Admin', Icon: AdminIcon, exact: false },
]

export function Sidebar() {
  return (
    <aside style={{
      width: 220,
      flexShrink: 0,
      background: '#0c0c0e',
      borderRight: '1px solid #27272a',
      display: 'flex',
      flexDirection: 'column',
      height: '100vh',
    }}>
      {/* Logo */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        padding: '18px 16px',
        borderBottom: '1px solid #27272a',
      }}>
        <svg width="24" height="24" viewBox="0 0 30 30" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ flexShrink: 0 }}>
          <defs>
            <clipPath id="logoClip">
              <circle cx="15" cy="15" r="12"/>
            </clipPath>
            <linearGradient id="waveGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#93c5fd"/>
              <stop offset="100%" stopColor="#2563eb"/>
            </linearGradient>
          </defs>
          <g clipPath="url(#logoClip)" stroke="url(#waveGrad)" strokeWidth="2" fill="none" strokeLinecap="round">
            <path d="M3,8.5  Q9,5.5  15,8.5  Q21,11.5 27,8.5"/>
            <path d="M3,12   Q9,9    15,12   Q21,15   27,12"/>
            <path d="M3,15.5 Q9,12.5 15,15.5 Q21,18.5 27,15.5"/>
            <path d="M3,19   Q9,16   15,19   Q21,22   27,19"/>
            <path d="M3,22.5 Q9,19.5 15,22.5 Q21,25.5 27,22.5"/>
          </g>
        </svg>
        <span style={{ fontSize: 14, fontWeight: 600, color: '#f4f4f5', letterSpacing: '-0.2px' }}>Prism</span>
      </div>

      {/* Nav */}
      <nav style={{ flex: 1, padding: '10px 8px' }}>
        {nav.map(({ to, label, Icon, exact }) => (
          <NavLink
            key={to}
            to={to}
            end={exact}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              gap: 9,
              padding: '8px 12px',
              borderRadius: 8,
              marginBottom: 2,
              fontSize: 13,
              fontWeight: isActive ? 500 : 400,
              color: isActive ? '#f4f4f5' : '#71717a',
              background: isActive ? '#27272a' : 'transparent',
              textDecoration: 'none',
              transition: 'background 0.15s, color 0.15s',
            })}
            onMouseEnter={e => {
              const el = e.currentTarget
              if (!el.getAttribute('aria-current')) {
                el.style.color = '#d4d4d8'
                el.style.background = 'rgba(39,39,42,0.5)'
              }
            }}
            onMouseLeave={e => {
              const el = e.currentTarget
              if (!el.getAttribute('aria-current')) {
                el.style.color = '#71717a'
                el.style.background = 'transparent'
              }
            }}
          >
            <Icon color="currentColor" />
            {label}
          </NavLink>
        ))}
      </nav>

    </aside>
  )
}

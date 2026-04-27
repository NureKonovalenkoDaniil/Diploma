import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  Pill,
  Cpu,
  MapPin,
  AlertTriangle,
  Bell,
  ClipboardList,
  FlaskConical,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAuth } from '@/contexts/AuthContext'

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Дашборд' },
  { to: '/medicines', icon: Pill, label: 'Препарати' },
  { to: '/iot-devices', icon: Cpu, label: 'IoT Пристрої' },
  { to: '/storage-locations', icon: MapPin, label: 'Локації' },
  { to: '/incidents', icon: AlertTriangle, label: 'Інциденти' },
  { to: '/notifications', icon: Bell, label: 'Сповіщення' },
]

const adminItems = [
  { to: '/audit-log', icon: ClipboardList, label: 'Журнал аудиту' },
]

export function Sidebar() {
  const { isAdmin } = useAuth()

  return (
    <aside className="flex h-screen w-60 flex-col border-r bg-sidebar text-sidebar-foreground">
      {/* Logo */}
      <div className="flex h-16 items-center gap-3 border-b border-sidebar-border px-5">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
          <FlaskConical className="h-4 w-4 text-primary-foreground" />
        </div>
        <div className="leading-tight">
          <p className="text-sm font-semibold">MedStorage</p>
          <p className="text-[10px] text-sidebar-foreground/60">Система управління</p>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto py-4">
        <ul className="space-y-1 px-3">
          {navItems.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-sidebar-accent text-sidebar-accent-foreground'
                      : 'text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground',
                  )
                }
              >
                <item.icon className="h-4 w-4 shrink-0" />
                {item.label}
              </NavLink>
            </li>
          ))}

          {isAdmin && (
            <>
              <li className="mt-4 mb-1 px-3 text-[10px] font-semibold uppercase tracking-widest text-sidebar-foreground/40">
                Адміністрування
              </li>
              {adminItems.map((item) => (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    className={({ isActive }) =>
                      cn(
                        'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                        isActive
                          ? 'bg-sidebar-accent text-sidebar-accent-foreground'
                          : 'text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground',
                      )
                    }
                  >
                    <item.icon className="h-4 w-4 shrink-0" />
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </>
          )}
        </ul>
      </nav>

      {/* Footer */}
      <div className="border-t border-sidebar-border px-4 py-3 text-[10px] text-sidebar-foreground/40 text-center">
        © 2026 MedStorage System
      </div>
    </aside>
  )
}

import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Pill,
  Cpu,
  MapPin,
  AlertTriangle,
  Bell,
  ClipboardList,
  FlaskConical,
  Users,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuth } from '@/contexts/AuthContext';
import { useQuery } from '@tanstack/react-query';
import { incidentApi, notificationApi } from '@/api';

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Дашборд' },
  { to: '/medicines', icon: Pill, label: 'Препарати' },
  { to: '/storage-locations', icon: MapPin, label: 'Локації' },
  { to: '/incidents', icon: AlertTriangle, label: 'Інциденти', badge: 'incidents' },
  { to: '/notifications', icon: Bell, label: 'Сповіщення', badge: 'notifications' },
];

const managerItems = [{ to: '/iot-devices', icon: Cpu, label: 'Інвентар пристроїв' }];

const adminItems = [
  { to: '/users', icon: Users, label: 'Користувачі' },
  { to: '/audit-log', icon: ClipboardList, label: 'Журнал аудиту' },
];

export function Sidebar() {
  const { isAdmin, isManager } = useAuth();
  const canManageDevices = isAdmin || isManager;

  // Polling for active incidents count
  const { data: activeIncidents = [] } = useQuery({
    queryKey: ['incidents', 'active'],
    queryFn: incidentApi.getActive,
    refetchInterval: 10000,
  });

  // Polling for unread notifications count
  const { data: unreadNotifications = [] } = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: notificationApi.getUnread,
    refetchInterval: 10000,
  });

  const getBadge = (type?: string) => {
    if (type === 'incidents' && activeIncidents.length > 0) {
      return (
        <span className="ml-auto flex h-5 min-w-5 items-center justify-center rounded-full bg-destructive px-1.5 text-[10px] font-bold text-destructive-foreground">
          {activeIncidents.length > 99 ? '99+' : activeIncidents.length}
        </span>
      );
    }
    if (type === 'notifications' && unreadNotifications.length > 0) {
      return (
        <span className="ml-auto flex h-5 min-w-5 items-center justify-center rounded-full bg-amber-500 px-1.5 text-[10px] font-bold text-white">
          {unreadNotifications.length > 99 ? '99+' : unreadNotifications.length}
        </span>
      );
    }
    return null;
  };

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
                }>
                <item.icon className="h-4 w-4 shrink-0" />
                {item.label}
                {item.badge && getBadge(item.badge)}
              </NavLink>
            </li>
          ))}

          {canManageDevices &&
            managerItems.map((item) => (
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
                  }>
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
                    }>
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
  );
}

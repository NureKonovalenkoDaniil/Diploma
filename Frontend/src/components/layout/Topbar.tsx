import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Bell, Sun, Moon, Monitor, LogOut, User } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { notificationApi } from '@/api'
import { useAuth } from '@/contexts/AuthContext'
import { useTheme } from '@/contexts/ThemeContext'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { format } from 'date-fns'

export function Topbar() {
  const { user, logout } = useAuth()
  const { theme, setTheme } = useTheme()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: unread = [] } = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: notificationApi.getUnread,
    refetchInterval: 10000,
  })

  const markAllMutation = useMutation({
    mutationFn: notificationApi.markAllAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const ThemeIcon = theme === 'dark' ? Moon : theme === 'light' ? Sun : Monitor

  return (
    <header className="flex h-16 items-center justify-between border-b bg-background px-6">
      {/* Breadcrumb / Page Title placeholder */}
      <div />

      <div className="flex items-center gap-2">
        {/* Theme Toggle */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon">
              <ThemeIcon className="h-4 w-4" />
              <span className="sr-only">Тема</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>Тема оформлення</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => setTheme('light')}>
              <Sun className="mr-2 h-4 w-4" /> Світла
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => setTheme('dark')}>
              <Moon className="mr-2 h-4 w-4" /> Темна
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => setTheme('system')}>
              <Monitor className="mr-2 h-4 w-4" /> Системна
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        {/* Notifications Bell */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" className="relative">
              <Bell className="h-4 w-4" />
              {unread.length > 0 && (
                <span className="absolute -right-0.5 -top-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-destructive text-[10px] font-bold text-destructive-foreground">
                  {unread.length > 9 ? '9+' : unread.length}
                </span>
              )}
              <span className="sr-only">Сповіщення</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-80">
            <div className="flex items-center justify-between px-2 py-1.5">
              <DropdownMenuLabel className="p-0">Сповіщення</DropdownMenuLabel>
              {unread.length > 0 && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-auto p-0 text-xs text-muted-foreground hover:text-foreground"
                  onClick={() => markAllMutation.mutate()}
                >
                  Прочитати всі
                </Button>
              )}
            </div>
            <DropdownMenuSeparator />
            {unread.length === 0 ? (
              <div className="py-6 text-center text-sm text-muted-foreground">
                Нових сповіщень немає
              </div>
            ) : (
              <>
                {unread.slice(0, 5).map((n) => (
                  <DropdownMenuItem key={n.notificationId} className="flex-col items-start gap-0.5 py-2">
                    <div className="flex w-full items-center justify-between">
                      <span className="text-sm font-medium">{n.title}</span>
                      <Badge
                        variant={
                          n.type === 'StorageViolation' ? 'destructive' : 
                          n.type === 'StorageRestored' ? 'success' :
                          (n.type === 'LowStock' || n.type === 'Expiry') ? 'warning' : 
                          'info'
                        }
                        className="text-[10px]"
                      >
                        {n.type === 'StorageViolation' ? 'Порушення' : 
                         n.type === 'StorageRestored' ? 'Норма' :
                         n.type === 'LowStock' ? 'Запас' :
                         n.type === 'Expiry' ? 'Термін' : n.type}
                      </Badge>
                    </div>
                    <p className="text-xs text-muted-foreground line-clamp-2">{n.message}</p>
                    <p className="text-[10px] text-muted-foreground/60">
                      {format(new Date(n.createdAt), 'dd.MM HH:mm')}
                    </p>
                  </DropdownMenuItem>
                ))}
                {unread.length > 5 && (
                  <DropdownMenuItem onClick={() => navigate('/notifications')} className="justify-center text-xs text-primary">
                    Переглянути всі ({unread.length})
                  </DropdownMenuItem>
                )}
              </>
            )}
          </DropdownMenuContent>
        </DropdownMenu>

        {/* User Menu */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="gap-2">
              <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10">
                <User className="h-3.5 w-3.5 text-primary" />
              </div>
              <span className="hidden text-sm sm:block">{user?.email}</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>
              <p>{user?.email}</p>
              <p className="text-xs font-normal text-muted-foreground">{user?.roles?.join(', ')}</p>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={handleLogout} className="text-destructive">
              <LogOut className="mr-2 h-4 w-4" />
              Вийти
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  )
}

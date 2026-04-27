import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { notificationApi } from '@/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Bell, Check, CheckCheck } from 'lucide-react'
import { format } from 'date-fns'

const TYPE_CONFIG: Record<string, { label: string; variant: 'destructive' | 'warning' | 'info' | 'secondary' }> = {
  StorageViolation: { label: 'Порушення', variant: 'destructive' },
  Expiry: { label: 'Термін', variant: 'warning' },
  LowStock: { label: 'Запас', variant: 'warning' },
  System: { label: 'Система', variant: 'info' },
}

export default function NotificationsPage() {
  const qc = useQueryClient()

  const { data: notifications = [], isLoading } = useQuery({
    queryKey: ['notifications', 'all'],
    queryFn: () => notificationApi.getAll(),
  })

  const markReadMutation = useMutation({
    mutationFn: (id: number) => notificationApi.markAsRead(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  })

  const markAllMutation = useMutation({
    mutationFn: notificationApi.markAllAsRead,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  })

  const unreadCount = notifications.filter((n) => !n.isRead).length

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Сповіщення</h1>
          <p className="text-muted-foreground">{unreadCount} непрочитаних</p>
        </div>
        {unreadCount > 0 && (
          <Button variant="outline" onClick={() => markAllMutation.mutate()} disabled={markAllMutation.isPending}>
            <CheckCheck className="h-4 w-4" />
            Прочитати всі
          </Button>
        )}
      </div>

      <div className="space-y-3">
        {isLoading
          ? Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-20" />)
          : notifications.length === 0
            ? (
              <Card>
                <CardContent className="flex flex-col items-center gap-3 py-16">
                  <Bell className="h-10 w-10 text-muted-foreground/40" />
                  <p className="text-muted-foreground">Сповіщень немає</p>
                </CardContent>
              </Card>
            )
            : notifications.map((n) => {
              const cfg = TYPE_CONFIG[n.type] ?? { label: n.type, variant: 'secondary' }
              return (
                <Card
                  key={n.notificationId}
                  className={`transition-colors ${!n.isRead ? 'border-primary/30 bg-primary/5' : ''}`}
                >
                  <CardHeader className="pb-2">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-2">
                        {!n.isRead && <div className="h-2 w-2 rounded-full bg-primary shrink-0" />}
                        <CardTitle className="text-sm font-semibold">{n.title}</CardTitle>
                        <Badge variant={cfg.variant} className="text-[10px]">{cfg.label}</Badge>
                      </div>
                      <div className="flex items-center gap-2 shrink-0">
                        <CardDescription className="text-xs">{format(new Date(n.createdAt), 'dd.MM.yyyy HH:mm')}</CardDescription>
                        {!n.isRead && (
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-7 w-7"
                            onClick={() => markReadMutation.mutate(n.notificationId)}
                          >
                            <Check className="h-3.5 w-3.5" />
                          </Button>
                        )}
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent className="pt-0">
                    <p className="text-sm text-muted-foreground">{n.message}</p>
                    {n.targetRole !== 'All' && (
                      <p className="mt-1 text-[10px] text-muted-foreground/60">Для: {n.targetRole}</p>
                    )}
                  </CardContent>
                </Card>
              )
            })}
      </div>
    </div>
  )
}

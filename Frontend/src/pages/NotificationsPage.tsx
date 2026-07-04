import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { notificationApi } from '@/api';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Bell, Check, CheckCheck } from 'lucide-react';
import { format } from 'date-fns';
import { useLocale, translateNotification } from '@/contexts/LocaleContext';

const NOTIFICATION_TYPES = ['StorageViolation', 'StorageRestored', 'Expiry', 'LowStock'];

export default function NotificationsPage() {
  const qc = useQueryClient();
  const { t } = useLocale();
  const [typeFilter, setTypeFilter] = useState('all');
  const [readFilter, setReadFilter] = useState<'all' | 'unread' | 'read'>('all');

  const { data: notifications = [], isLoading } = useQuery({
    queryKey: ['notifications', 'all'],
    queryFn: () => notificationApi.getAll(),
  });

  const markReadMutation = useMutation({
    mutationFn: (id: number) => notificationApi.markAsRead(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  });

  const markAllMutation = useMutation({
    mutationFn: notificationApi.markAllAsRead,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  });

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  const filtered = notifications.filter((n) => {
    const matchType = typeFilter === 'all' || n.type === typeFilter;
    const matchRead =
      readFilter === 'all' ||
      (readFilter === 'unread' && !n.isRead) ||
      (readFilter === 'read' && n.isRead);
    return matchType && matchRead;
  });

  const getLabel = (type: string) => {
    switch (type) {
      case 'StorageViolation': return t('notificationTypeStorageViolation');
      case 'StorageRestored': return t('notificationTypeStorageRestored');
      case 'Expiry': return t('notificationTypeExpiry');
      case 'LowStock': return t('notificationTypeLowStock');
      case 'IncidentCreated': return t('notificationTypeIncidentCreated');
      default: return type;
    }
  };

  const getVariant = (type: string): 'destructive' | 'success' | 'warning' | 'info' | 'secondary' => {
    switch (type) {
      case 'StorageViolation': return 'destructive';
      case 'StorageRestored': return 'success';
      case 'Expiry': return 'warning';
      case 'LowStock': return 'warning';
      case 'IncidentCreated': return 'destructive';
      default: return 'secondary';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t('notificationsTitle')}</h1>
          <p className="text-muted-foreground">{t('unreadCount', { count: unreadCount })}</p>
        </div>
        {unreadCount > 0 && (
          <Button
            variant="outline"
            onClick={() => markAllMutation.mutate()}
            disabled={markAllMutation.isPending}>
            <CheckCheck className="h-4 w-4" />
            {t('readAll')}
          </Button>
        )}
      </div>

      {/* Filter bar */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2">
          <label className="text-xs text-muted-foreground whitespace-nowrap">{t('filterByType')}:</label>
          <select
            aria-label={t('filterByType')}
            className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground"
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}>
            <option value="all">{t('filterAll')}</option>
            {NOTIFICATION_TYPES.map((nt) => (
              <option key={nt} value={nt}>
                {getLabel(nt)}
              </option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-xs text-muted-foreground whitespace-nowrap">{t('filterByRead')}:</label>
          <select
            aria-label={t('filterByRead')}
            className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground"
            value={readFilter}
            onChange={(e) => setReadFilter(e.target.value as 'all' | 'unread' | 'read')}>
            <option value="all">{t('filterAll')}</option>
            <option value="unread">{t('filterUnread')}</option>
            <option value="read">{t('filterRead')}</option>
          </select>
        </div>
        <span className="text-xs text-muted-foreground">
          {t('filteredCount', { filtered: filtered.length, total: notifications.length })}
        </span>
      </div>

      <div className="space-y-3">
        {isLoading ? (
          Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-20" />)
        ) : notifications.length === 0 ? (
          <Card>
            <CardContent className="flex flex-col items-center gap-3 py-16">
              <Bell className="h-10 w-10 text-muted-foreground/40" />
              <p className="text-muted-foreground">{t('noNotificationsCard')}</p>
            </CardContent>
          </Card>
        ) : filtered.length === 0 ? (
          <Card>
            <CardContent className="flex flex-col items-center gap-3 py-16">
              <Bell className="h-10 w-10 text-muted-foreground/40" />
              <p className="text-muted-foreground">{t('noItemsMatchFilter')}</p>
            </CardContent>
          </Card>
        ) : (
          filtered.map((n) => {
            const label = getLabel(n.type);
            const variant = getVariant(n.type);
            const { title: translatedTitle, message: translatedMessage } = translateNotification(n.title, n.message, t);
            return (
              <Card
                key={n.notificationId}
                className={`transition-colors ${!n.isRead ? 'border-primary/30 bg-primary/5' : ''}`}>
                <CardHeader className="pb-2">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2">
                      {!n.isRead && <div className="h-2 w-2 rounded-full bg-primary shrink-0" />}
                      <CardTitle className="text-sm font-semibold">{translatedTitle}</CardTitle>
                      <Badge variant={variant} className="text-[10px]">
                        {label}
                      </Badge>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <CardDescription className="text-xs">
                        {format(new Date(n.createdAt), 'dd.MM.yyyy HH:mm')}
                      </CardDescription>
                      {!n.isRead && (
                        <Button
                           variant="ghost"
                          size="icon"
                          className="h-7 w-7"
                          onClick={() => markReadMutation.mutate(n.notificationId)}>
                          <Check className="h-3.5 w-3.5" />
                        </Button>
                      )}
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="pt-0">
                  <p className="text-sm text-muted-foreground">{translatedMessage}</p>
                  {n.targetRole !== 'All' && (
                    <p className="mt-1 text-[10px] text-muted-foreground/60">
                      {t('forRole', { role: t('role' + n.targetRole) })}
                    </p>
                  )}
                </CardContent>
              </Card>
            );
          })
        )}
      </div>
    </div>
  );
}

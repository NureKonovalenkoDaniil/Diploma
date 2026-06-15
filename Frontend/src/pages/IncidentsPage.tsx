import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { incidentApi } from '@/api';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { AlertTriangle, CheckCircle, Clock, Search } from 'lucide-react';
import { format } from 'date-fns';
import { useLocale } from '@/contexts/LocaleContext';

type TabType = 'active' | 'all';

export default function IncidentsPage() {
  const { isAdmin, isManager, isUser } = useAuth();
  const { t } = useLocale();
  const canManage = isAdmin || isManager || isUser;
  const qc = useQueryClient();
  const [tab, setTab] = useState<TabType>('active');
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');

  const { data: active = [], isLoading: aLoading } = useQuery({
    queryKey: ['incidents', 'active'],
    queryFn: incidentApi.getActive,
    refetchInterval: 10000,
  });

  const { data: all = [], isLoading: allLoading } = useQuery({
    queryKey: ['incidents', 'all'],
    queryFn: incidentApi.getAll,
    enabled: tab === 'all',
  });

  const resolveMutation = useMutation({
    mutationFn: (id: number) => incidentApi.resolve(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['incidents'] });
    },
  });

  const rawIncidents = tab === 'active' ? active : all;
  const isLoading = tab === 'active' ? aLoading : allLoading;

  const incidentTypeLabel = (tType: string) =>
    tType === 'TemperatureViolation' ? t('incidentTypeTemperature') : t('incidentTypeHumidity');

  const statusVariant = (s: string) => {
    if (s === 'Active') return 'destructive';
    if (s === 'Resolved' || s === 'AutoResolved') return 'success';
    return 'secondary';
  };

  const statusLabel = (s: string) => {
    if (s === 'Active') return t('statusActive');
    if (s === 'Resolved') return t('statusResolvedManual');
    if (s === 'AutoResolved') return t('statusResolvedAuto');
    return s;
  };

  const incidents = rawIncidents.filter((inc) => {
    const q = search.toLowerCase();
    const matchSearch =
      !q ||
      (inc.deviceLocation ?? '').toLowerCase().includes(q) ||
      (inc.locationName ?? '').toLowerCase().includes(q);
    const matchType = typeFilter === 'all' || inc.incidentType === typeFilter;
    return matchSearch && matchType;
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">{t('incidentsTitle')}</h1>
        <p className="text-muted-foreground">{t('incidentsSubtitle')}</p>
      </div>

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-destructive/10 p-3">
              <AlertTriangle className="h-5 w-5 text-destructive" />
            </div>
            <div>
              <p className="text-2xl font-bold">{active.length}</p>
              <p className="text-xs text-muted-foreground">{t('activeIncidentsCount')}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-amber-500/10 p-3">
              <Clock className="h-5 w-5 text-amber-500" />
            </div>
            <div>
              <p className="text-2xl font-bold">
                {all.filter((i) => i.status === 'Acknowledged').length}
              </p>
              <p className="text-xs text-muted-foreground">{t('confirmed')}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-emerald-500/10 p-3">
              <CheckCircle className="h-5 w-5 text-emerald-500" />
            </div>
            <div>
              <p className="text-2xl font-bold">
                {all.filter((i) => i.status === 'Resolved' || i.status === 'AutoResolved').length}
              </p>
              <p className="text-xs text-muted-foreground">{t('resolved')}</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Tabs */}
      <div className="flex gap-2">
        <Button
          variant={tab === 'active' ? 'default' : 'outline'}
          size="sm"
          onClick={() => setTab('active')}>
          {t('activeTab')} ({active.length})
        </Button>
        <Button
          variant={tab === 'all' ? 'default' : 'outline'}
          size="sm"
          onClick={() => setTab('all')}>
          {t('allIncidents')}
        </Button>
      </div>

      {/* Filter bar */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-48">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder={t('incidentDeviceFilter')}
            className="pl-9"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-xs text-muted-foreground whitespace-nowrap">{t('filterByIncidentType')}:</label>
          <select
            aria-label={t('filterByIncidentType')}
            className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground"
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}>
            <option value="all">{t('filterAll')}</option>
            <option value="TemperatureViolation">{t('incidentTypeTemperature')}</option>
            <option value="HumidityViolation">{t('incidentTypeHumidity')}</option>
          </select>
        </div>
        <span className="text-xs text-muted-foreground">
          {t('filteredCount', { filtered: incidents.length, total: rawIncidents.length })}
        </span>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">
            {tab === 'active' ? t('activeViolationsTitle') : t('allIncidentsTitle')}
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-4 space-y-2">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-10" />
              ))}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('incidentDevice')}</TableHead>
                  <TableHead>{t('incidentType')}</TableHead>
                  <TableHead>{t('incidentDetectedValue')}</TableHead>
                  <TableHead>{t('incidentNorm')}</TableHead>
                  <TableHead>{t('incidentStatus')}</TableHead>
                  <TableHead>{t('incidentStart')}</TableHead>
                  <TableHead>{t('incidentEnd')}</TableHead>
                  {canManage && <TableHead>{t('incidentActions')}</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {incidents.length === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={canManage ? 8 : 7}
                      className="py-10 text-center text-muted-foreground">
                      {rawIncidents.length === 0
                        ? (tab === 'active' ? t('noActiveIncidents') : t('noIncidents'))
                        : t('noItemsMatchFilter')}
                    </TableCell>
                  </TableRow>
                ) : (
                  incidents.map((inc) => (
                    <TableRow key={inc.incidentId}>
                      <TableCell>
                        <div className="font-medium">{inc.deviceLocation}</div>
                        {inc.locationName && (
                          <div className="text-xs text-muted-foreground">{inc.locationName}</div>
                        )}
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline">{incidentTypeLabel(inc.incidentType)}</Badge>
                      </TableCell>
                      <TableCell>
                        <span
                          className={`font-semibold ${inc.status === 'Active' ? 'text-destructive' : ''}`}>
                          {inc.detectedValue.toFixed(1)}
                          {inc.incidentType === 'TemperatureViolation' ? '°C' : '%'}
                        </span>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {inc.expectedMin} – {inc.expectedMax}
                        {inc.incidentType === 'TemperatureViolation' ? '°C' : '%'}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            statusVariant(inc.status) as 'destructive' | 'success' | 'secondary'
                          }>
                          {statusLabel(inc.status)}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm">
                        {format(new Date(inc.startTime), 'dd.MM HH:mm')}
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {inc.endTime ? format(new Date(inc.endTime), 'dd.MM HH:mm') : '—'}
                      </TableCell>
                      {canManage && (
                        <TableCell>
                          {inc.status === 'Active' && (
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => resolveMutation.mutate(inc.incidentId)}
                              disabled={resolveMutation.isPending}>
                              {t('close')}
                            </Button>
                          )}
                        </TableCell>
                      )}
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

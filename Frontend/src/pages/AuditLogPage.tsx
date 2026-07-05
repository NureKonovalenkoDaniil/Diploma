import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { auditApi } from '@/api';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useLocale } from '@/contexts/LocaleContext';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Skeleton } from '@/components/ui/skeleton';
import { Search, RotateCcw } from 'lucide-react';
import { format } from 'date-fns';

type Filters = { from: string; to: string; user: string; action: string };

const AUDIT_ACTIONS = [
  'medicine_actions',
  'location_actions',
  'incident_actions',
  'device_actions',
  'user_actions',
];

const severityVariant = (s: string): 'destructive' | 'warning' | 'info' => {
  if (s === 'Error') return 'destructive';
  if (s === 'Warning') return 'warning';
  return 'info';
};

function translateAuditAction(action: string, t: (key: string) => string): string {
  if (action === 'medicine_actions') return t('auditActionMedicine');
  if (action === 'location_actions') return t('auditActionLocation');
  if (action === 'incident_actions') return t('auditActionIncident');
  if (action === 'device_actions') return t('auditActionDevice');
  if (action === 'user_actions') return t('auditActionUser');

  const normalized = action.replace(/\s+/g, '').replace(/_/g, '').replace(/-/g, '');
  const key = `auditAction${normalized}`;
  const translated = t(key);
  return translated !== key ? translated : action;
}

function translateAuditEntity(entityType: string | null | undefined, t: (key: string) => string): string {
  if (!entityType) return '—';
  const key = `auditEntity${entityType}`;
  const translated = t(key);
  return translated !== key ? translated : entityType;
}

function translateAuditSeverity(severity: string, t: (key: string) => string): string {
  const key = `auditLevel${severity}`;
  const translated = t(key);
  return translated !== key ? translated : severity;
}

function translateAuditDetails(details: string | null | undefined, t: (key: string, params?: any) => string): string {
  if (!details) return '—';

  if (details === 'Successful login.') return t('auditDetailsSuccessfulLogin');
  if (details === 'Email confirmed.') return t('auditDetailsEmailConfirmed');
  if (details === 'Password reset completed.') return t('auditDetailsPasswordResetCompleted');
  if (details === 'Successful device login.') return t('auditDetailsSuccessfulDeviceLogin');
  if (details === 'Device claimed and secret issued.') return t('auditDetailsDeviceClaimed');
  if (details === 'Registered new user with role User.') return t('auditDetailsRegisteredUser');

  let match = details.match(/^Created manager (.*?) for org (.*?)\.?$/i);
  if (match) return t('auditDetailsCreatedManager', { email: match[1], orgId: match[2] });

  match = details.match(/^Assigned role (.*?) to user (.*?)\.?$/i);
  if (match) return t('auditDetailsAssignedRole', { role: match[1], email: match[2] });

  match = details.match(/^Created role:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsCreatedRole', { role: match[1] });

  match = details.match(/^Deleted user:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsDeletedUser', { email: match[1] });

  match = details.match(/^Created medicine:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsCreatedMedicine', { name: match[1] });

  match = details.match(/^Updated medicine:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsUpdatedMedicine', { name: match[1] });

  match = details.match(/^Deleted medicine with ID:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsDeletedMedicine', { id: match[1] });

  match = details.match(/^Created sensor:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsCreatedSensor', { id: match[1] });

  match = details.match(/^Deleted sensor:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsDeletedSensor', { id: match[1] });

  match = details.match(/^Sensor (.*?) status set to (.*?)\.?$/i);
  if (match) return t('auditDetailsSensorStatusSet', { id: match[1], status: match[2] });

  match = details.match(/^Created StorageLocation:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsCreatedStorageLocation', { name: match[1] });

  match = details.match(/^Updated StorageLocation:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsUpdatedStorageLocation', { name: match[1] });

  match = details.match(/^Deleted storage location with ID:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsDeletedStorageLocation', { id: match[1] });

  match = details.match(/^Created StorageIncident:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsCreatedStorageIncident', { id: match[1] });

  match = details.match(/^Resolved storage incident:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsResolvedStorageIncident', { id: match[1] });

  match = details.match(/^Created Condition:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsCreatedCondition', { id: match[1] });

  match = details.match(/^Deleted Condition:\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsDeletedCondition', { id: match[1] });

  // ─── NEW DETAILED TRANSLATIONS ───
  match = details.match(/^Received\s*\+(\d+)\s*for\s*medicine\s*ID\s*(\d+)\.?$/i);
  if (match) return t('auditDetailsReceivedQty', { qty: match[1], id: match[2] });

  match = details.match(/^Issued\s*-(\d+)\s*for\s*medicine\s*ID\s*(\d+)\.?$/i);
  if (match) return t('auditDetailsIssuedQty', { qty: match[1], id: match[2] });

  match = details.match(/^Disposed\s*(.*?)\s*for\s*medicine\s*ID\s*(\d+)\.?$/i);
  if (match) {
    const qty = match[1];
    const id = match[2];
    if (qty.toLowerCase() === 'all') {
      return t('auditDetailsDisposedAll', { id });
    } else {
      return t('auditDetailsDisposedQty', { qty, id });
    }
  }

  match = details.match(/^Moved\s*medicine\s*ID\s*(\d+)\s*to\s*StorageLocationId\s*(.*?)\.?$/i);
  if (match) return t('auditDetailsMovedMedicine', { id: match[1], locId: match[2] });

  match = details.match(/^Lifecycle\s*event\s*'(.*?)'\s*added\s*for\s*Medicine\s*ID:\s*(\d+)\.?$/i);
  if (match) return t('auditDetailsLifecycleEventAdded', { type: t('eventType' + match[1]), id: match[2] });

  // Reusing notification message translation regexes for sensor violations in logs:
  match = details.match(/Температурне порушення на пристрої (.*?):\s*([\d.-]+)°C\s*\(норма:\s*([\d.-]+)–([\d.-]+)°C\)/i);
  if (match) return t('notificationMessageTempViolation', { deviceId: match[1], temp: match[2], min: match[3], max: match[4] });

  match = details.match(/Порушення вологості на пристрої (.*?):\s*([\d.-]+)%\s*\(норма:\s*([\d.-]+)–([\d.-]+)%\)/i);
  if (match) return t('notificationMessageHumidityViolation', { deviceId: match[1], humidity: match[2], min: match[3], max: match[4] });

  match = details.match(/Температура нормалізована на пристрої (.*?):\s*([\d.-]+)°C\.\s*Інцидент\s*#(\d+)\s*закрито\s*автоматично/i);
  if (match) return t('notificationMessageTempRestored', { deviceId: match[1], temp: match[2], incidentId: match[3] });

  match = details.match(/Вологість нормалізована на пристрої (.*?):\s*([\d.-]+)%\.\s*Інцидент\s*#(\d+)\s*закрито\s*автоматично/i);
  if (match) return t('notificationMessageHumidityRestored', { deviceId: match[1], humidity: match[2], incidentId: match[3] });

  match = details.match(/Препарат «(.*?)» \(ID:\s*(\d+)\)\s*закінчується\s*([\d-]+)\s*\(через\s*(\d+)\s*д\.\)/i);
  if (match) return t('notificationMessageExpiry', { name: match[1], id: match[2], date: match[3], days: match[4] });

  return details;
}

export default function AuditLogPage() {
  const { t, locale } = useLocale();
  const [filters, setFilters] = useState<Filters>({ from: '', to: '', user: '', action: '' });
  const [applied, setApplied] = useState<Partial<Filters>>({});

  const {
    data: logs = [],
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['audit-log', applied],
    queryFn: () => {
      let fromIso, toIso;
      // We pass the local date as a simple YYYY-MM-DD string,
      // because we want the backend to do a naive date comparison
      // ignoring timezone shifts.
      if (applied.from) {
        fromIso = applied.from; // '2026-04-29'
      }
      if (applied.to) {
        toIso = applied.to; // '2026-04-29'
      }
      return auditApi.getLogs({
        from: fromIso,
        to: toIso,
        user: applied.user || undefined,
        action: applied.action || undefined,
      });
    },
  });

  const applyFilters = () => setApplied({ ...filters });
  const resetFilters = () => {
    setFilters({ from: '', to: '', user: '', action: '' });
    setApplied({});
  };

  const f = (k: keyof Filters, v: string) => setFilters((p) => ({ ...p, [k]: v }));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">{t('auditLogTitle')}</h1>
        <p className="text-muted-foreground">{t('auditLogSubtitle')}</p>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium">{t('filters')}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="space-y-1.5">
              <Label className="text-xs">{t('fromDate')}</Label>
              <Input
                type={filters.from ? 'date' : 'text'}
                placeholder={locale === 'uk' ? 'дд.мм.рррр' : 'dd.mm.yyyy'}
                onFocus={(e) => {
                  e.target.type = 'date';
                }}
                onBlur={(e) => {
                  if (!e.target.value) e.target.type = 'text';
                }}
                value={filters.from}
                onChange={(e) => f('from', e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">{t('toDate')}</Label>
              <Input
                type={filters.to ? 'date' : 'text'}
                placeholder={locale === 'uk' ? 'дд.мм.рррр' : 'dd.mm.yyyy'}
                onFocus={(e) => {
                  e.target.type = 'date';
                }}
                onBlur={(e) => {
                  if (!e.target.value) e.target.type = 'text';
                }}
                value={filters.to}
                onChange={(e) => f('to', e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">{t('userSearch')}</Label>
              <Input
                placeholder="email@example.com"
                value={filters.user}
                onChange={(e) => f('user', e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">{t('actionSearch')}</Label>
              <select
                className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                value={filters.action}
                onChange={(e) => f('action', e.target.value)}>
                <option value="">{t('filterAll')}</option>
                {AUDIT_ACTIONS.map((act) => (
                  <option key={act} value={act}>
                    {translateAuditAction(act, t)}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="mt-3 flex gap-2">
            <Button size="sm" onClick={applyFilters}>
              <Search className="h-3.5 w-3.5" /> {t('apply')}
            </Button>
            <Button size="sm" variant="outline" onClick={resetFilters}>
              <RotateCcw className="h-3.5 w-3.5" /> {t('reset')}
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Log Table */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">{t('recordsCount', { count: logs.length })}</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-4 space-y-2">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} className="h-10" />
              ))}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('colDateTime')}</TableHead>
                  <TableHead>{t('colAction')}</TableHead>
                  <TableHead>{t('colUser')}</TableHead>
                  <TableHead>{t('colDetails')}</TableHead>
                  <TableHead>{t('colEntity')}</TableHead>
                  <TableHead>{t('colLevel')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {logs.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="py-10 text-center text-muted-foreground">
                      {t('noRecordsFound')}
                    </TableCell>
                  </TableRow>
                ) : (
                  logs.map((log) => (
                    <TableRow key={log.id}>
                      <TableCell className="whitespace-nowrap text-xs text-muted-foreground">
                        {format(new Date(log.timestamp), 'dd.MM.yyyy HH:mm:ss')}
                      </TableCell>
                      <TableCell className="font-medium text-sm">
                        {translateAuditAction(log.action, t)}
                      </TableCell>
                      <TableCell className="text-sm">{log.user}</TableCell>
                      <TableCell className="max-w-xs truncate text-sm text-muted-foreground">
                        {translateAuditDetails(log.details, t)}
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground">
                        {log.entityType ? `${translateAuditEntity(log.entityType, t)} #${log.entityId}` : '—'}
                      </TableCell>
                      <TableCell>
                        <Badge variant={severityVariant(log.severity)}>
                          {translateAuditSeverity(log.severity, t)}
                        </Badge>
                      </TableCell>
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

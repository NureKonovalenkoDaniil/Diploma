import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { auditApi } from '@/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'
import { Search, RotateCcw } from 'lucide-react'
import { format } from 'date-fns'

type Filters = { from: string; to: string; user: string; action: string }

const severityVariant = (s: string): 'destructive' | 'warning' | 'info' => {
  if (s === 'Error') return 'destructive'
  if (s === 'Warning') return 'warning'
  return 'info'
}

export default function AuditLogPage() {
  const [filters, setFilters] = useState<Filters>({ from: '', to: '', user: '', action: '' })
  const [applied, setApplied] = useState<Partial<Filters>>({})

  const { data: logs = [], isLoading, refetch } = useQuery({
    queryKey: ['audit-log', applied],
    queryFn: () => auditApi.getLogs({
      from: applied.from || undefined,
      to: applied.to || undefined,
      user: applied.user || undefined,
      action: applied.action || undefined,
    }),
  })

  const applyFilters = () => setApplied({ ...filters })
  const resetFilters = () => {
    setFilters({ from: '', to: '', user: '', action: '' })
    setApplied({})
  }

  const f = (k: keyof Filters, v: string) => setFilters((p) => ({ ...p, [k]: v }))

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Журнал аудиту</h1>
        <p className="text-muted-foreground">Повний журнал дій у системі</p>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader><CardTitle className="text-sm font-medium">Фільтри</CardTitle></CardHeader>
        <CardContent>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="space-y-1.5">
              <Label className="text-xs">З дати</Label>
              <Input type="datetime-local" value={filters.from} onChange={(e) => f('from', e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">По дату</Label>
              <Input type="datetime-local" value={filters.to} onChange={(e) => f('to', e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Користувач</Label>
              <Input placeholder="email@example.com" value={filters.user} onChange={(e) => f('user', e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Дія</Label>
              <Input placeholder="Create Medicine" value={filters.action} onChange={(e) => f('action', e.target.value)} />
            </div>
          </div>
          <div className="mt-3 flex gap-2">
            <Button size="sm" onClick={applyFilters}>
              <Search className="h-3.5 w-3.5" /> Застосувати
            </Button>
            <Button size="sm" variant="outline" onClick={resetFilters}>
              <RotateCcw className="h-3.5 w-3.5" /> Скинути
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Log Table */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">{logs.length} записів</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-4 space-y-2">{Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-10" />)}</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Дата/час</TableHead>
                  <TableHead>Дія</TableHead>
                  <TableHead>Користувач</TableHead>
                  <TableHead>Деталі</TableHead>
                  <TableHead>Сутність</TableHead>
                  <TableHead>Рівень</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {logs.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="py-10 text-center text-muted-foreground">
                      Записів не знайдено
                    </TableCell>
                  </TableRow>
                ) : logs.map((log) => (
                  <TableRow key={log.id}>
                    <TableCell className="whitespace-nowrap text-xs text-muted-foreground">
                      {format(new Date(log.timestamp), 'dd.MM.yyyy HH:mm:ss')}
                    </TableCell>
                    <TableCell className="font-medium text-sm">{log.action}</TableCell>
                    <TableCell className="text-sm">{log.user}</TableCell>
                    <TableCell className="max-w-xs truncate text-sm text-muted-foreground">{log.details}</TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {log.entityType ? `${log.entityType} #${log.entityId}` : '—'}
                    </TableCell>
                    <TableCell>
                      <Badge variant={severityVariant(log.severity)}>{log.severity}</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

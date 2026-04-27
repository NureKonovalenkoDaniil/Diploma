import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { incidentApi } from '@/api'
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'
import { AlertTriangle, CheckCircle, Clock } from 'lucide-react'
import { format } from 'date-fns'

type TabType = 'active' | 'all'

export default function IncidentsPage() {
  const { isAdmin } = useAuth()
  const qc = useQueryClient()
  const [tab, setTab] = useState<TabType>('active')

  const { data: active = [], isLoading: aLoading } = useQuery({
    queryKey: ['incidents', 'active'],
    queryFn: incidentApi.getActive,
    refetchInterval: 30_000,
  })

  const { data: all = [], isLoading: allLoading } = useQuery({
    queryKey: ['incidents', 'all'],
    queryFn: incidentApi.getAll,
    enabled: tab === 'all',
  })

  const resolveMutation = useMutation({
    mutationFn: (id: number) => incidentApi.resolve(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['incidents'] })
    },
  })

  const incidents = tab === 'active' ? active : all
  const isLoading = tab === 'active' ? aLoading : allLoading

  const incidentTypeLabel = (t: string) =>
    t === 'TemperatureViolation' ? '🌡️ Температура' : '💧 Вологість'

  const statusVariant = (s: string) => {
    if (s === 'Active') return 'destructive'
    if (s === 'Resolved') return 'success'
    return 'secondary'
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Інциденти зберігання</h1>
        <p className="text-muted-foreground">Моніторинг порушень умов зберігання</p>
      </div>

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-destructive/10 p-3"><AlertTriangle className="h-5 w-5 text-destructive" /></div>
            <div><p className="text-2xl font-bold">{active.length}</p><p className="text-xs text-muted-foreground">Активних інцидентів</p></div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-amber-500/10 p-3"><Clock className="h-5 w-5 text-amber-500" /></div>
            <div>
              <p className="text-2xl font-bold">{all.filter((i) => i.status === 'Acknowledged').length}</p>
              <p className="text-xs text-muted-foreground">Підтверджено</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-emerald-500/10 p-3"><CheckCircle className="h-5 w-5 text-emerald-500" /></div>
            <div>
              <p className="text-2xl font-bold">{all.filter((i) => i.status === 'Resolved').length}</p>
              <p className="text-xs text-muted-foreground">Вирішено</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Tabs */}
      <div className="flex gap-2">
        <Button variant={tab === 'active' ? 'default' : 'outline'} size="sm" onClick={() => setTab('active')}>
          Активні ({active.length})
        </Button>
        <Button variant={tab === 'all' ? 'default' : 'outline'} size="sm" onClick={() => setTab('all')}>
          Всі інциденти
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">
            {tab === 'active' ? 'Активні порушення' : 'Всі інциденти'}
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-4 space-y-2">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-10" />)}</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Пристрій</TableHead>
                  <TableHead>Тип</TableHead>
                  <TableHead>Виявлене значення</TableHead>
                  <TableHead>Норма</TableHead>
                  <TableHead>Статус</TableHead>
                  <TableHead>Початок</TableHead>
                  <TableHead>Кінець</TableHead>
                  {isAdmin && <TableHead>Дії</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {incidents.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={isAdmin ? 8 : 7} className="py-10 text-center text-muted-foreground">
                      {tab === 'active' ? '✅ Жодних активних порушень' : 'Інцидентів немає'}
                    </TableCell>
                  </TableRow>
                ) : incidents.map((inc) => (
                  <TableRow key={inc.incidentId}>
                    <TableCell>
                      <div className="font-medium">{inc.deviceLocation}</div>
                      {inc.locationName && <div className="text-xs text-muted-foreground">{inc.locationName}</div>}
                    </TableCell>
                    <TableCell><Badge variant="outline">{incidentTypeLabel(inc.incidentType)}</Badge></TableCell>
                    <TableCell>
                      <span className={`font-semibold ${inc.status === 'Active' ? 'text-destructive' : ''}`}>
                        {inc.detectedValue.toFixed(1)}
                        {inc.incidentType === 'TemperatureViolation' ? '°C' : '%'}
                      </span>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {inc.expectedMin} – {inc.expectedMax}
                      {inc.incidentType === 'TemperatureViolation' ? '°C' : '%'}
                    </TableCell>
                    <TableCell>
                      <Badge variant={statusVariant(inc.status) as 'destructive' | 'success' | 'secondary'}>
                        {inc.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-sm">{format(new Date(inc.startTime), 'dd.MM HH:mm')}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {inc.endTime ? format(new Date(inc.endTime), 'dd.MM HH:mm') : '—'}
                    </TableCell>
                    {isAdmin && (
                      <TableCell>
                        {inc.status === 'Active' && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => resolveMutation.mutate(inc.incidentId)}
                            disabled={resolveMutation.isPending}
                          >
                            Закрити
                          </Button>
                        )}
                      </TableCell>
                    )}
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

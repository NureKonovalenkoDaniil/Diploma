import { useQuery } from '@tanstack/react-query'
import { Pill, Cpu, AlertTriangle, Bell, TrendingDown, Package } from 'lucide-react'
import { medicineApi, iotApi, incidentApi, notificationApi } from '@/api'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts'
import { format } from 'date-fns'
import { useAuth } from '@/contexts/AuthContext'

function StatCard({
  title,
  value,
  icon: Icon,
  description,
  variant = 'default',
}: {
  title: string
  value: number | string
  icon: React.ElementType
  description?: string
  variant?: 'default' | 'warning' | 'danger'
}) {
  const iconColors = {
    default: 'text-primary bg-primary/10',
    warning: 'text-amber-500 bg-amber-500/10',
    danger: 'text-destructive bg-destructive/10',
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        <div className={`rounded-lg p-2 ${iconColors[variant]}`}>
          <Icon className="h-4 w-4" />
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-3xl font-bold">{value}</p>
        {description && <p className="mt-1 text-xs text-muted-foreground">{description}</p>}
      </CardContent>
    </Card>
  )
}

export default function DashboardPage() {
  const { isAdmin } = useAuth()

  const { data: medicines = [], isLoading: mLoading } = useQuery({
    queryKey: ['medicines'],
    queryFn: medicineApi.getAll,
  })

  const { data: devices = [], isLoading: dLoading } = useQuery({
    queryKey: ['iot-devices'],
    queryFn: iotApi.getAll,
  })

  const { data: activeIncidents = [], isLoading: iLoading } = useQuery({
    queryKey: ['incidents', 'active'],
    queryFn: incidentApi.getActive,
    refetchInterval: 60_000,
  })

  const { data: unread = [] } = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: notificationApi.getUnread,
    refetchInterval: 30_000,
  })

  const { data: lowStock = [] } = useQuery({
    queryKey: ['medicines', 'low-stock'],
    queryFn: () => medicineApi.getLowStock(),
    enabled: isAdmin,
  })

  // Build chart data from last conditions of active devices
  const { data: conditionsData = [] } = useQuery({
    queryKey: ['conditions-chart'],
    queryFn: async () => {
      if (devices.length === 0) return []
      const firstDevice = devices[0]
      const conds = await iotApi.getConditions(firstDevice.deviceID)
      return conds
        .slice(-24)
        .map((c) => ({
          time: format(new Date(c.timestamp), 'HH:mm'),
          Температура: c.temperature,
          Вологість: c.humidity,
        }))
    },
    enabled: devices.length > 0,
  })

  const isLoading = mLoading || dLoading || iLoading
  const activeDevices = devices.filter((d) => d.isActive).length

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Дашборд</h1>
        <p className="text-muted-foreground">Огляд системи моніторингу медичних препаратів</p>
      </div>

      {/* Stat Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {isLoading ? (
          Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-28" />)
        ) : (
          <>
            <StatCard title="Препарати" value={medicines.length} icon={Pill} description="Всього у системі" />
            <StatCard
              title="Активні пристрої"
              value={`${activeDevices} / ${devices.length}`}
              icon={Cpu}
              description="IoT датчиків онлайн"
            />
            <StatCard
              title="Активні інциденти"
              value={activeIncidents.length}
              icon={AlertTriangle}
              variant={activeIncidents.length > 0 ? 'danger' : 'default'}
              description="Порушення умов зберігання"
            />
            <StatCard
              title="Непрочитані"
              value={unread.length}
              icon={Bell}
              variant={unread.length > 0 ? 'warning' : 'default'}
              description="Нові сповіщення"
            />
          </>
        )}
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Chart */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-base">Умови зберігання</CardTitle>
            <CardDescription>
              {devices[0] ? `Пристрій: ${devices[0].location}` : 'Температура та вологість'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {conditionsData.length === 0 ? (
              <div className="flex h-48 items-center justify-center text-muted-foreground text-sm">
                Немає даних для відображення
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={220}>
                <AreaChart data={conditionsData}>
                  <defs>
                    <linearGradient id="temp" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="hsl(221.2 83.2% 53.3%)" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="hsl(221.2 83.2% 53.3%)" stopOpacity={0} />
                    </linearGradient>
                    <linearGradient id="hum" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="hsl(160 60% 45%)" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="hsl(160 60% 45%)" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
                  <XAxis dataKey="time" tick={{ fontSize: 11 }} className="text-muted-foreground" />
                  <YAxis tick={{ fontSize: 11 }} className="text-muted-foreground" />
                  <Tooltip
                    contentStyle={{
                      background: 'hsl(var(--popover))',
                      border: '1px solid hsl(var(--border))',
                      borderRadius: '8px',
                      fontSize: '12px',
                    }}
                  />
                  <Legend wrapperStyle={{ fontSize: '12px' }} />
                  <Area
                    type="monotone"
                    dataKey="Температура"
                    stroke="hsl(221.2 83.2% 53.3%)"
                    fill="url(#temp)"
                    strokeWidth={2}
                  />
                  <Area
                    type="monotone"
                    dataKey="Вологість"
                    stroke="hsl(160 60% 45%)"
                    fill="url(#hum)"
                    strokeWidth={2}
                  />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        {/* Active Incidents */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Активні інциденти</CardTitle>
            <CardDescription>{activeIncidents.length} активних порушень</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {activeIncidents.length === 0 ? (
              <div className="py-8 text-center text-sm text-muted-foreground">
                ✅ Жодних порушень
              </div>
            ) : (
              activeIncidents.slice(0, 5).map((inc) => (
                <div key={inc.incidentId} className="rounded-lg border p-3 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-medium">{inc.deviceLocation}</span>
                    <Badge variant="destructive" className="text-[10px]">
                      {inc.incidentType === 'TemperatureViolation' ? '🌡️ Темп.' : '💧 Вол.'}
                    </Badge>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {inc.detectedValue.toFixed(1)} (норма: {inc.expectedMin}–{inc.expectedMax})
                  </p>
                  <p className="text-[10px] text-muted-foreground/60">
                    {format(new Date(inc.startTime), 'dd.MM HH:mm')}
                  </p>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </div>

      {/* Low Stock */}
      {isAdmin && lowStock.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Package className="h-4 w-4 text-amber-500" />
              <CardTitle className="text-base">Препарати з низьким запасом</CardTitle>
            </div>
            <CardDescription>{lowStock.length} препаратів потребують поповнення</CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Назва</TableHead>
                  <TableHead>Тип</TableHead>
                  <TableHead>К-сть</TableHead>
                  <TableHead>Локація</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {lowStock.slice(0, 8).map((m) => (
                  <TableRow key={m.medicineID}>
                    <TableCell className="font-medium">{m.name}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{m.type}</Badge>
                    </TableCell>
                    <TableCell>
                      <span className="text-amber-600 font-semibold">{m.quantity}</span>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{m.storageLocationName ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {/* Low Stock empty state */}
      {isAdmin && lowStock.length === 0 && !isLoading && (
        <Card>
          <CardContent className="flex items-center gap-3 py-4">
            <TrendingDown className="h-5 w-5 text-emerald-500" />
            <p className="text-sm text-muted-foreground">Всі препарати мають достатній запас</p>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

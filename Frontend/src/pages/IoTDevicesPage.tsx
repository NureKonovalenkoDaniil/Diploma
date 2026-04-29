import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, Fragment } from 'react'
import { useNavigate } from 'react-router-dom'
import { iotApi } from '@/api'
import type { IoTDeviceDto } from '@/types/api'
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'
import { Cpu, Activity, ChevronRight, Power, Trash2 } from 'lucide-react'
import { format } from 'date-fns'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export default function IoTDevicesPage() {
  const { isAdmin, isManager } = useAuth()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [newDevice, setNewDevice] = useState({ deviceID: '', location: '', type: '', minTemp: 2, maxTemp: 8, minHum: 30, maxHum: 60 })

  const { data: devices = [], isLoading } = useQuery({
    queryKey: ['iot-devices'],
    queryFn: iotApi.getAll,
    refetchInterval: 30_000,
  })

  const toggleMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      iotApi.setStatus(id, active),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['iot-devices'] }),
  })

  const registerMutation = useMutation({
    mutationFn: (device: any) => iotApi.create(device),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['iot-devices'] })
      setIsDialogOpen(false)
      setNewDevice({ deviceID: '', location: '', type: '', minTemp: 2, maxTemp: 8, minHum: 30, maxHum: 60 })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => iotApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['iot-devices'] }),
  })

  const [expanded, setExpanded] = useState<string | null>(null)

  const { data: conditions = [], isFetching: condFetching } = useQuery({
    queryKey: ['conditions', expanded],
    queryFn: () => iotApi.getConditions(expanded!),
    enabled: expanded !== null,
  })

  const activeCount = devices.filter((d) => d.isActive).length

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">IoT Пристрої</h1>
          <p className="text-muted-foreground">{activeCount} з {devices.length} активних датчиків</p>
        </div>
        {(isAdmin || isManager) && (
          <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
            <DialogTrigger asChild>
              <Button>Зареєструвати пристрій</Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Реєстрація нового IoT-пристрою</DialogTitle>
                <DialogDescription>
                  Введіть серійний номер датчика (DeviceId) та параметри.
                </DialogDescription>
              </DialogHeader>
              <div className="grid gap-4 py-4">
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="deviceId" className="text-right">DeviceId</Label>
                  <Input id="deviceId" value={newDevice.deviceID} onChange={e => setNewDevice({...newDevice, deviceID: e.target.value})} className="col-span-3" placeholder="Наприклад, ESP-8800" />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="location" className="text-right">Розташування</Label>
                  <Input id="location" value={newDevice.location} onChange={e => setNewDevice({...newDevice, location: e.target.value})} className="col-span-3" placeholder="Холодильник 1" />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="type" className="text-right">Тип</Label>
                  <Input id="type" value={newDevice.type} onChange={e => setNewDevice({...newDevice, type: e.target.value})} className="col-span-3" placeholder="DHT22" />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label className="text-right">Темп. (°C)</Label>
                  <div className="col-span-3 flex gap-2">
                    <Input type="number" value={newDevice.minTemp} onChange={e => setNewDevice({...newDevice, minTemp: Number(e.target.value)})} placeholder="Min" />
                    <Input type="number" value={newDevice.maxTemp} onChange={e => setNewDevice({...newDevice, maxTemp: Number(e.target.value)})} placeholder="Max" />
                  </div>
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label className="text-right">Вологість (%)</Label>
                  <div className="col-span-3 flex gap-2">
                    <Input type="number" value={newDevice.minHum} onChange={e => setNewDevice({...newDevice, minHum: Number(e.target.value)})} placeholder="Min" />
                    <Input type="number" value={newDevice.maxHum} onChange={e => setNewDevice({...newDevice, maxHum: Number(e.target.value)})} placeholder="Max" />
                  </div>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsDialogOpen(false)}>Скасувати</Button>
                <Button onClick={() => registerMutation.mutate({
                  deviceID: newDevice.deviceID,
                  location: newDevice.location,
                  type: newDevice.type,
                  minTemperature: newDevice.minTemp,
                  maxTemperature: newDevice.maxTemp,
                  minHumidity: newDevice.minHum,
                  maxHumidity: newDevice.maxHum,
                  isActive: true,
                  parameters: "{}"
                })} disabled={!newDevice.deviceID || registerMutation.isPending}>
                  Зареєструвати
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        )}
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-primary/10 p-3"><Cpu className="h-5 w-5 text-primary" /></div>
            <div><p className="text-2xl font-bold">{devices.length}</p><p className="text-xs text-muted-foreground">Всього пристроїв</p></div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-emerald-500/10 p-3"><Activity className="h-5 w-5 text-emerald-500" /></div>
            <div><p className="text-2xl font-bold">{activeCount}</p><p className="text-xs text-muted-foreground">Онлайн</p></div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-destructive/10 p-3"><Power className="h-5 w-5 text-destructive" /></div>
            <div><p className="text-2xl font-bold">{devices.length - activeCount}</p><p className="text-xs text-muted-foreground">Офлайн</p></div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Список пристроїв</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-4 space-y-2">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-10" />)}</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Розташування</TableHead>
                  <TableHead>Тип</TableHead>
                  <TableHead>Статус</TableHead>
                  <TableHead>Діапазон темп.</TableHead>
                  <TableHead>Діапазон вол.</TableHead>
                  {(isAdmin || isManager) && <TableHead>Дії</TableHead>}
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {devices.map((d) => (
                  <Fragment key={d.deviceID}>
                    <TableRow className="cursor-pointer" onClick={() => setExpanded(expanded === d.deviceID ? null : d.deviceID)}>
                      <TableCell className="font-mono text-xs">#{d.deviceID}</TableCell>
                      <TableCell className="font-medium">{d.location}</TableCell>
                      <TableCell><Badge variant="outline">{d.type}</Badge></TableCell>
                      <TableCell>
                        <Badge variant={d.isActive ? 'success' : 'secondary'}>
                          {d.isActive ? 'Активний' : 'Неактивний'}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm">{d.minTemperature}°C – {d.maxTemperature}°C</TableCell>
                      <TableCell className="text-sm">{d.minHumidity}% – {d.maxHumidity}%</TableCell>
                      {(isAdmin || isManager) && (
                        <TableCell onClick={(e) => e.stopPropagation()} className="flex items-center gap-1">
                          <Button
                            variant={d.isActive ? 'outline' : 'default'}
                            size="sm"
                            onClick={() => toggleMutation.mutate({ id: d.deviceID, active: !d.isActive })}
                          >
                            {d.isActive ? 'Вимкнути' : 'Увімкнути'}
                          </Button>
                          <AlertDialog>
                            <AlertDialogTrigger asChild>
                              <Button variant="ghost" size="icon" className="text-destructive hover:text-destructive">
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                              <AlertDialogHeader>
                                <AlertDialogTitle>Видалити пристрій?</AlertDialogTitle>
                                <AlertDialogDescription>
                                  Пристрій <strong>{d.deviceID}</strong> ({d.location}) буде назавжди видалено з системи. Цю дію неможливо скасувати.
                                </AlertDialogDescription>
                              </AlertDialogHeader>
                              <AlertDialogFooter>
                                <AlertDialogCancel>Скасувати</AlertDialogCancel>
                                <AlertDialogAction
                                  className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                  onClick={() => deleteMutation.mutate(d.deviceID)}
                                >
                                  Видалити
                                </AlertDialogAction>
                              </AlertDialogFooter>
                            </AlertDialogContent>
                          </AlertDialog>
                        </TableCell>
                      )}
                      <TableCell>
                        <ChevronRight className={`h-4 w-4 text-muted-foreground transition-transform ${expanded === d.deviceID ? 'rotate-90' : ''}`} />
                      </TableCell>
                    </TableRow>
                    {expanded === d.deviceID && (
                      <TableRow key={`${d.deviceID}-exp`}>
                        <TableCell colSpan={(isAdmin || isManager) ? 8 : 7} className="bg-muted/30 p-4">
                          <p className="mb-2 text-sm font-medium">Останні показники умов зберігання</p>
                          {condFetching ? (
                            <Skeleton className="h-20" />
                          ) : conditions.length === 0 ? (
                            <p className="text-sm text-muted-foreground">Немає даних</p>
                          ) : (
                            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                              {conditions.slice(-4).map((c) => (
                                <div key={c.conditionID} className="rounded-lg border bg-card p-3">
                                  <p className="text-xs text-muted-foreground">{format(new Date(c.timestamp), 'dd.MM HH:mm')}</p>
                                  <p className="text-sm font-semibold">🌡️ {c.temperature.toFixed(1)}°C</p>
                                  <p className="text-sm font-semibold">💧 {c.humidity.toFixed(1)}%</p>
                                </div>
                              ))}
                            </div>
                          )}
                        </TableCell>
                      </TableRow>
                    )}
                  </Fragment>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

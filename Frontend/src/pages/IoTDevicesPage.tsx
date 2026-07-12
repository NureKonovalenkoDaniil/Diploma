import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, Fragment } from 'react';
import { useNavigate } from 'react-router-dom';
import { iotApi, locationApi } from '@/api';
import type { IoTDeviceDto } from '@/types/api';
import { useAuth } from '@/contexts/AuthContext';
import { useLocale } from '@/contexts/LocaleContext';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Skeleton } from '@/components/ui/skeleton';
import { Cpu, Activity, ChevronRight, Power, Trash2, Pencil, Search } from 'lucide-react';
import { format } from 'date-fns';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
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
} from '@/components/ui/alert-dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export default function IoTDevicesPage() {
  const { isAdmin, isManager, isUser } = useAuth();
  const { t } = useLocale();
  const canManage = isAdmin || isManager || isUser;
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [newDevice, setNewDevice] = useState({
    deviceID: '',
    location: '',
    type: '',
    minTemp: 2,
    maxTemp: 8,
    minHum: 30,
    maxHum: 60,
  });

  const { data: locations = [] } = useQuery({
    queryKey: ['locations'],
    queryFn: locationApi.getAll,
  });

  const { data: devices = [], isLoading } = useQuery({
    queryKey: ['iot-devices'],
    queryFn: iotApi.getAll,
    refetchInterval: 30_000,
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => iotApi.setStatus(id, active),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['iot-devices'] }),
  });

  const registerMutation = useMutation({
    mutationFn: (device: any) => iotApi.create(device),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['iot-devices'] });
      qc.invalidateQueries({ queryKey: ['locations'] });
      setIsDialogOpen(false);
      setNewDevice({
        deviceID: '',
        location: '',
        type: '',
        minTemp: 2,
        maxTemp: 8,
        minHum: 30,
        maxHum: 60,
      });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => iotApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['iot-devices'] });
      qc.invalidateQueries({ queryKey: ['locations'] });
    },
  });

  const [expanded, setExpanded] = useState<string | null>(null);
  const [editingDevice, setEditingDevice] = useState<IoTDeviceDto | null>(null);

  const { data: conditions = [], isFetching: condFetching } = useQuery({
    queryKey: ['conditions', expanded],
    queryFn: () => iotApi.getConditions(expanded!),
    enabled: expanded !== null,
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<IoTDeviceDto> }) => {
      const patchData = Object.entries(data).map(([path, value]) => ({
        op: 'replace',
        path: `/${path}`,
        value,
      }));
      return iotApi.update(id, patchData);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['iot-devices'] });
      qc.invalidateQueries({ queryKey: ['locations'] });
      setEditingDevice(null);
    },
  });

  const activeCount = devices.filter((d) => d.isActive).length;
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<'all' | 'active' | 'inactive'>('all');

  const filteredDevices = devices.filter((d) => {
    const q = search.toLowerCase();
    const matchSearch =
      !q ||
      d.deviceID.toLowerCase().includes(q) ||
      d.location.toLowerCase().includes(q) ||
      (d.type ?? '').toLowerCase().includes(q);
    const matchStatus =
      statusFilter === 'all' ||
      (statusFilter === 'active' && d.isActive) ||
      (statusFilter === 'inactive' && !d.isActive);
    return matchSearch && matchStatus;
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t('iotDevicesTitle')}</h1>
          <p className="text-muted-foreground">
            {t('iotDevicesSubtitle', { active: activeCount, total: devices.length })}
          </p>
        </div>
        {canManage && (
          <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
            <DialogTrigger asChild>
              <Button>{t('registerDevice')}</Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>{t('registerDeviceTitle')}</DialogTitle>
                <DialogDescription>
                  {t('registerDeviceSubtitle')}
                </DialogDescription>
              </DialogHeader>
              <div className="grid gap-4 py-4">
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="deviceId" className="text-right">
                    {t('deviceId')}
                  </Label>
                  <Input
                    id="deviceId"
                    value={newDevice.deviceID}
                    onChange={(e) => setNewDevice({ ...newDevice, deviceID: e.target.value })}
                    className="col-span-3"
                    placeholder={t('deviceIdPlaceholder')}
                  />
                </div>
                 <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="location" className="text-right">
                    {t('deviceLocation')}
                  </Label>
                  <select
                    id="location"
                    value={newDevice.location}
                    onChange={(e) => setNewDevice({ ...newDevice, location: e.target.value })}
                    className="col-span-3 flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm text-foreground">
                    <option value="">{t('chooseLocationOption')}</option>
                    {locations.map((loc) => (
                      <option key={loc.locationId} value={loc.name}>
                        {loc.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="type" className="text-right">
                    {t('deviceType')}
                  </Label>
                  <Input
                    id="type"
                    value={newDevice.type}
                    onChange={(e) => setNewDevice({ ...newDevice, type: e.target.value })}
                    className="col-span-3"
                    placeholder="DHT22"
                  />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label className="text-right">{t('deviceTempLabel')}</Label>
                  <div className="col-span-3 flex gap-2">
                    <Input
                      type="text"
                      value={newDevice.minTemp}
                      onChange={(e) => {
                        const val = e.target.value;
                        if (val === '' || val === '-' || val === '.' || val === '-.' || !isNaN(Number(val))) {
                          setNewDevice({ ...newDevice, minTemp: val as any });
                        }
                      }}
                      placeholder="Min"
                    />
                    <Input
                      type="text"
                      value={newDevice.maxTemp}
                      onChange={(e) => {
                        const val = e.target.value;
                        if (val === '' || val === '-' || val === '.' || val === '-.' || !isNaN(Number(val))) {
                          setNewDevice({ ...newDevice, maxTemp: val as any });
                        }
                      }}
                      placeholder="Max"
                    />
                  </div>
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label className="text-right">{t('deviceHumidityLabel')}</Label>
                  <div className="col-span-3 flex gap-2">
                    <Input
                      type="number"
                      value={newDevice.minHum}
                      onChange={(e) =>
                        setNewDevice({ ...newDevice, minHum: Number(e.target.value) })
                      }
                      placeholder="Min"
                    />
                    <Input
                      type="number"
                      value={newDevice.maxHum}
                      onChange={(e) =>
                        setNewDevice({ ...newDevice, maxHum: Number(e.target.value) })
                      }
                      placeholder="Max"
                    />
                  </div>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                  {t('cancel')}
                </Button>
                <Button
                  onClick={() =>
                    registerMutation.mutate({
                      deviceID: newDevice.deviceID,
                      location: newDevice.location,
                      type: newDevice.type,
                      minTemperature: (newDevice.minTemp as any) !== '' && (newDevice.minTemp as any) !== '-' ? Number(newDevice.minTemp) : 0,
                      maxTemperature: (newDevice.maxTemp as any) !== '' && (newDevice.maxTemp as any) !== '-' ? Number(newDevice.maxTemp) : 0,
                      minHumidity: newDevice.minHum,
                      maxHumidity: newDevice.maxHum,
                      isActive: true,
                      parameters: '{}',
                    })
                  }
                  disabled={!newDevice.deviceID || registerMutation.isPending}>
                  {t('registerDevice')}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        )}
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-primary/10 p-3">
              <Cpu className="h-5 w-5 text-primary" />
            </div>
            <div>
              <p className="text-2xl font-bold">{devices.length}</p>
              <p className="text-xs text-muted-foreground">{t('totalDevicesCount')}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-emerald-500/10 p-3">
              <Activity className="h-5 w-5 text-emerald-500" />
            </div>
            <div>
              <p className="text-2xl font-bold">{activeCount}</p>
              <p className="text-xs text-muted-foreground">{t('onlineCount')}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-destructive/10 p-3">
              <Power className="h-5 w-5 text-destructive" />
            </div>
            <div>
              <p className="text-2xl font-bold">{devices.length - activeCount}</p>
              <p className="text-xs text-muted-foreground">{t('offlineCount')}</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <div className="flex flex-wrap items-center gap-3">
            <CardTitle className="text-base flex-1">{t('devicesListTitle')}</CardTitle>
            <div className="relative min-w-48">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder={t('deviceSearch')}
                className="pl-9 h-9"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="text-xs text-muted-foreground whitespace-nowrap">{t('filterByStatus')}:</label>
              <select
                aria-label={t('filterByStatus')}
                className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as 'all' | 'active' | 'inactive')}>
                <option value="all">{t('filterAll')}</option>
                <option value="active">{t('filterActive')}</option>
                <option value="inactive">{t('filterInactive')}</option>
              </select>
            </div>
            <span className="text-xs text-muted-foreground">
              {t('filteredCount', { filtered: filteredDevices.length, total: devices.length })}
            </span>
          </div>
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
                  <TableHead>ID</TableHead>
                  <TableHead>{t('colDeviceLocation')}</TableHead>
                  <TableHead>{t('colDeviceType')}</TableHead>
                  <TableHead>{t('colDeviceStatus')}</TableHead>
                  <TableHead>{t('colDeviceTempRange')}</TableHead>
                  <TableHead>{t('colDeviceHumidityRange')}</TableHead>
                  {canManage && <TableHead>{t('colDeviceActions')}</TableHead>}
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredDevices.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={canManage ? 8 : 7} className="py-10 text-center text-muted-foreground">
                      {devices.length === 0 ? t('noMetricsData') : t('noItemsMatchFilter')}
                    </TableCell>
                  </TableRow>
                ) : filteredDevices.map((d) => (
                  <Fragment key={d.deviceID}>
                    <TableRow
                       className="cursor-pointer"
                      onClick={() => setExpanded(expanded === d.deviceID ? null : d.deviceID)}>
                      <TableCell className="font-mono text-xs">#{d.deviceID}</TableCell>
                      <TableCell className="font-medium">{d.location === 'Unassigned' ? t('unassigned') : d.location}</TableCell>
                      <TableCell>
                        <Badge variant="outline">{d.type}</Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={d.isActive ? 'success' : 'secondary'}>
                          {d.isActive ? t('statusActiveDevice') : t('statusInactiveDevice')}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm">
                        {d.minTemperature}°C – {d.maxTemperature}°C
                      </TableCell>
                      <TableCell className="text-sm">
                        {d.minHumidity}% – {d.maxHumidity}%
                      </TableCell>
                      {canManage && (
                        <TableCell
                          onClick={(e) => e.stopPropagation()}
                          className="flex items-center gap-1">
                          <Button
                            variant={d.isActive ? 'outline' : 'default'}
                            size="sm"
                            onClick={() =>
                              toggleMutation.mutate({ id: d.deviceID, active: !d.isActive })
                            }>
                            {d.isActive ? t('actionDeactivate') : t('actionActivate')}
                          </Button>
                          <Button variant="ghost" size="icon" onClick={() => setEditingDevice(d)}>
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <AlertDialog>
                            <AlertDialogTrigger asChild>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="text-destructive hover:text-destructive">
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                              <AlertDialogHeader>
                                <AlertDialogTitle>{t('deleteDeviceConfirmTitle')}</AlertDialogTitle>
                                <AlertDialogDescription>
                                  {t('deleteDeviceConfirmText', { id: d.deviceID, location: d.location === 'Unassigned' ? t('unassigned') : d.location })}
                                </AlertDialogDescription>
                              </AlertDialogHeader>
                              <AlertDialogFooter>
                                <AlertDialogCancel>{t('cancel')}</AlertDialogCancel>
                                <AlertDialogAction
                                  className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                  onClick={() => deleteMutation.mutate(d.deviceID)}>
                                  {t('deleteDeviceConfirmBtn')}
                                </AlertDialogAction>
                              </AlertDialogFooter>
                            </AlertDialogContent>
                          </AlertDialog>
                        </TableCell>
                      )}
                      <TableCell>
                        <ChevronRight
                          className={`h-4 w-4 text-muted-foreground transition-transform ${expanded === d.deviceID ? 'rotate-90' : ''}`}
                        />
                      </TableCell>
                    </TableRow>
                    {expanded === d.deviceID && (
                      <TableRow key={`${d.deviceID}-exp`}>
                        <TableCell
                          colSpan={canManage ? 8 : 7}
                          className="bg-muted/30 p-4">
                          <p className="mb-2 text-sm font-medium">
                            {t('latestMetricsTitle')}
                          </p>
                          {condFetching ? (
                            <Skeleton className="h-20" />
                          ) : conditions.length === 0 ? (
                            <p className="text-sm text-muted-foreground">{t('noMetricsData')}</p>
                          ) : (
                            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                              {conditions.slice(-4).map((c) => (
                                <div key={c.conditionID} className="rounded-lg border bg-card p-3">
                                  <p className="text-xs text-muted-foreground">
                                    {format(new Date(c.timestamp), 'dd.MM HH:mm')}
                                  </p>
                                  <p className="text-sm font-semibold">
                                    🌡️ {c.temperature.toFixed(1)}°C
                                  </p>
                                  <p className="text-sm font-semibold">
                                    💧 {c.humidity.toFixed(1)}%
                                  </p>
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
      {/* Edit Device Dialog */}
      {canManage && editingDevice && (
        <Dialog open={!!editingDevice} onOpenChange={(open) => !open && setEditingDevice(null)}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{t('editDeviceTitle', { id: editingDevice.deviceID })}</DialogTitle>
              <DialogDescription>{t('editDeviceSubtitle')}</DialogDescription>
            </DialogHeader>
            <div className="grid gap-4 py-4">
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="edit-location" className="text-right">
                  {t('deviceLocation')}
                </Label>
                <select
                  id="edit-location"
                  value={editingDevice.location}
                  onChange={(e) => setEditingDevice({ ...editingDevice, location: e.target.value })}
                  className="col-span-3 flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm text-foreground">
                  <option value="">{t('chooseLocationOption')}</option>
                  {locations.map((loc) => (
                    <option key={loc.locationId} value={loc.name}>
                      {loc.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="edit-type" className="text-right">
                  {t('deviceType')}
                </Label>
                <Input
                  id="edit-type"
                  value={editingDevice.type}
                  onChange={(e) => setEditingDevice({ ...editingDevice, type: e.target.value })}
                  className="col-span-3"
                />
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label className="text-right">{t('deviceTempLabel')}</Label>
                <div className="col-span-3 flex gap-2">
                  <Input
                    type="text"
                    value={editingDevice.minTemperature}
                    onChange={(e) => {
                      const val = e.target.value;
                      if (val === '' || val === '-' || val === '.' || val === '-.' || !isNaN(Number(val))) {
                        setEditingDevice({ ...editingDevice, minTemperature: val as any });
                      }
                    }}
                    placeholder="Min"
                  />
                  <Input
                    type="text"
                    value={editingDevice.maxTemperature}
                    onChange={(e) => {
                      const val = e.target.value;
                      if (val === '' || val === '-' || val === '.' || val === '-.' || !isNaN(Number(val))) {
                        setEditingDevice({ ...editingDevice, maxTemperature: val as any });
                      }
                    }}
                    placeholder="Max"
                  />
                </div>
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label className="text-right">{t('deviceHumidityLabel')}</Label>
                <div className="col-span-3 flex gap-2">
                  <Input
                    type="number"
                    step="0.1"
                    value={editingDevice.minHumidity}
                    onChange={(e) =>
                      setEditingDevice({ ...editingDevice, minHumidity: Number(e.target.value) })
                    }
                    placeholder="Min"
                  />
                  <Input
                    type="number"
                    step="0.1"
                    value={editingDevice.maxHumidity}
                    onChange={(e) =>
                      setEditingDevice({ ...editingDevice, maxHumidity: Number(e.target.value) })
                    }
                    placeholder="Max"
                  />
                </div>
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setEditingDevice(null)}>
                {t('cancel')}
              </Button>
              <Button
                onClick={() =>
                  updateMutation.mutate({
                    id: editingDevice.deviceID,
                    data: {
                      location: editingDevice.location,
                      type: editingDevice.type,
                      minTemperature: (editingDevice.minTemperature as any) !== '' && (editingDevice.minTemperature as any) !== '-' ? Number(editingDevice.minTemperature) : 0,
                      maxTemperature: (editingDevice.maxTemperature as any) !== '' && (editingDevice.maxTemperature as any) !== '-' ? Number(editingDevice.maxTemperature) : 0,
                      minHumidity: editingDevice.minHumidity,
                      maxHumidity: editingDevice.maxHumidity,
                    },
                  })
                }
                disabled={updateMutation.isPending}>
                {t('save')}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}

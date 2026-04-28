import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { locationApi, iotApi } from '@/api'
import type { StorageLocationDto, IoTDeviceDto } from '@/types/api'
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Plus, Pencil, Trash2, Loader2, MapPin } from 'lucide-react'

const LOCATION_TYPES = ['Refrigerator', 'Shelf', 'Warehouse', 'Cabinet', 'Other']

type FormData = Omit<StorageLocationDto, 'locationId' | 'ioTDeviceLocation'>

function LocationForm({
  initial,
  onSave,
  onClose,
  isLoading,
}: {
  initial?: Partial<FormData>
  onSave: (d: FormData) => void
  onClose: () => void
  isLoading: boolean
  devices: IoTDeviceDto[]
}) {
  const [form, setForm] = useState<Partial<FormData>>(initial ?? {})
  const set = (k: keyof FormData, v: string | number | undefined) => setForm((p) => ({ ...p, [k]: v }))

  return (
    <div className="space-y-4">
      <div className="space-y-1.5">
        <Label>Назва *</Label>
        <Input value={form.name ?? ''} onChange={(e) => set('name', e.target.value)} placeholder="Холодильник A" />
      </div>
      <div className="space-y-1.5">
        <Label>Адреса</Label>
        <Input value={form.address ?? ''} onChange={(e) => set('address', e.target.value)} placeholder="Кімната 203, 2 поверх" />
      </div>
      <div className="space-y-1.5">
        <Label>Тип локації</Label>
        <select
          className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
          value={form.locationType ?? 'Other'}
          onChange={(e) => set('locationType', e.target.value)}
        >
          {LOCATION_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
      </div>
      <div className="space-y-1.5">
        <Label>IoT-пристрій</Label>
        <select
          className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
          value={form.ioTDeviceId ?? ''}
          onChange={(e) => set('ioTDeviceId', e.target.value || undefined)}
        >
          <option value="">Немає (Без пристрою)</option>
          {devices.map((d) => (
            <option key={d.deviceID} value={d.deviceID}>
              {d.deviceID} ({d.location})
            </option>
          ))}
        </select>
      </div>
      <DialogFooter>
        <Button variant="outline" onClick={onClose}>Скасувати</Button>
        <Button
          onClick={() => onSave(form as FormData)}
          disabled={isLoading || !form.name}
        >
          {isLoading && <Loader2 className="h-4 w-4 animate-spin" />}
          Зберегти
        </Button>
      </DialogFooter>
    </div>
  )
}

export default function StorageLocationsPage() {
  const { isAdmin } = useAuth()
  const qc = useQueryClient()
  const [dialogMode, setDialogMode] = useState<'create' | 'edit' | null>(null)
  const [selected, setSelected] = useState<StorageLocationDto | null>(null)

  const { data: locations = [], isLoading } = useQuery({
    queryKey: ['locations'],
    queryFn: locationApi.getAll,
  })

  const { data: devices = [] } = useQuery({
    queryKey: ['iot-devices'],
    queryFn: iotApi.getAll,
  })

  const createMutation = useMutation({
    mutationFn: (d: FormData) => locationApi.create(d),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['locations'] }); setDialogMode(null) },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: FormData }) => locationApi.update(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['locations'] }); setDialogMode(null) },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => locationApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['locations'] }),
  })

  const typeColors: Record<string, string> = {
    Refrigerator: 'info',
    Shelf: 'secondary',
    Warehouse: 'warning',
    Cabinet: 'outline',
    Other: 'outline',
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Локації зберігання</h1>
          <p className="text-muted-foreground">Управління місцями зберігання препаратів</p>
        </div>
        {isAdmin && (
          <Button onClick={() => { setSelected(null); setDialogMode('create') }}>
            <Plus className="h-4 w-4" /> Додати
          </Button>
        )}
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {isLoading ? (
          Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-32" />)
        ) : (
          locations.map((l) => (
            <Card key={l.locationId} className="relative overflow-hidden">
              <div className="absolute right-4 top-4">
                <Badge variant={(typeColors[l.locationType] ?? 'outline') as 'info' | 'secondary' | 'warning' | 'outline'}>
                  {l.locationType}
                </Badge>
              </div>
              <CardHeader className="pb-2">
                <div className="flex items-start gap-2">
                  <MapPin className="mt-0.5 h-4 w-4 text-primary shrink-0" />
                  <CardTitle className="text-base">{l.name}</CardTitle>
                </div>
              </CardHeader>
              <CardContent className="space-y-1 text-sm text-muted-foreground">
                {l.address && <p>📍 {l.address}</p>}
                {l.ioTDeviceLocation && <p>🔌 IoT: {l.ioTDeviceLocation}</p>}
                {isAdmin && (
                  <div className="flex gap-2 pt-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => { setSelected(l); setDialogMode('edit') }}
                    >
                      <Pencil className="h-3 w-3" /> Редагувати
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      className="text-destructive hover:text-destructive"
                      onClick={() => deleteMutation.mutate(l.locationId)}
                    >
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  </div>
                )}
              </CardContent>
            </Card>
          ))
        )}
        {!isLoading && locations.length === 0 && (
          <Card className="col-span-3">
            <CardContent className="py-10 text-center text-muted-foreground">
              Локацій ще немає. {isAdmin && 'Додайте першу.'}
            </CardContent>
          </Card>
        )}
      </div>

      <Dialog open={dialogMode !== null} onOpenChange={(o) => !o && setDialogMode(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{dialogMode === 'create' ? 'Нова локація' : 'Редагувати локацію'}</DialogTitle>
          </DialogHeader>
          <LocationForm
            initial={selected ? {
              name: selected.name,
              address: selected.address,
              locationType: selected.locationType,
              ioTDeviceId: selected.ioTDeviceId,
            } : {}}
            isLoading={createMutation.isPending || updateMutation.isPending}
            devices={devices}
            onClose={() => setDialogMode(null)}
            onSave={(data) => {
              if (dialogMode === 'create') createMutation.mutate(data)
              else if (selected) updateMutation.mutate({ id: selected.locationId, data })
            }}
          />
        </DialogContent>
      </Dialog>
    </div>
  )
}

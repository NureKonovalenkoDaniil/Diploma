import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Plus, Loader2 } from 'lucide-react'
import { medicineApi, lifecycleApi, locationApi } from '@/api'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { format } from 'date-fns'
import { useState } from 'react'

const EVENT_TYPES = ['Received', 'Dispensed', 'Relocated', 'Disposed', 'Inspected', 'Quarantined', 'Returned']

export default function MedicineDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [eventForm, setEventForm] = useState({ eventType: 'Received', description: '', quantity: '' })

  const medId = Number(id)

  const { data: medicine, isLoading: mLoading } = useQuery({
    queryKey: ['medicines', medId],
    queryFn: () => medicineApi.getById(medId),
  })

  const { data: events = [], isLoading: eLoading } = useQuery({
    queryKey: ['lifecycle', medId],
    queryFn: () => lifecycleApi.getByMedicine(medId),
  })

  const { data: locations = [] } = useQuery({
    queryKey: ['locations'],
    queryFn: locationApi.getAll,
  })

  const addEventMutation = useMutation({
    mutationFn: () =>
      lifecycleApi.addEvent({
        medicineId: medId,
        eventType: eventForm.eventType,
        description: eventForm.description || undefined,
        quantity: eventForm.quantity ? Number(eventForm.quantity) : undefined,
        relatedLocationId: undefined,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['lifecycle', medId] })
      setOpen(false)
      setEventForm({ eventType: 'Received', description: '', quantity: '' })
    },
  })

  if (mLoading) return <div className="space-y-4"><Skeleton className="h-32" /><Skeleton className="h-48" /></div>
  if (!medicine) return <p className="text-muted-foreground">Препарат не знайдено</p>

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate('/medicines')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold">{medicine.name}</h1>
          <p className="text-muted-foreground">{medicine.type} · {medicine.category}</p>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card><CardContent className="pt-6"><p className="text-xs text-muted-foreground">Кількість</p><p className="text-2xl font-bold">{medicine.quantity}</p></CardContent></Card>
        <Card><CardContent className="pt-6"><p className="text-xs text-muted-foreground">Термін придатності</p><p className="text-lg font-semibold">{format(new Date(medicine.expiryDate), 'dd.MM.yyyy')}</p></CardContent></Card>
        <Card><CardContent className="pt-6"><p className="text-xs text-muted-foreground">Виробник</p><p className="text-lg font-semibold">{medicine.manufacturer ?? '—'}</p></CardContent></Card>
        <Card><CardContent className="pt-6"><p className="text-xs text-muted-foreground">Локація</p><p className="text-lg font-semibold">{medicine.storageLocationName ?? '—'}</p></CardContent></Card>
      </div>

      {/* Details */}
      <Card>
        <CardHeader><CardTitle className="text-base">Деталі</CardTitle></CardHeader>
        <CardContent className="grid sm:grid-cols-2 gap-4 text-sm">
          <div><span className="text-muted-foreground">Номер партії:</span> {medicine.batchNumber ?? '—'}</div>
          <div><span className="text-muted-foreground">Опис:</span> {medicine.description ?? '—'}</div>
          <div><span className="text-muted-foreground">Мін. темп. зберіг.:</span> {medicine.minStorageTemp != null ? `${medicine.minStorageTemp}°C` : '—'}</div>
          <div><span className="text-muted-foreground">Макс. темп. зберіг.:</span> {medicine.maxStorageTemp != null ? `${medicine.maxStorageTemp}°C` : '—'}</div>
        </CardContent>
      </Card>

      {/* Lifecycle Events */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-base">Lifecycle-події</CardTitle>
          <Button size="sm" onClick={() => setOpen(true)}>
            <Plus className="h-3.5 w-3.5" /> Додати подію
          </Button>
        </CardHeader>
        <CardContent className="p-0">
          {eLoading ? (
            <div className="p-4 space-y-2">{Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-9" />)}</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Подія</TableHead>
                  <TableHead>К-сть</TableHead>
                  <TableHead>Опис</TableHead>
                  <TableHead>Виконав</TableHead>
                  <TableHead>Дата</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {events.length === 0 ? (
                  <TableRow><TableCell colSpan={5} className="text-center text-muted-foreground py-8">Подій ще немає</TableCell></TableRow>
                ) : events.map((e) => (
                  <TableRow key={e.eventId}>
                    <TableCell><Badge variant="secondary">{e.eventType}</Badge></TableCell>
                    <TableCell>{e.quantity ?? '—'}</TableCell>
                    <TableCell className="text-muted-foreground">{e.description ?? '—'}</TableCell>
                    <TableCell>{e.performedBy}</TableCell>
                    <TableCell>{format(new Date(e.performedAt), 'dd.MM.yyyy HH:mm')}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Нова lifecycle-подія</DialogTitle></DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label>Тип події</Label>
              <select
                className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
                value={eventForm.eventType}
                onChange={(e) => setEventForm((p) => ({ ...p, eventType: e.target.value }))}
              >
                {EVENT_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label>Кількість</Label>
              <Input type="number" value={eventForm.quantity} onChange={(e) => setEventForm((p) => ({ ...p, quantity: e.target.value }))} />
            </div>
            <div className="space-y-1.5">
              <Label>Опис</Label>
              <Input value={eventForm.description} onChange={(e) => setEventForm((p) => ({ ...p, description: e.target.value }))} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>Скасувати</Button>
            <Button onClick={() => addEventMutation.mutate()} disabled={addEventMutation.isPending}>
              {addEventMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Зберегти
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

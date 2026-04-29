import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Search, Pencil, Trash2, Loader2, ChevronRight } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { medicineApi, locationApi } from '@/api'
import type { MedicineDto } from '@/types/api'
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { format, isPast, isWithinInterval, addDays } from 'date-fns'

function getMedicineStatus(m: MedicineDto): { label: string; variant: 'destructive' | 'warning' | 'success' | 'secondary' } {
  const expiry = new Date(m.expiryDate)
  if (isPast(expiry)) return { label: 'Прострочено', variant: 'destructive' }
  if (isWithinInterval(expiry, { start: new Date(), end: addDays(new Date(), 7) })) return { label: 'Скоро', variant: 'warning' }
  if (m.quantity <= 10) return { label: 'Мало', variant: 'warning' }
  return { label: 'Норма', variant: 'success' }
}

function MedicineForm({
  initial,
  locations,
  onSave,
  onClose,
  isLoading,
}: {
  initial?: Partial<MedicineDto>
  locations: { locationId: number; name: string }[]
  onSave: (data: Partial<MedicineDto>) => void
  onClose: () => void
  isLoading: boolean
}) {
  const [form, setForm] = useState<Partial<MedicineDto>>(initial ?? {})
  const [touched, setTouched] = useState<Partial<Record<keyof MedicineDto, boolean>>>({})
  const set = (k: keyof MedicineDto, v: string | number | undefined) => {
    setForm((p) => ({ ...p, [k]: v }))
    setTouched((p) => ({ ...p, [k]: true }))
  }

  const errors: Partial<Record<keyof MedicineDto, string>> = {}
  if (!form.name?.trim())        errors.name       = "Назва є обов'язковою"
  if (!form.type?.trim())        errors.type       = "Тип є обов'язковим"
  if (!form.category?.trim())    errors.category   = "Категорія є обов'язковою"
  if (!form.quantity && form.quantity !== 0) errors.quantity = "Вкажіть кількість"
  if (!form.expiryDate)          errors.expiryDate = "Вкажіть термін придатності"

  const isValid = Object.keys(errors).length === 0

  const field = (label: string, key: keyof MedicineDto, required = false, children: React.ReactNode) => (
    <div className="space-y-1.5">
      <Label className={required ? 'after:content-["*"] after:ml-0.5 after:text-destructive' : ''}>
        {label}
      </Label>
      {children}
      {touched[key] && errors[key] && (
        <p className="text-xs text-destructive">{errors[key]}</p>
      )}
    </div>
  )

  const handleSave = () => {
    // позначити всі обов'язкові поля як торкнуті
    setTouched({ name: true, type: true, category: true, quantity: true, expiryDate: true })
    if (isValid) onSave(form)
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        {field('Назва', 'name', true,
          <Input
            value={form.name ?? ''}
            onChange={(e) => set('name', e.target.value)}
            placeholder="Amoxicillin"
            className={touched.name && errors.name ? 'border-destructive' : ''}
          />
        )}
        {field('Тип', 'type', true,
          <Input
            value={form.type ?? ''}
            onChange={(e) => set('type', e.target.value)}
            placeholder="Antibiotic"
            className={touched.type && errors.type ? 'border-destructive' : ''}
          />
        )}
        {field('Категорія', 'category', true,
          <Input
            value={form.category ?? ''}
            onChange={(e) => set('category', e.target.value)}
            placeholder="Prescription"
            className={touched.category && errors.category ? 'border-destructive' : ''}
          />
        )}
        {field('Кількість', 'quantity', true,
          <Input
            type="number"
            min={0}
            value={form.quantity ?? ''}
            onChange={(e) => set('quantity', Number(e.target.value))}
            className={touched.quantity && errors.quantity ? 'border-destructive' : ''}
          />
        )}
        {field('Термін придатності', 'expiryDate', true,
          <Input
            type="date"
            value={form.expiryDate ? form.expiryDate.slice(0, 10) : ''}
            onChange={(e) => set('expiryDate', e.target.value)}
            className={touched.expiryDate && errors.expiryDate ? 'border-destructive' : ''}
          />
        )}
        {field('Виробник', 'manufacturer', false,
          <Input
            value={form.manufacturer ?? ''}
            onChange={(e) => set('manufacturer', e.target.value)}
          />
        )}
        {field('Номер партії', 'batchNumber', false,
          <Input
            value={form.batchNumber ?? ''}
            onChange={(e) => set('batchNumber', e.target.value)}
          />
        )}
        <div className="space-y-1.5">
          <Label>Локація</Label>
          <select
            className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
            value={form.storageLocationId ?? ''}
            onChange={(e) => set('storageLocationId', e.target.value ? Number(e.target.value) : undefined)}
          >
            <option value="">— Не вибрано —</option>
            {locations.map((l) => (
              <option key={l.locationId} value={l.locationId}>{l.name}</option>
            ))}
          </select>
        </div>
      </div>
      <div className="space-y-1.5">
        <Label>Опис</Label>
        <Input value={form.description ?? ''} onChange={(e) => set('description', e.target.value)} />
      </div>
      <DialogFooter>
        <Button variant="outline" onClick={onClose}>Скасувати</Button>
        <Button onClick={handleSave} disabled={isLoading}>
          {isLoading && <Loader2 className="h-4 w-4 animate-spin" />}
          Зберегти
        </Button>
      </DialogFooter>
    </div>
  )
}

export default function MedicinesPage() {
  const { isAdmin, isManager } = useAuth()
  const canManage = isAdmin || isManager
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [search, setSearch] = useState('')
  const [dialogMode, setDialogMode] = useState<'create' | 'edit' | null>(null)
  const [selected, setSelected] = useState<MedicineDto | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)

  const { data: medicines = [], isLoading } = useQuery({
    queryKey: ['medicines'],
    queryFn: medicineApi.getAll,
  })

  const { data: locations = [] } = useQuery({
    queryKey: ['locations'],
    queryFn: locationApi.getAll,
  })

  const createMutation = useMutation({
    mutationFn: (data: Partial<MedicineDto>) =>
      medicineApi.create(data as Omit<MedicineDto, 'medicineID' | 'storageLocationName'>),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['medicines'] })
      setDialogMode(null)
      setServerError(null)
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.title || err?.response?.data || 'Помилка сервера. Спробуйте ще раз.'
      setServerError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<MedicineDto> }) => {
      // Exclude read-only DTO fields that don't exist on the backend entity
      const { medicineID, storageLocationName, ...patchData } = data as any
      return medicineApi.update(
        id,
        Object.entries(patchData).map(([path, value]) => ({ op: 'replace', path: `/${path}`, value })),
      )
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['medicines'] })
      setDialogMode(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => medicineApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['medicines'] }),
  })

  const filtered = medicines.filter(
    (m) =>
      m.name.toLowerCase().includes(search.toLowerCase()) ||
      m.type.toLowerCase().includes(search.toLowerCase()),
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Препарати</h1>
          <p className="text-muted-foreground">Управління медичними препаратами</p>
        </div>
        {canManage && (
          <Button onClick={() => { setSelected(null); setDialogMode('create') }}>
            <Plus className="h-4 w-4" /> Додати
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center gap-3">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Пошук за назвою або типом..."
                className="pl-9"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <CardDescription>{filtered.length} із {medicines.length}</CardDescription>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="space-y-2 p-4">
              {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10" />)}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Назва</TableHead>
                  <TableHead>Тип</TableHead>
                  <TableHead>К-сть</TableHead>
                  <TableHead>Термін</TableHead>
                  <TableHead>Статус</TableHead>
                  <TableHead>Локація</TableHead>
                  {canManage && <TableHead className="text-right">Дії</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="py-10 text-center text-muted-foreground">
                      Препарати не знайдені
                    </TableCell>
                  </TableRow>
                ) : (
                  filtered.map((m) => {
                    const status = getMedicineStatus(m)
                    return (
                      <TableRow
                        key={m.medicineID}
                        className="cursor-pointer"
                        onClick={() => navigate(`/medicines/${m.medicineID}`)}
                      >
                        <TableCell className="font-medium">{m.name}</TableCell>
                        <TableCell><Badge variant="outline">{m.type}</Badge></TableCell>
                        <TableCell>{m.quantity}</TableCell>
                        <TableCell>{format(new Date(m.expiryDate), 'dd.MM.yyyy')}</TableCell>
                        <TableCell>
                          <Badge variant={status.variant}>{status.label}</Badge>
                        </TableCell>
                        <TableCell className="text-muted-foreground">{m.storageLocationName ?? '—'}</TableCell>
                        {canManage && (
                          <TableCell className="text-right">
                            <div className="flex items-center justify-end gap-1" onClick={(e) => e.stopPropagation()}>
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => { setSelected(m); setDialogMode('edit') }}
                              >
                                <Pencil className="h-3.5 w-3.5" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="text-destructive hover:text-destructive"
                                onClick={() => deleteMutation.mutate(m.medicineID)}
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </Button>
                              <Button variant="ghost" size="icon">
                                <ChevronRight className="h-3.5 w-3.5" />
                              </Button>
                            </div>
                          </TableCell>
                        )}
                      </TableRow>
                    )
                  })
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={dialogMode !== null} onOpenChange={(o) => { if (!o) { setDialogMode(null); setServerError(null) } }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{dialogMode === 'create' ? 'Додати препарат' : 'Редагувати препарат'}</DialogTitle>
            <DialogDescription className="sr-only">
              Форма для {dialogMode === 'create' ? 'створення нового' : 'редагування існуючого'} медичного препарату
            </DialogDescription>
          </DialogHeader>
          {serverError && (
            <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-2 text-sm text-destructive">
              {serverError}
            </div>
          )}
          <MedicineForm
            initial={selected ?? {}}
            locations={locations}
            isLoading={createMutation.isPending || updateMutation.isPending}
            onClose={() => { setDialogMode(null); setServerError(null) }}
            onSave={(data) => {
              setServerError(null)
              if (dialogMode === 'create') {
                createMutation.mutate(data)
              } else if (selected) {
                updateMutation.mutate({ id: selected.medicineID, data })
              }
            }}
          />
        </DialogContent>
      </Dialog>
    </div>
  )
}

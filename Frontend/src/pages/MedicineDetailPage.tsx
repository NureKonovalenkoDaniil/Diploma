import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Plus, Truck, ArrowDownCircle, ArrowUpCircle, Trash2, Loader2 } from 'lucide-react';
import { medicineApi, lifecycleApi, locationApi } from '@/api';
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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { format } from 'date-fns';
import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useLocale } from '@/contexts/LocaleContext';

function translateEventDescription(desc: string | null | undefined, t: (key: string) => string) {
  if (!desc) return '—';
  let match = desc.match(/^(?:Авто-надходження при створенні|Auto-received on creation):\s*\+(\d+)/i);
  if (match) {
    return `${t('eventAutoReceivedAtCreation')}: +${match[1]}`;
  }
  match = desc.match(/^(?:Надходження|Received):\s*\+(\d+)/i);
  if (match) {
    return `${t('eventTypeReceived')}: +${match[1]}`;
  }
  match = desc.match(/^(?:Видача|Issued):\s*-(\d+)/i);
  if (match) {
    return `${t('eventTypeIssued')}: -${match[1]}`;
  }
  match = desc.match(/^(?:Утилізація|Disposed):\s*-(\d+)/i);
  if (match) {
    return `${t('eventTypeDisposed')}: -${match[1]}`;
  }
  match = desc.match(/^(?:Переміщення|Moved):\s*(.*)/i);
  if (match) {
    return `${t('eventTypeMoved')}: ${match[1]}`;
  }
  return desc;
}

const EVENT_TYPES = ['Received', 'Issued', 'Moved', 'Expired', 'Disposed', 'Recalled'];

export default function MedicineDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { isAdmin, isManager, isUser } = useAuth();
  const { t } = useLocale();
  const [open, setOpen] = useState(false);
  const [moveOpen, setMoveOpen] = useState(false);
  const [stockOpen, setStockOpen] = useState<null | 'receive' | 'issue' | 'dispose'>(null);
  const [eventForm, setEventForm] = useState({
    eventType: 'Received',
    description: '',
    quantity: '',
    relatedLocationId: '',
  });
  const [moveForm, setMoveForm] = useState({
    storageLocationId: '',
    description: '',
    quantity: '',
  });
  const [stockForm, setStockForm] = useState({
    quantity: '',
    description: '',
    storageLocationId: '',
  });

  const medId = Number(id);

  const { data: medicine, isLoading: mLoading } = useQuery({
    queryKey: ['medicines', medId],
    queryFn: () => medicineApi.getById(medId),
  });

  const { data: events = [], isLoading: eLoading } = useQuery({
    queryKey: ['lifecycle', medId],
    queryFn: () => lifecycleApi.getByMedicine(medId),
  });

  const { data: locations = [] } = useQuery({
    queryKey: ['locations'],
    queryFn: locationApi.getAll,
  });

  const canManage = isAdmin || isManager || isUser;

  const invalidateMedicineViews = () => {
    qc.invalidateQueries({ queryKey: ['medicines', medId] });
    qc.invalidateQueries({ queryKey: ['medicines'] });
    qc.invalidateQueries({ queryKey: ['lifecycle', medId] });
  };

  const moveMutation = useMutation({
    mutationFn: () =>
      medicineApi.move(medId, {
        storageLocationId: Number(moveForm.storageLocationId),
        description: moveForm.description || undefined,
        quantity: moveForm.quantity ? Number(moveForm.quantity) : undefined,
      }),
    onSuccess: () => {
      invalidateMedicineViews();
      setMoveOpen(false);
      setMoveForm({ storageLocationId: '', description: '', quantity: '' });
    },
  });

  const stockMutation = useMutation({
    mutationFn: async () => {
      const qty = Number(stockForm.quantity);
      if (!stockOpen) throw new Error('Invalid operation');
      if (!Number.isFinite(qty)) throw new Error('Invalid quantity');

      if (stockOpen === 'receive') {
        return medicineApi.receive(medId, {
          quantity: qty,
          description: stockForm.description || undefined,
          storageLocationId: stockForm.storageLocationId ? Number(stockForm.storageLocationId) : undefined,
        });
      }
      if (stockOpen === 'issue') {
        return medicineApi.issue(medId, {
          quantity: qty,
          description: stockForm.description || undefined,
        });
      }
      // dispose: qty==0 => backend interprets as "dispose all"
      return medicineApi.dispose(medId, {
        quantity: qty,
        description: stockForm.description || undefined,
      });
    },
    onSuccess: () => {
      invalidateMedicineViews();
      setStockOpen(null);
      setStockForm({ quantity: '', description: '', storageLocationId: '' });
    },
  });

  const addEventMutation = useMutation({
    mutationFn: () =>
      lifecycleApi.addEvent({
        medicineId: medId,
        eventType: eventForm.eventType,
        description: eventForm.description || undefined,
        quantity: eventForm.quantity ? Number(eventForm.quantity) : undefined,
        relatedLocationId: eventForm.relatedLocationId
          ? Number(eventForm.relatedLocationId)
          : undefined,
      }),
    onSuccess: () => {
      invalidateMedicineViews();
      setOpen(false);
      setEventForm({ eventType: 'Received', description: '', quantity: '', relatedLocationId: '' });
    },
  });

  if (mLoading)
    return (
      <div className="space-y-4">
        <Skeleton className="h-32" />
        <Skeleton className="h-48" />
      </div>
    );
  if (!medicine) return <p className="text-muted-foreground">{t('medicineNotFound')}</p>;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate('/medicines')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold">{medicine.name}</h1>
          <p className="text-muted-foreground">
            {medicine.type} · {medicine.category}
          </p>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardContent className="pt-6">
            <p className="text-xs text-muted-foreground">{t('quantity')}</p>
            <p className="text-2xl font-bold">{medicine.quantity}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <p className="text-xs text-muted-foreground">{t('expiryDate')}</p>
            <p className="text-lg font-semibold">
              {format(new Date(medicine.expiryDate), 'dd.MM.yyyy')}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <p className="text-xs text-muted-foreground">{t('manufacturer')}</p>
            <p className="text-lg font-semibold">{medicine.manufacturer ?? '—'}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <p className="text-xs text-muted-foreground">{t('location')}</p>
            <p className="text-lg font-semibold">{medicine.storageLocationName ?? '—'}</p>
          </CardContent>
        </Card>
      </div>

      {/* Details */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">{t('details')}</CardTitle>
        </CardHeader>
        <CardContent className="grid sm:grid-cols-2 gap-4 text-sm">
          <div>
            <span className="text-muted-foreground">{t('batchNumber')}:</span>{' '}
            {medicine.batchNumber ?? '—'}
          </div>
          <div>
            <span className="text-muted-foreground">{t('description')}:</span> {medicine.description ?? '—'}
          </div>
          <div>
            <span className="text-muted-foreground">{t('minStorageTemp')}:</span>{' '}
            {medicine.minStorageTemp != null ? `${medicine.minStorageTemp}°C` : '—'}
          </div>
          <div>
            <span className="text-muted-foreground">{t('maxStorageTemp')}:</span>{' '}
            {medicine.maxStorageTemp != null ? `${medicine.maxStorageTemp}°C` : '—'}
          </div>
          <div>
            <span className="text-muted-foreground">{t('minStorageHumidity')}:</span>{' '}
            {medicine.minStorageHumidity != null ? `${medicine.minStorageHumidity}%` : '—'}
          </div>
          <div>
            <span className="text-muted-foreground">{t('maxStorageHumidity')}:</span>{' '}
            {medicine.maxStorageHumidity != null ? `${medicine.maxStorageHumidity}%` : '—'}
          </div>
        </CardContent>
      </Card>

      {/* Lifecycle Events */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-base">{t('lifecycleEventsTitle')}</CardTitle>
          <div className="flex items-center gap-2">
            {canManage && (
              <>
                <Button size="sm" variant="outline" onClick={() => setStockOpen('receive')}>
                  <ArrowDownCircle className="h-3.5 w-3.5" /> {t('eventReceived')}
                </Button>
                <Button size="sm" variant="outline" onClick={() => setStockOpen('issue')}>
                  <ArrowUpCircle className="h-3.5 w-3.5" /> {t('eventIssued')}
                </Button>
                <Button size="sm" variant="outline" onClick={() => setStockOpen('dispose')}>
                  <Trash2 className="h-3.5 w-3.5" /> {t('eventDisposed')}
                </Button>
                <Button size="sm" variant="outline" onClick={() => setMoveOpen(true)}>
                  <Truck className="h-3.5 w-3.5" /> {t('eventMoved')}
                </Button>
              </>
            )}
            <Button size="sm" onClick={() => setOpen(true)}>
              <Plus className="h-3.5 w-3.5" /> {t('addEvent')}
            </Button>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {eLoading ? (
            <div className="p-4 space-y-2">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-9" />
              ))}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('colEvent')}</TableHead>
                  <TableHead>{t('colEventQuantity')}</TableHead>
                  <TableHead>{t('colEventDescription')}</TableHead>
                  <TableHead>{t('colEventLocation')}</TableHead>
                  <TableHead>{t('colEventPerformedBy')}</TableHead>
                  <TableHead>{t('colEventDate')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {events.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                      {t('noEventsYet')}
                    </TableCell>
                  </TableRow>
                ) : (
                  events.map((e) => (
                    <TableRow key={e.eventId}>
                      <TableCell>
                        <Badge variant="secondary">{t('eventType' + e.eventType)}</Badge>
                      </TableCell>
                      <TableCell>{e.quantity ?? '—'}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {translateEventDescription(e.description, t)}
                      </TableCell>
                      <TableCell>{e.relatedLocationName ?? '—'}</TableCell>
                      <TableCell>{e.performedBy}</TableCell>
                      <TableCell>{format(new Date(e.performedAt), 'dd.MM.yyyy HH:mm')}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog open={moveOpen} onOpenChange={setMoveOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('moveMedicineTitle')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label>{t('newLocationLabel')}</Label>
              <select
                title={t('newLocationLabel')}
                className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm text-foreground"
                value={moveForm.storageLocationId}
                onChange={(e) => setMoveForm((p) => ({ ...p, storageLocationId: e.target.value }))}>
                <option value="">{t('chooseLocationOption')}</option>
                {locations.map((loc) => (
                  <option key={loc.locationId} value={loc.locationId}>
                    {loc.name}
                  </option>
                ))}
              </select>
              {!moveForm.storageLocationId && (
                <p className="text-xs text-muted-foreground">{t('requiredForMove')}</p>
              )}
            </div>
            <div className="space-y-1.5">
              <Label>{t('quantityOptional')}</Label>
              <Input
                type="number"
                min={0}
                value={moveForm.quantity}
                onChange={(e) => setMoveForm((p) => ({ ...p, quantity: e.target.value }))}
              />
            </div>
            <div className="space-y-1.5">
              <Label>{t('commentOptional')}</Label>
              <Input
                value={moveForm.description}
                onChange={(e) => setMoveForm((p) => ({ ...p, description: e.target.value }))}
                placeholder={t('commentMovePlaceholder')}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setMoveOpen(false)}>
              {t('cancel')}
            </Button>
            <Button
              onClick={() => moveMutation.mutate()}
              disabled={moveMutation.isPending || !moveForm.storageLocationId}>
              {moveMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              {t('eventMoved')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={stockOpen !== null} onOpenChange={(o) => { if (!o) setStockOpen(null); }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {stockOpen === 'receive'
                ? t('eventReceived')
                : stockOpen === 'issue'
                  ? t('eventIssued')
                  : t('eventDisposed')}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label>{t('quantity')}</Label>
              <Input
                type="number"
                min={stockOpen === 'dispose' ? 0 : 1}
                value={stockForm.quantity}
                onChange={(e) => setStockForm((p) => ({ ...p, quantity: e.target.value }))}
                placeholder={stockOpen === 'dispose' ? t('disposePlaceholder') : t('quantityPlaceholder')}
              />
              {stockOpen === 'dispose' && (
                <p className="text-xs text-muted-foreground">{t('disposeHint')}</p>
              )}
            </div>

            {stockOpen === 'receive' && (
              <div className="space-y-1.5">
                <Label>{t('locationReceiveOptional')}</Label>
                <select
                  title={t('locationReceiveOptional')}
                  className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm text-foreground"
                  value={stockForm.storageLocationId}
                  onChange={(e) => setStockForm((p) => ({ ...p, storageLocationId: e.target.value }))}>
                  <option value="">{t('noLocationChange')}</option>
                  {locations.map((loc) => (
                    <option key={loc.locationId} value={loc.locationId}>
                      {loc.name}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div className="space-y-1.5">
              <Label>{t('commentOptional')}</Label>
              <Input
                value={stockForm.description}
                onChange={(e) => setStockForm((p) => ({ ...p, description: e.target.value }))}
                placeholder={t('commentReceivePlaceholder')}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setStockOpen(null)}>
              {t('cancel')}
            </Button>
            <Button
              onClick={() => stockMutation.mutate()}
              disabled={stockMutation.isPending || !stockForm.quantity}>
              {stockMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              {t('confirmBtn')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('newEventTitle')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label>{t('eventTypeLabel')}</Label>
              <select
                title={t('eventTypeLabel')}
                className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm text-foreground"
                value={eventForm.eventType}
                onChange={(e) => setEventForm((p) => ({ ...p, eventType: e.target.value }))}>
                {EVENT_TYPES.map((tVal) => (
                  <option key={tVal} value={tVal}>
                    {t('eventType' + tVal)}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label>{t('quantity')}</Label>
              <Input
                type="number"
                value={eventForm.quantity}
                onChange={(e) => setEventForm((p) => ({ ...p, quantity: e.target.value }))}
              />
            </div>
            <div className="space-y-1.5">
              <Label>{t('description')}</Label>
              <Input
                value={eventForm.description}
                onChange={(e) => setEventForm((p) => ({ ...p, description: e.target.value }))}
              />
            </div>
            <div className="space-y-1.5">
              <Label>{t('relatedLocationOptional')}</Label>
              <select
                title={t('relatedLocationOptional')}
                className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm text-foreground"
                value={eventForm.relatedLocationId}
                onChange={(e) =>
                  setEventForm((p) => ({ ...p, relatedLocationId: e.target.value }))
                }>
                <option value="">{t('notSelected')}</option>
                {locations.map((loc) => (
                  <option key={loc.locationId} value={loc.locationId}>
                    {loc.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>
              {t('cancel')}
            </Button>
            <Button onClick={() => addEventMutation.mutate()} disabled={addEventMutation.isPending}>
              {addEventMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              {t('save')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

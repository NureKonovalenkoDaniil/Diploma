import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { locationApi, iotApi } from '@/api';
import type { StorageLocationDto, IoTDeviceDto } from '@/types/api';
import { useAuth } from '@/contexts/AuthContext';
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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import { Plus, Pencil, Trash2, Loader2, MapPin, Search } from 'lucide-react';
import { useLocale } from '@/contexts/LocaleContext';
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

const LOCATION_TYPES = ['Refrigerator', 'Shelf', 'Warehouse', 'Cabinet', 'Other'];

type FormData = Omit<StorageLocationDto, 'locationId' | 'ioTDeviceLocation'>;

function LocationForm({
  initial,
  onSave,
  onClose,
  isLoading,
  devices,
}: {
  initial?: Partial<FormData>;
  onSave: (d: FormData) => void;
  onClose: () => void;
  isLoading: boolean;
  devices: IoTDeviceDto[];
}) {
  const [form, setForm] = useState<Partial<FormData>>({
    locationType: 'Refrigerator',
    ...initial,
  });
  const { t } = useLocale();
  const set = (k: keyof FormData, v: string | number | undefined) =>
    setForm((p) => ({ ...p, [k]: v }));

  return (
    <div className="space-y-4">
      <div className="space-y-1.5">
        <Label>{t('locationNameLabel')}</Label>
        <Input
          value={form.name ?? ''}
          onChange={(e) => set('name', e.target.value)}
          placeholder={t('locationNamePlaceholder')}
        />
      </div>
      <div className="space-y-1.5">
        <Label>{t('locationAddressLabel')}</Label>
        <Input
          value={form.address ?? ''}
          onChange={(e) => set('address', e.target.value)}
          placeholder={t('locationAddressPlaceholder')}
        />
      </div>
      <div className="space-y-1.5">
        <Label>{t('locationTypeLabel')}</Label>
        <select
          aria-label={t('locationTypeLabel')}
          className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm text-foreground"
          value={form.locationType ?? 'Other'}
          onChange={(e) => set('locationType', e.target.value)}>
          {LOCATION_TYPES.map((tVal) => (
            <option key={tVal} value={tVal}>
              {t('locationType' + tVal)}
            </option>
          ))}
        </select>
      </div>
      <div className="space-y-1.5">
        <Label>{t('iotDeviceLabel')}</Label>
        <select
          aria-label={t('iotDeviceLabel')}
          className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm text-foreground"
          value={form.ioTDeviceId ?? ''}
          onChange={(e) => set('ioTDeviceId', e.target.value || undefined)}>
          <option value="">{t('noDeviceOption')}</option>
          {devices.map((d) => (
            <option key={d.deviceID} value={d.deviceID}>
              {d.deviceID} ({d.location === 'Unassigned' ? t('unassigned') : d.location})
            </option>
          ))}
        </select>
      </div>
      <DialogFooter>
        <Button variant="outline" onClick={onClose}>
          {t('cancel')}
        </Button>
        <Button onClick={() => onSave(form as FormData)} disabled={isLoading || !form.name}>
          {isLoading && <Loader2 className="h-4 w-4 animate-spin" />}
          {t('save')}
        </Button>
      </DialogFooter>
    </div>
  );
}

export default function StorageLocationsPage() {
  const { isAdmin, isManager, isUser } = useAuth();
  const { t } = useLocale();
  const canManage = isAdmin || isManager || isUser;
  const qc = useQueryClient();
  const [dialogMode, setDialogMode] = useState<'create' | 'edit' | null>(null);
  const [selected, setSelected] = useState<StorageLocationDto | null>(null);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');

  const { data: locations = [], isLoading } = useQuery({
    queryKey: ['locations'],
    queryFn: locationApi.getAll,
  });

  const { data: devices = [] } = useQuery({
    queryKey: ['iot-devices'],
    queryFn: iotApi.getAll,
  });

  const createMutation = useMutation({
    mutationFn: (d: FormData) => locationApi.create(d),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['locations'] });
      setDialogMode(null);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: FormData }) => locationApi.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['locations'] });
      setDialogMode(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => locationApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['locations'] });
      qc.invalidateQueries({ queryKey: ['iot-devices'] });
      qc.invalidateQueries({ queryKey: ['medicines'] });
    },
  });

  const typeColors: Record<string, string> = {
    Refrigerator: 'info',
    Shelf: 'secondary',
    Warehouse: 'warning',
    Cabinet: 'outline',
    Other: 'outline',
  };

  const filtered = locations.filter((l) => {
    const q = search.toLowerCase();
    const matchSearch =
      !q ||
      l.name.toLowerCase().includes(q) ||
      (l.address ?? '').toLowerCase().includes(q) ||
      (l.ioTDeviceLocation ?? '').toLowerCase().includes(q);
    const matchType = typeFilter === 'all' || l.locationType === typeFilter;
    return matchSearch && matchType;
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t('storageLocationsTitle')}</h1>
          <p className="text-muted-foreground">{t('storageLocationsSubtitle')}</p>
        </div>
        {canManage && (
          <Button
            onClick={() => {
              setSelected(null);
              setDialogMode('create');
            }}>
            <Plus className="h-4 w-4" /> {t('addLocation')}
          </Button>
        )}
      </div>

      {/* Filter bar */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-48">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder={t('locationSearch')}
            className="pl-9"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-xs text-muted-foreground whitespace-nowrap">{t('filterByLocationType')}:</label>
          <select
            aria-label={t('filterByLocationType')}
            className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground"
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}>
            <option value="all">{t('filterAll')}</option>
            {LOCATION_TYPES.map((lt) => (
              <option key={lt} value={lt}>
                {t('locationType' + lt)}
              </option>
            ))}
          </select>
        </div>
        <span className="text-xs text-muted-foreground">
          {t('filteredCount', { filtered: filtered.length, total: locations.length })}
        </span>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {isLoading
          ? Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-32" />)
          : filtered.map((l) => (
              <Card key={l.locationId} className="relative overflow-hidden">
                <div className="absolute right-4 top-4">
                  <Badge
                    variant={
                      (typeColors[l.locationType] ?? 'outline') as
                        | 'info'
                        | 'secondary'
                        | 'warning'
                        | 'outline'
                    }>
                    {t('locationType' + l.locationType)}
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
                  {canManage && (
                    <div className="flex gap-2 pt-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          setSelected(l);
                          setDialogMode('edit');
                        }}>
                        <Pencil className="h-3 w-3" /> {t('edit')}
                      </Button>
                      <AlertDialog>
                        <AlertDialogTrigger asChild>
                          <Button
                            variant="outline"
                            size="sm"
                            className="text-destructive hover:text-destructive">
                            <Trash2 className="h-3 w-3" />
                          </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                          <AlertDialogHeader>
                            <AlertDialogTitle>{t('deleteLocationConfirmTitle')}</AlertDialogTitle>
                            <AlertDialogDescription>
                              {t('deleteLocationConfirmText', { name: l.name })}
                            </AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>{t('cancel')}</AlertDialogCancel>
                            <AlertDialogAction
                              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                              onClick={() => deleteMutation.mutate(l.locationId)}>
                              {t('deleteLocationConfirmBtn')}
                            </AlertDialogAction>
                          </AlertDialogFooter>
                        </AlertDialogContent>
                      </AlertDialog>
                    </div>
                  )}
                </CardContent>
              </Card>
            ))}
        {!isLoading && filtered.length === 0 && (
          <Card className="col-span-3">
            <CardContent className="py-10 text-center text-muted-foreground">
              {locations.length === 0
                ? `${t('noLocations')} ${canManage ? t('addFirst') : ''}`
                : t('noItemsMatchFilter')}
            </CardContent>
          </Card>
        )}
      </div>

      <Dialog open={dialogMode !== null} onOpenChange={(o) => !o && setDialogMode(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {dialogMode === 'create' ? t('createLocation') : t('editLocation')}
            </DialogTitle>
            <DialogDescription className="sr-only">
              {dialogMode === 'create' ? t('createLocation') : t('editLocation')}
            </DialogDescription>
          </DialogHeader>
          <LocationForm
            initial={
              selected
                ? {
                    name: selected.name,
                    address: selected.address,
                    locationType: selected.locationType,
                    ioTDeviceId: selected.ioTDeviceId,
                  }
                : {}
            }
            isLoading={createMutation.isPending || updateMutation.isPending}
            devices={devices}
            onClose={() => setDialogMode(null)}
            onSave={(data) => {
              if (dialogMode === 'create') createMutation.mutate(data);
              else if (selected) updateMutation.mutate({ id: selected.locationId, data });
            }}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}

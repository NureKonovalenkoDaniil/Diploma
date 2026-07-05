import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2, Loader2, Users, ShieldCheck, User as UserIcon, Search } from 'lucide-react';
import { api } from '@/api/client';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
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
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { useLocale } from '@/contexts/LocaleContext';

// ─── Types ────────────────────────────────────────────────────────────────────
interface UserDto {
  id: string;
  email: string;
  userName: string;
  roles: string[];
  organizationId: string;
  organizationName?: string;
}

interface CreateManagerForm {
  email: string;
  password: string;
  confirmPassword: string;
  organizationId: string;
  role: string;
}

// ─── API ──────────────────────────────────────────────────────────────────────
const usersApi = {
  getAll: () => api.get<UserDto[]>('/api/auth/users').then((r) => r.data),
  createManager: (data: { email: string; password: string; organizationId: string; role: string }) =>
    api.post('/api/auth/create-manager', data).then((r) => r.data),
  deleteUser: (id: string) => api.delete(`/api/auth/users/${id}`),
};

// ─── Role badge ───────────────────────────────────────────────────────────────
function RoleBadge({ role }: { role: string }) {
  const { t } = useLocale();
  const map: Record<
    string,
    { label: string; variant: 'default' | 'secondary' | 'outline' | 'destructive' }
  > = {
    Administrator: { label: t('roleAdministrator'), variant: 'destructive' },
    OrganizationAdmin: { label: t('roleOrganizationAdmin'), variant: 'outline' },
    Manager: { label: t('roleManager'), variant: 'default' },
    User: { label: t('roleUser'), variant: 'secondary' },
    Device: { label: t('roleDevice'), variant: 'outline' },
  };
  const cfg = map[role] ?? { label: role, variant: 'outline' };
  return <Badge variant={cfg.variant}>{cfg.label}</Badge>;
}

// ─── Form ─────────────────────────────────────────────────────────────────────
function CreateManagerDialog({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: (email: string) => void;
}) {
  const { user } = useAuth();
  const [form, setForm] = useState<CreateManagerForm>({
    email: '',
    password: '',
    confirmPassword: '',
    organizationId: user?.organizationId ?? '',
    role: 'Manager',
  });
  const [errors, setErrors] = useState<Partial<CreateManagerForm & { server: string }>>({});
  const { t } = useLocale();

  const isGlobalAdmin = user?.roles.includes('Administrator');

  const { data: organizations = [] } = useQuery({
    queryKey: ['organizations'],
    queryFn: () => api.get<any[]>('/api/organization').then((r) => r.data),
    enabled: open && isGlobalAdmin,
  });

  const mutation = useMutation({
    mutationFn: () =>
      usersApi.createManager({
        email: form.email,
        password: form.password,
        organizationId: form.organizationId,
        role: form.role,
      }),
    onSuccess: () => {
      onCreated(form.email);
      onClose();
      setForm({
        email: '',
        password: '',
        confirmPassword: '',
        organizationId: user?.organizationId ?? '',
        role: 'Manager',
      });
      setErrors({});
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.title || err?.response?.data || t('serverErrorDefault');
      setErrors((p) => ({ ...p, server: typeof msg === 'string' ? msg : JSON.stringify(msg) }));
    },
  });

  const validate = () => {
    const e: typeof errors = {};
    if (!form.email.includes('@')) e.email = t('emailInvalid');
    if (form.password.length < 6) e.password = t('passwordMin');
    if (form.password !== form.confirmPassword) e.confirmPassword = t('passwordsMismatch');
    if (!form.organizationId.trim()) e.organizationId = t('organizationIdRequired');
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = () => {
    if (validate()) mutation.mutate();
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>
            {isGlobalAdmin ? 'Створити адміністратора або менеджера' : t('addManagerTitle')}
          </DialogTitle>
          <DialogDescription>
            {isGlobalAdmin 
              ? 'Глобальний адміністратор може створити адміністратора організації або менеджера.' 
              : t('managerDescription')}
          </DialogDescription>
        </DialogHeader>

        {errors.server && (
          <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-2 text-sm text-destructive">
            {errors.server}
          </div>
        )}

        <div className="space-y-4">
          <div className="space-y-1.5">
            <Label className='after:content-["*"] after:ml-0.5 after:text-destructive'>Email</Label>
            <Input
              type="email"
              value={form.email}
              onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))}
              placeholder="user@company.com"
              className={errors.email ? 'border-destructive' : ''}
            />
            {errors.email && <p className="text-xs text-destructive">{errors.email}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className='after:content-["*"] after:ml-0.5 after:text-destructive'>
              {t('password')}
            </Label>
            <Input
              type="password"
              value={form.password}
              onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))}
              className={errors.password ? 'border-destructive' : ''}
            />
            {errors.password && <p className="text-xs text-destructive">{errors.password}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className='after:content-["*"] after:ml-0.5 after:text-destructive'>
              {t('confirmPassword')}
            </Label>
            <Input
              type="password"
              value={form.confirmPassword}
              onChange={(e) => setForm((p) => ({ ...p, confirmPassword: e.target.value }))}
              className={errors.confirmPassword ? 'border-destructive' : ''}
            />
            {errors.confirmPassword && (
              <p className="text-xs text-destructive">{errors.confirmPassword}</p>
            )}
          </div>

          {isGlobalAdmin && (
            <div className="space-y-1.5">
              <Label className='after:content-["*"] after:ml-0.5 after:text-destructive'>
                Роль користувача
              </Label>
              <select
                aria-label="Роль користувача"
                className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                value={form.role}
                onChange={(e) => setForm((p) => ({ ...p, role: e.target.value }))}
              >
                <option value="Manager">Менеджер (Manager)</option>
                <option value="OrganizationAdmin">Адміністратор організації (OrganizationAdmin)</option>
              </select>
            </div>
          )}

          <div className="space-y-1.5">
            <Label className='after:content-["*"] after:ml-0.5 after:text-destructive'>
              {isGlobalAdmin ? 'Організація (Оберіть або введіть нову назву)' : 'OrganizationId'}
            </Label>
            {isGlobalAdmin ? (
              <div className="space-y-2">
                <select
                  aria-label="Обрати існуючу організацію"
                  className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                  onChange={(e) => {
                    if (e.target.value !== 'NEW') {
                      setForm((p) => ({ ...p, organizationId: e.target.value }));
                    } else {
                      setForm((p) => ({ ...p, organizationId: '' }));
                    }
                  }}
                >
                  <option value="">-- Оберіть організацію --</option>
                  {organizations.map((org: any) => (
                    <option key={org.id} value={org.id}>
                      {org.name}
                    </option>
                  ))}
                  <option value="NEW">+ Створити нову організацію...</option>
                </select>
                <Input
                  value={form.organizationId}
                  onChange={(e) => setForm((p) => ({ ...p, organizationId: e.target.value }))}
                  placeholder="Введіть UUID існуючої або назву нової організації"
                  className={errors.organizationId ? 'border-destructive' : ''}
                />
              </div>
            ) : (
              <Input
                value={form.organizationId}
                onChange={(e) => setForm((p) => ({ ...p, organizationId: e.target.value }))}
                placeholder={t('managerEmailPlaceholder')}
                className={errors.organizationId ? 'border-destructive' : ''}
              />
            )}
            {errors.organizationId && (
              <p className="text-xs text-destructive">{errors.organizationId}</p>
            )}
            {!isGlobalAdmin && (
              <p className="text-xs text-muted-foreground">
                {t('managerEmailHint')}
              </p>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            {t('cancel')}
          </Button>
          <Button onClick={handleSubmit} disabled={mutation.isPending}>
            {mutation.isPending && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
            {t('createManagerBtn')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────
export default function UsersPage() {
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');
  const { t } = useLocale();

  const { data: users = [], isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: usersApi.getAll,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => usersApi.deleteUser(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
  });

  const { user } = useAuth();
  const isGlobalAdmin = user?.roles.includes('Administrator');

  const roleOrder = ['Administrator', 'OrganizationAdmin', 'Manager', 'User', 'Device'];
  const sorted = [...users].sort((a, b) => {
    const ar = roleOrder.indexOf(a.roles[0] ?? 'User');
    const br = roleOrder.indexOf(b.roles[0] ?? 'User');
    return ar - br;
  });

  const filtered = sorted.filter((u) => {
    const q = search.toLowerCase();
    const matchSearch =
      !q ||
      u.email.toLowerCase().includes(q) ||
      u.userName.toLowerCase().includes(q);
    const matchRole =
      roleFilter === 'all' ||
      u.roles.includes(roleFilter);
    return matchSearch && matchRole;
  });

  const managers = sorted.filter((u) => u.roles.includes('Manager'));
  const others = sorted.filter(
    (u) => !u.roles.includes('Manager') && !u.roles.includes('Administrator') && !u.roles.includes('OrganizationAdmin'),
  );
  const admins = sorted.filter((u) => u.roles.includes('Administrator') || u.roles.includes('OrganizationAdmin'));

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t('usersTitle')}</h1>
          <p className="text-muted-foreground">{t('usersSubtitle')}</p>
        </div>
        <Button onClick={() => setShowCreate(true)}>
          <Plus className="h-4 w-4 mr-1" /> {isGlobalAdmin ? 'Створити користувача' : t('addManager')}
        </Button>
      </div>

      {successMessage && (
        <div className="rounded-md border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-sm text-emerald-700">
          {successMessage}
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4">
        {[
          {
            label: t('usersSectionAdminsLabel'),
            count: admins.length,
            icon: ShieldCheck,
            color: 'text-destructive',
          },
          { label: t('usersSectionManagersLabel'), count: managers.length, icon: Users, color: 'text-primary' },
          {
            label: t('usersSectionOthersLabel'),
            count: others.length,
            icon: UserIcon,
            color: 'text-muted-foreground',
          },
        ].map(({ label, count, icon: Icon, color }) => (
          <Card key={label}>
            <CardContent className="flex items-center gap-4 pt-6">
              <Icon className={`h-8 w-8 ${color}`} />
              <div>
                <p className="text-2xl font-bold">{count}</p>
                <p className="text-sm text-muted-foreground">{label}</p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Table */}
      <Card>
        <CardHeader>
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex-1">
              <CardTitle>{t('allUsersTitle')}</CardTitle>
              <CardDescription>
                {isGlobalAdmin ? 'Всі облікові записи в системі' : t('onlyOrgUsersDesc')}
              </CardDescription>
            </div>
            <div className="relative min-w-48">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder={t('userSearch')}
                className="pl-9 h-9"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="text-xs text-muted-foreground whitespace-nowrap">{t('filterByRole')}:</label>
              <select
                aria-label={t('filterByRole')}
                className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground"
                value={roleFilter}
                onChange={(e) => setRoleFilter(e.target.value)}>
                <option value="all">{t('filterAll')}</option>
                <option value="Administrator">{t('roleAdministrator')}</option>
                <option value="OrganizationAdmin">{t('roleOrganizationAdmin')}</option>
                <option value="Manager">{t('roleManager')}</option>
                <option value="User">{t('roleUser')}</option>
                <option value="Device">{t('roleDevice')}</option>
              </select>
            </div>
            <span className="text-xs text-muted-foreground">
              {t('filteredCount', { filtered: filtered.length, total: sorted.length })}
            </span>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6 text-center text-muted-foreground text-sm">{t('loading')}</div>
          ) : sorted.length === 0 ? (
            <div className="p-6 text-center text-muted-foreground text-sm">
              {t('noUsersFound')}
            </div>
          ) : filtered.length === 0 ? (
            <div className="p-6 text-center text-muted-foreground text-sm">
              {t('noItemsMatchFilter')}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Email</TableHead>
                  <TableHead>{t('colRoles')}</TableHead>
                  <TableHead>Організація</TableHead>
                  <TableHead className="text-right">{t('colActions')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.map((u) => (
                  <TableRow key={u.id}>
                    <TableCell className="font-medium">{u.email}</TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        {u.roles.map((r) => (
                          <RoleBadge key={r} role={r} />
                        ))}
                      </div>
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {u.organizationName || u.organizationId || '-'}
                    </TableCell>
                    <TableCell className="text-right">
                      {!u.roles.includes('Administrator') && (
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-destructive hover:text-destructive"
                          disabled={deleteMutation.isPending}
                          onClick={() => {
                            if (confirm(t('deleteUserConfirm', { email: u.email }))) {
                              deleteMutation.mutate(u.id);
                            }
                          }}>
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <CreateManagerDialog
        open={showCreate}
        onClose={() => setShowCreate(false)}
        onCreated={(email) => {
          qc.invalidateQueries({ queryKey: ['users'] });
          setSuccessMessage(t('managerCreatedSuccess', { email }));
        }}
      />
    </div>
  );
}

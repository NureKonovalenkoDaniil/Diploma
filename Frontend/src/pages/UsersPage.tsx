import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2, Loader2, Users, ShieldCheck, User as UserIcon } from 'lucide-react'
import { api } from '@/api/client'
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog'

// ─── Types ────────────────────────────────────────────────────────────────────
interface UserDto {
  id: string
  email: string
  userName: string
  roles: string[]
  organizationId: string
}

interface CreateManagerForm {
  email: string
  password: string
  confirmPassword: string
  organizationId: string
}

// ─── API ──────────────────────────────────────────────────────────────────────
const usersApi = {
  getAll: () => api.get<UserDto[]>('/api/auth/users').then(r => r.data),
  createManager: (data: { email: string; password: string; organizationId: string }) =>
    api.post('/api/auth/create-manager', data).then(r => r.data),
  deleteUser: (id: string) => api.delete(`/api/auth/users/${id}`),
}

// ─── Role badge ───────────────────────────────────────────────────────────────
function RoleBadge({ role }: { role: string }) {
  const map: Record<string, { label: string; variant: 'default' | 'secondary' | 'outline' | 'destructive' }> = {
    Administrator: { label: 'Адміністратор', variant: 'destructive' },
    Manager:       { label: 'Менеджер',       variant: 'default'     },
    User:          { label: 'Користувач',     variant: 'secondary'   },
    Device:        { label: 'Пристрій',       variant: 'outline'     },
  }
  const cfg = map[role] ?? { label: role, variant: 'outline' }
  return <Badge variant={cfg.variant}>{cfg.label}</Badge>
}

// ─── Form ─────────────────────────────────────────────────────────────────────
function CreateManagerDialog({
  open,
  onClose,
  onCreated,
}: {
  open: boolean
  onClose: () => void
  onCreated: () => void
}) {
  const { user } = useAuth()
  const [form, setForm] = useState<CreateManagerForm>({
    email: '',
    password: '',
    confirmPassword: '',
    organizationId: user?.organizationId ?? '',
  })
  const [errors, setErrors] = useState<Partial<CreateManagerForm & { server: string }>>({})

  const mutation = useMutation({
    mutationFn: () => usersApi.createManager({
      email: form.email,
      password: form.password,
      organizationId: form.organizationId,
    }),
    onSuccess: () => {
      onCreated()
      onClose()
      setForm({ email: '', password: '', confirmPassword: '', organizationId: user?.organizationId ?? '' })
      setErrors({})
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.title || err?.response?.data || 'Помилка сервера.'
      setErrors(p => ({ ...p, server: typeof msg === 'string' ? msg : JSON.stringify(msg) }))
    },
  })

  const validate = () => {
    const e: typeof errors = {}
    if (!form.email.includes('@'))    e.email = 'Введіть коректний email'
    if (form.password.length < 6)     e.password = 'Мінімум 6 символів'
    if (form.password !== form.confirmPassword) e.confirmPassword = 'Паролі не збігаються'
    if (!form.organizationId.trim())  e.organizationId = 'OrganizationId є обов\'язковим'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const handleSubmit = () => { if (validate()) mutation.mutate() }

  return (
    <Dialog open={open} onOpenChange={o => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Додати менеджера</DialogTitle>
          <DialogDescription>
            Менеджер матиме доступ до управління препаратами, локаціями та пристроями у межах вашої організації.
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
              onChange={e => setForm(p => ({ ...p, email: e.target.value }))}
              placeholder="manager@company.com"
              className={errors.email ? 'border-destructive' : ''}
            />
            {errors.email && <p className="text-xs text-destructive">{errors.email}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className='after:content-["*"] after:ml-0.5 after:text-destructive'>Пароль</Label>
            <Input
              type="password"
              value={form.password}
              onChange={e => setForm(p => ({ ...p, password: e.target.value }))}
              className={errors.password ? 'border-destructive' : ''}
            />
            {errors.password && <p className="text-xs text-destructive">{errors.password}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className='after:content-["*"] after:ml-0.5 after:text-destructive'>Підтвердження паролю</Label>
            <Input
              type="password"
              value={form.confirmPassword}
              onChange={e => setForm(p => ({ ...p, confirmPassword: e.target.value }))}
              className={errors.confirmPassword ? 'border-destructive' : ''}
            />
            {errors.confirmPassword && <p className="text-xs text-destructive">{errors.confirmPassword}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className='after:content-["*"] after:ml-0.5 after:text-destructive'>OrganizationId</Label>
            <Input
              value={form.organizationId}
              onChange={e => setForm(p => ({ ...p, organizationId: e.target.value }))}
              placeholder="UUID організації"
              className={errors.organizationId ? 'border-destructive' : ''}
            />
            {errors.organizationId && <p className="text-xs text-destructive">{errors.organizationId}</p>}
            <p className="text-xs text-muted-foreground">
              Підставляється з вашого профілю автоматично. Змінювати лише при потребі.
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Скасувати</Button>
          <Button onClick={handleSubmit} disabled={mutation.isPending}>
            {mutation.isPending && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
            Створити менеджера
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ─── Page ─────────────────────────────────────────────────────────────────────
export default function UsersPage() {
  const qc = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)

  const { data: users = [], isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: usersApi.getAll,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => usersApi.deleteUser(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
  })

  const roleOrder = ['Administrator', 'Manager', 'User', 'Device']
  const sorted = [...users].sort((a, b) => {
    const ar = roleOrder.indexOf(a.roles[0] ?? 'User')
    const br = roleOrder.indexOf(b.roles[0] ?? 'User')
    return ar - br
  })

  const managers = sorted.filter(u => u.roles.includes('Manager'))
  const others   = sorted.filter(u => !u.roles.includes('Manager') && !u.roles.includes('Administrator'))
  const admins   = sorted.filter(u => u.roles.includes('Administrator'))

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Управління користувачами</h1>
          <p className="text-muted-foreground">Перегляд та управління акаунтами вашої організації</p>
        </div>
        <Button onClick={() => setShowCreate(true)}>
          <Plus className="h-4 w-4 mr-1" /> Додати менеджера
        </Button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4">
        {[
          { label: 'Адміністратори', count: admins.length,   icon: ShieldCheck, color: 'text-destructive'   },
          { label: 'Менеджери',      count: managers.length, icon: Users,        color: 'text-primary'       },
          { label: 'Користувачі',    count: others.length,   icon: UserIcon,     color: 'text-muted-foreground' },
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
          <CardTitle>Всі користувачі</CardTitle>
          <CardDescription>Лише користувачі вашої організації</CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6 text-center text-muted-foreground text-sm">Завантаження...</div>
          ) : sorted.length === 0 ? (
            <div className="p-6 text-center text-muted-foreground text-sm">Користувачів не знайдено</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Email</TableHead>
                  <TableHead>Ролі</TableHead>
                  <TableHead>OrganizationId</TableHead>
                  <TableHead className="text-right">Дії</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {sorted.map(u => (
                  <TableRow key={u.id}>
                    <TableCell className="font-medium">{u.email}</TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        {u.roles.map(r => <RoleBadge key={r} role={r} />)}
                      </div>
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground font-mono">{u.organizationId}</TableCell>
                    <TableCell className="text-right">
                      {!u.roles.includes('Administrator') && (
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-destructive hover:text-destructive"
                          disabled={deleteMutation.isPending}
                          onClick={() => {
                            if (confirm(`Видалити користувача ${u.email}?`)) {
                              deleteMutation.mutate(u.id)
                            }
                          }}
                        >
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
        onCreated={() => qc.invalidateQueries({ queryKey: ['users'] })}
      />
    </div>
  )
}

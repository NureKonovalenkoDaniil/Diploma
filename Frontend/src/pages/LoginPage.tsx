import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { FlaskConical, Eye, EyeOff, Loader2 } from 'lucide-react';
import { authApi } from '@/api';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

const schema = z.object({
  email: z.string().email('Введіть коректний email'),
  password: z.string().min(1, 'Введіть пароль'),
});

type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorType, setErrorType] = useState<'unconfirmed' | 'invalid' | null>(null);
  const [resendStatus, setResendStatus] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    setError(null);
    setErrorType(null);
    setResendStatus(null);
    try {
      const res = await authApi.login(data);
      await login(res.token);
      navigate('/dashboard');
    } catch (err: any) {
      const status = err?.response?.status;
      if (status === 403) {
        setError('Потрібно підтвердити пошту перед входом');
        setErrorType('unconfirmed');
        return;
      }
      setError('Невірний email або пароль');
      setErrorType('invalid');
    }
  };

  const resendConfirmation = async () => {
    setResendStatus(null);
    const email = getValues('email');
    if (!email) {
      setResendStatus('Вкажіть email у полі вище');
      return;
    }
    try {
      await authApi.resendConfirmation(email);
      setResendStatus('Лист підтвердження надіслано');
    } catch {
      setResendStatus('Не вдалося надіслати лист. Спробуйте пізніше.');
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-background to-muted/40 p-4">
      <div className="w-full max-w-md space-y-6">
        {/* Logo */}
        <div className="flex flex-col items-center gap-3">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary shadow-lg">
            <FlaskConical className="h-7 w-7 text-primary-foreground" />
          </div>
          <div className="text-center">
            <h1 className="text-2xl font-bold">MedStorage</h1>
            <p className="text-sm text-muted-foreground">
              Система управління медичними препаратами
            </p>
          </div>
        </div>

        {/* Form */}
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle>Вхід у систему</CardTitle>
            <CardDescription>Введіть ваші облікові дані для доступу</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="admin@example.com"
                  autoComplete="email"
                  {...register('email')}
                />
                {errors.email && <p className="text-xs text-destructive">{errors.email.message}</p>}
              </div>

              <div className="space-y-2">
                <Label htmlFor="password">Пароль</Label>
                <div className="relative">
                  <Input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    placeholder="••••••••"
                    autoComplete="current-password"
                    {...register('password')}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((p) => !p)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {errors.password && (
                  <p className="text-xs text-destructive">{errors.password.message}</p>
                )}
              </div>

              {error && (
                <div className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
                  {error}
                </div>
              )}

              {errorType === 'unconfirmed' && (
                <div className="space-y-2">
                  <Button type="button" variant="outline" onClick={resendConfirmation}>
                    Надіслати лист підтвердження ще раз
                  </Button>
                  {resendStatus && <p className="text-xs text-muted-foreground">{resendStatus}</p>}
                </div>
              )}

              <Button type="submit" className="w-full" disabled={isSubmitting}>
                {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                Увійти
              </Button>
            </form>
          </CardContent>
        </Card>

        <p className="text-center text-sm text-muted-foreground">
          Немає акаунту?{' '}
          <Link to="/register" className="text-primary hover:underline font-medium">
            Зареєструватися
          </Link>
        </p>
        <p className="text-center text-sm text-muted-foreground">
          Забули пароль?{' '}
          <Link to="/forgot-password" className="text-primary hover:underline font-medium">
            Відновити
          </Link>
        </p>
      </div>
    </div>
  );
}

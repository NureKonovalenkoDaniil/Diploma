import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { FlaskConical, Eye, EyeOff, Loader2 } from 'lucide-react';
import { api } from '@/api/client';
import { authApi } from '@/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

import { useLocale } from '@/contexts/LocaleContext';

type FormData = {
  email: string;
  password: string;
  confirmPassword?: string;
};

export default function RegisterPage() {
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [successEmail, setSuccessEmail] = useState<string | null>(null);
  const [resendStatus, setResendStatus] = useState<string | null>(null);
  const { t } = useLocale();

  const schema = z
    .object({
      email: z.string().email(t('registerEmailInvalid')),
      password: z.string().min(4, t('registerPasswordMin')),
      confirmPassword: z.string(),
    })
    .refine((d) => d.password === d.confirmPassword, {
      message: t('registerPasswordsMismatch'),
      path: ['confirmPassword'],
    });

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: z.infer<typeof schema>) => {
    setError(null);
    try {
      await api.post('/api/auth/register', {
        email: data.email,
        password: data.password,
      });
      setSuccess(true);
      setSuccessEmail(data.email);
    } catch (err: unknown) {
      const e = err as { response?: { data?: string | object; status?: number } };
      if (e.response?.status === 409) {
        setError(t('registerEmailExists'));
      } else {
        setError(t('registerErrorDefault'));
      }
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
              {t('registerAppDesc')}
            </p>
          </div>
        </div>

        {/* Form */}
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle>{t('register')}</CardTitle>
            <CardDescription>
              {t('registerRoleAdminHint')}{' '}
              <span className="font-semibold text-foreground">{t('registerAdminSpan')}</span>
            </CardDescription>
          </CardHeader>
          <CardContent>
            {success ? (
              <div className="flex flex-col items-center gap-3 py-4 text-center">
                <div className="text-4xl">✅</div>
                <p className="font-semibold">{t('registerSuccessTitle')}</p>
                <p className="text-sm text-muted-foreground">
                  {t('registerSuccessDesc')}
                </p>
                <Button className="w-full" onClick={() => navigate('/confirm-email')}>
                  {t('enterCodeBtn')}
                </Button>
                <Button
                  variant="outline"
                  onClick={async () => {
                    if (!successEmail) return;
                    setResendStatus(null);
                    try {
                      await authApi.resendConfirmation(successEmail);
                      setResendStatus(t('resendSuccess'));
                    } catch {
                      setResendStatus(t('resendFailed'));
                    }
                  }}>
                  {t('resendCodeBtn')}
                </Button>
                {resendStatus && <p className="text-xs text-muted-foreground">{resendStatus}</p>}
              </div>
            ) : (
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="reg-email">Email</Label>
                  <Input
                    id="reg-email"
                    type="email"
                    placeholder="admin@example.com"
                    autoComplete="email"
                    {...register('email')}
                  />
                  {errors.email && (
                    <p className="text-xs text-destructive">{errors.email.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="reg-password">{t('passwordLabel')}</Label>
                  <div className="relative">
                    <Input
                      id="reg-password"
                      type={showPassword ? 'text' : 'password'}
                      placeholder={t('passwordMinHint')}
                      autoComplete="new-password"
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

                <div className="space-y-2">
                  <Label htmlFor="reg-confirm">{t('confirmPasswordLabel')}</Label>
                  <Input
                    id="reg-confirm"
                    type={showPassword ? 'text' : 'password'}
                    placeholder="••••••••"
                    autoComplete="new-password"
                    {...register('confirmPassword')}
                  />
                  {errors.confirmPassword && (
                    <p className="text-xs text-destructive">{errors.confirmPassword.message}</p>
                  )}
                </div>

                {error && (
                  <div className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
                    {error}
                  </div>
                )}

                <Button type="submit" className="w-full" disabled={isSubmitting}>
                  {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                  {t('registerBtn')}
                </Button>
              </form>
            )}
          </CardContent>
        </Card>

        <p className="text-center text-sm text-muted-foreground">
          {t('alreadyHaveAccount')}{' '}
          <Link to="/login" className="text-primary hover:underline font-medium">
            {t('loginLink')}
          </Link>
        </p>
      </div>
    </div>
  );
}

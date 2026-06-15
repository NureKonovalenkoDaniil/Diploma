import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { authApi } from '@/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useLocale } from '@/contexts/LocaleContext';

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [status, setStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const [message, setMessage] = useState<string | null>(null);
  const { t } = useLocale();

  const email = params.get('email') || '';
  const [code, setCode] = useState('');

  const submit = async () => {
    setMessage(null);
    if (!email || !code) {
      setStatus('error');
      setMessage(t('confirmationCode'));
      return;
    }
    if (!password || password.length < 4) {
      setStatus('error');
      setMessage('Password must contain at least 4 characters.');
      return;
    }
    if (password !== confirm) {
      setStatus('error');
      setMessage('Passwords do not match.');
      return;
    }

    try {
      await authApi.resetPassword({ email, code, newPassword: password });
      setStatus('success');
    } catch {
      setStatus('error');
      setMessage(t('confirmationFailed'));
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-background to-muted/40 p-4">
      <div className="w-full max-w-md space-y-6">
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle>{t('resetPasswordTitle')}</CardTitle>
            <CardDescription>{t('resetPasswordSubtitle')}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {status === 'success' ? (
              <div className="space-y-2 text-sm">
                <p className="text-emerald-600 font-medium">{t('passwordUpdated')}</p>
                <Button asChild variant="outline">
                  <Link to="/login">{t('goToLogin')}</Link>
                </Button>
              </div>
            ) : (
              <>
                <div className="space-y-2">
                  <Label htmlFor="rp-code">{t('recoveryCode')}</Label>
                  <Input
                    id="rp-code"
                    type="text"
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    maxLength={6}
                    placeholder="123456"
                    className="text-center tracking-widest text-lg"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="rp-password">{t('newPassword')}</Label>
                  <Input
                    id="rp-password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    autoComplete="new-password"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="rp-confirm">{t('confirmPassword')}</Label>
                  <Input
                    id="rp-confirm"
                    type="password"
                    value={confirm}
                    onChange={(e) => setConfirm(e.target.value)}
                    autoComplete="new-password"
                  />
                </div>
                {message && (
                  <div className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
                    {message}
                  </div>
                )}
                <Button className="w-full" onClick={submit}>
                  {t('changePassword')}
                </Button>
                <Button asChild variant="outline" className="w-full">
                  <Link to="/login">{t('backToLogin')}</Link>
                </Button>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

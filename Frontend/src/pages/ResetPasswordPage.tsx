import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { authApi } from '@/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [status, setStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const [message, setMessage] = useState<string | null>(null);

  const email = params.get('email') || '';
  const token = params.get('token') || '';

  const submit = async () => {
    setMessage(null);
    if (!email || !token) {
      setStatus('error');
      setMessage('Невірне посилання для скидання пароля.');
      return;
    }
    if (!password || password.length < 4) {
      setStatus('error');
      setMessage('Пароль має містити щонайменше 4 символи.');
      return;
    }
    if (password !== confirm) {
      setStatus('error');
      setMessage('Паролі не збігаються.');
      return;
    }

    try {
      await authApi.resetPassword({ email, token, newPassword: password });
      setStatus('success');
    } catch {
      setStatus('error');
      setMessage('Не вдалося змінити пароль. Спробуйте знову.');
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-background to-muted/40 p-4">
      <div className="w-full max-w-md space-y-6">
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle>Скидання пароля</CardTitle>
            <CardDescription>Введіть новий пароль</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {status === 'success' ? (
              <div className="space-y-2 text-sm">
                <p className="text-emerald-600 font-medium">Пароль оновлено успішно.</p>
                <Button asChild variant="outline">
                  <Link to="/login">Перейти до входу</Link>
                </Button>
              </div>
            ) : (
              <>
                <div className="space-y-2">
                  <Label htmlFor="rp-password">Новий пароль</Label>
                  <Input
                    id="rp-password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    autoComplete="new-password"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="rp-confirm">Підтвердження пароля</Label>
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
                  Змінити пароль
                </Button>
                <Button asChild variant="outline" className="w-full">
                  <Link to="/login">Повернутися до входу</Link>
                </Button>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

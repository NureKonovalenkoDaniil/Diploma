import { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '@/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState<'idle' | 'sent' | 'error'>('idle');
  const [message, setMessage] = useState<string | null>(null);

  const submit = async () => {
    setMessage(null);
    try {
      await authApi.forgotPassword(email);
      setStatus('sent');
    } catch {
      setStatus('error');
      setMessage('Не вдалося надіслати лист. Спробуйте ще раз.');
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-background to-muted/40 p-4">
      <div className="w-full max-w-md space-y-6">
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle>Відновлення пароля</CardTitle>
            <CardDescription>Вкажіть email, щоб отримати посилання для скидання</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {status === 'sent' ? (
              <div className="space-y-2 text-sm">
                <p className="text-emerald-600 font-medium">Лист для відновлення надіслано.</p>
                <p className="text-muted-foreground">Перевірте пошту та перейдіть за посиланням.</p>
                <Button asChild variant="outline">
                  <Link to="/login">Повернутися до входу</Link>
                </Button>
              </div>
            ) : (
              <>
                <div className="space-y-2">
                  <Label htmlFor="fp-email">Email</Label>
                  <Input
                    id="fp-email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="user@example.com"
                    autoComplete="email"
                  />
                </div>
                {message && (
                  <div className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
                    {message}
                  </div>
                )}
                <Button className="w-full" onClick={submit} disabled={!email}>
                  Надіслати лист
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

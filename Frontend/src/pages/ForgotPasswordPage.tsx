import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '@/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useLocale } from '@/contexts/LocaleContext';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState<'idle' | 'sent' | 'error'>('idle');
  const [message, setMessage] = useState<string | null>(null);
  const navigate = useNavigate();
  const { t } = useLocale();

  const submit = async () => {
    setMessage(null);
    try {
      await authApi.forgotPassword(email);
      setStatus('sent');
      navigate(`/reset-password?email=${encodeURIComponent(email)}`);
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
            <CardTitle>{t('forgotPasswordTitle')}</CardTitle>
            <CardDescription>{t('forgotPasswordSubtitle')}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {status === 'sent' ? (
              <div className="space-y-2 text-sm">
                <p className="text-emerald-600 font-medium">{t('confirmationSent')}</p>
                <p className="text-muted-foreground">...</p>
              </div>
            ) : (
              <>
                <div className="space-y-2">
                  <Label htmlFor="fp-email">{t('email')}</Label>
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
                  {t('sendEmail')}
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

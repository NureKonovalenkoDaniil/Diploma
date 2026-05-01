import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { authApi } from '@/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

export default function ConfirmEmailPage() {
  const [params] = useSearchParams();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');

  useEffect(() => {
    const userId = params.get('userId');
    const token = params.get('token');

    if (!userId || !token) {
      setStatus('error');
      return;
    }

    authApi
      .confirmEmail(userId, token)
      .then(() => setStatus('success'))
      .catch(() => setStatus('error'));
  }, [params]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-background to-muted/40 p-4">
      <div className="w-full max-w-md">
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle>Підтвердження email</CardTitle>
            <CardDescription>Перевіряємо посилання підтвердження</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {status === 'loading' && <p>Обробка запиту...</p>}
            {status === 'success' && (
              <div className="space-y-2">
                <p className="text-emerald-600 font-medium">Email підтверджено успішно.</p>
                <Button asChild>
                  <Link to="/login">Перейти до входу</Link>
                </Button>
              </div>
            )}
            {status === 'error' && (
              <div className="space-y-2">
                <p className="text-destructive font-medium">
                  Не вдалося підтвердити email. Перевірте посилання або повторіть спробу.
                </p>
                <Button variant="outline" asChild>
                  <Link to="/login">Повернутися до входу</Link>
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

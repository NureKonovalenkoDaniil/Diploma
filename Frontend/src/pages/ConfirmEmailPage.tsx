import { useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { authApi } from '@/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
// Fallback toast for environments where '@/components/ui/use-toast' is unavailable

export default function ConfirmEmailPage() {
  const [code, setCode] = useState('');
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const toast = ({ title, description }: { title?: string; description?: string }) => {
    // simple fallback using alert so the page still provides feedback
    alert(`${title ? title + '\n' : ''}${description ?? ''}`);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !code) {
      toast({ title: 'Помилка', description: 'Заповніть всі поля' });
      return;
    }

    try {
      setLoading(true);
      await authApi.confirmEmail({ email, code });
      toast({ title: 'Успіх', description: 'Email підтверджено успішно. Тепер ви можете увійти.' });
      navigate('/login');
    } catch (error: any) {
      toast({
        title: 'Помилка',
        description: error.response?.data || 'Невірний код або помилка сервера',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-background to-muted/40 p-4">
      <div className="w-full max-w-md">
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle>Підтвердження email</CardTitle>
            <CardDescription>Введіть 6-значний код, надісланий на вашу пошту</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Ваш Email</label>
                <Input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  required
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Код підтвердження</label>
                <Input
                  type="text"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="123456"
                  maxLength={6}
                  className="text-center text-lg tracking-widest"
                  required
                />
              </div>
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? 'Перевірка...' : 'Підтвердити'}
              </Button>
              <div className="text-center mt-4 text-sm">
                <Link to="/login" className="text-primary hover:underline">
                  Повернутися до входу
                </Link>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

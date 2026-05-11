import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth } from './AuthContext';
import { QueryClient } from '@tanstack/react-query';
import { authApi } from '@/api';

vi.mock('@/api', () => ({
  authApi: {
    me: vi.fn(),
  },
}));

vi.mock('jwt-decode', () => ({
  jwtDecode: vi.fn((token: string) => {
    if (token === 'admin-token') {
      return { 'http://schemas.microsoft.com/ws/2008/06/identity/claims/role': 'Administrator' };
    }
    if (token === 'manager-token') {
      return { 'http://schemas.microsoft.com/ws/2008/06/identity/claims/role': 'Manager' };
    }
    if (token === 'user-token') {
      return { 'http://schemas.microsoft.com/ws/2008/06/identity/claims/role': 'User' };
    }
    return { role: 'User' };
  }),
}));

function TestComponent() {
  const auth = useAuth();
  return (
    <div>
      <div data-testid="user">{auth.user ? auth.user.email : 'null'}</div>
      <div data-testid="token">{auth.token || 'null'}</div>
      <div data-testid="role">{auth.role || 'null'}</div>
      <div data-testid="isAdmin">{auth.isAdmin ? 'true' : 'false'}</div>
      <div data-testid="isManager">{auth.isManager ? 'true' : 'false'}</div>
      <div data-testid="isLoading">{auth.isLoading ? 'true' : 'false'}</div>
      <button onClick={() => auth.login('admin-token')}>Login</button>
      <button onClick={auth.logout}>Logout</button>
    </div>
  );
}

describe('AuthContext', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });
    localStorage.clear();
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should throw error when useAuth is used outside AuthProvider', () => {
    expect(() => render(<TestComponent />)).toThrow('useAuth must be inside AuthProvider');
  });

  it('should initialize with no user when localStorage is empty', async () => {
    vi.mocked(authApi.me).mockResolvedValue({
      id: '1',
      userName: 'testuser',
      email: 'test@example.com',
      roles: ['User'],
      organizationId: 'org-1',
    });

    render(
      <AuthProvider queryClient={queryClient}>
        <TestComponent />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('isLoading').textContent).toBe('false');
    });

    expect(screen.getByTestId('user').textContent).toBe('null');
    expect(screen.getByTestId('token').textContent).toBe('null');
    expect(screen.getByTestId('role').textContent).toBe('null');
  });

  it('should restore session from localStorage on mount', async () => {
    localStorage.setItem('token', 'admin-token');
    vi.mocked(authApi.me).mockResolvedValue({
      id: '1',
      userName: 'admin',
      email: 'admin@example.com',
      roles: ['Administrator'],
      organizationId: 'org-1',
    });

    render(
      <AuthProvider queryClient={queryClient}>
        <TestComponent />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('isLoading').textContent).toBe('false');
    });

    expect(screen.getByTestId('user').textContent).toBe('admin@example.com');
    expect(screen.getByTestId('token').textContent).toBe('admin-token');
    expect(screen.getByTestId('role').textContent).toBe('Administrator');
    expect(screen.getByTestId('isAdmin').textContent).toBe('true');
  });

  it('should parse Administrator role correctly', async () => {
    localStorage.setItem('token', 'admin-token');
    vi.mocked(authApi.me).mockResolvedValue({
      id: '1',
      userName: 'admin',
      email: 'admin@example.com',
      roles: ['Administrator'],
      organizationId: 'org-1',
    });

    render(
      <AuthProvider queryClient={queryClient}>
        <TestComponent />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('role').textContent).toBe('Administrator');
    });

    expect(screen.getByTestId('isAdmin').textContent).toBe('true');
    expect(screen.getByTestId('isManager').textContent).toBe('true');
  });

  it('should parse Manager role correctly', async () => {
    localStorage.setItem('token', 'manager-token');
    vi.mocked(authApi.me).mockResolvedValue({
      id: '2',
      userName: 'manager',
      email: 'manager@example.com',
      roles: ['Manager'],
      organizationId: 'org-1',
    });

    render(
      <AuthProvider queryClient={queryClient}>
        <TestComponent />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('role').textContent).toBe('Manager');
    });

    expect(screen.getByTestId('isAdmin').textContent).toBe('true');
    expect(screen.getByTestId('isManager').textContent).toBe('true');
  });

  it('should parse User role correctly', async () => {
    localStorage.setItem('token', 'user-token');
    vi.mocked(authApi.me).mockResolvedValue({
      id: '3',
      userName: 'user',
      email: 'user@example.com',
      roles: ['User'],
      organizationId: 'org-1',
    });

    render(
      <AuthProvider queryClient={queryClient}>
        <TestComponent />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('role').textContent).toBe('User');
    });

    expect(screen.getByTestId('isAdmin').textContent).toBe('true');
    expect(screen.getByTestId('isManager').textContent).toBe('true');
  });

  it('should handle login and set user data', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.me).mockResolvedValue({
      id: '1',
      userName: 'newuser',
      email: 'newuser@example.com',
      roles: ['Manager'],
      organizationId: 'org-1',
    });

    render(
      <AuthProvider queryClient={queryClient}>
        <TestComponent />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('isLoading').textContent).toBe('false');
    });

    await user.click(screen.getByText('Login'));

    await waitFor(() => {
      expect(screen.getByTestId('user').textContent).toBe('newuser@example.com');
    });

    expect(screen.getByTestId('token').textContent).toBe('admin-token');
    expect(screen.getByTestId('role').textContent).toBe('Administrator');
    expect(localStorage.getItem('token')).toBe('admin-token');
  });

  it('should handle logout and clear user data', async () => {
    const user = userEvent.setup();
    localStorage.setItem('token', 'admin-token');
    vi.mocked(authApi.me).mockResolvedValue({
      id: '1',
      userName: 'admin',
      email: 'admin@example.com',
      roles: ['Administrator'],
      organizationId: 'org-1',
    });

    render(
      <AuthProvider queryClient={queryClient}>
        <TestComponent />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('user').textContent).toBe('admin@example.com');
    });

    await user.click(screen.getByText('Logout'));

    await waitFor(() => {
      expect(screen.getByTestId('user').textContent).toBe('null');
    });

    expect(screen.getByTestId('token').textContent).toBe('null');
    expect(screen.getByTestId('role').textContent).toBe('null');
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('should clear localStorage and state when authApi.me fails on mount', async () => {
    localStorage.setItem('token', 'invalid-token');
    vi.mocked(authApi.me).mockRejectedValue(new Error('Unauthorized'));

    render(
      <AuthProvider queryClient={queryClient}>
        <TestComponent />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('isLoading').textContent).toBe('false');
    });

    expect(screen.getByTestId('user').textContent).toBe('null');
    expect(screen.getByTestId('token').textContent).toBe('null');
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('should clear queryClient cache on login', async () => {
    const user = userEvent.setup();
    const clearSpy = vi.spyOn(queryClient, 'clear');
    vi.mocked(authApi.me).mockResolvedValue({
      id: '1',
      userName: 'testuser',
      email: 'test@example.com',
      roles: ['User'],
      organizationId: 'org-1',
    });

    render(
      <AuthProvider queryClient={queryClient}>
        <TestComponent />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('isLoading').textContent).toBe('false');
    });

    await user.click(screen.getByText('Login'));

    await waitFor(() => {
      expect(clearSpy).toHaveBeenCalled();
    });
  });

  it('should clear queryClient cache on logout', async () => {
    const user = userEvent.setup();
    localStorage.setItem('token', 'admin-token');
    const clearSpy = vi.spyOn(queryClient, 'clear');
    vi.mocked(authApi.me).mockResolvedValue({
      id: '1',
      userName: 'admin',
      email: 'admin@example.com',
      roles: ['Administrator'],
      organizationId: 'org-1',
    });

    render(
      <AuthProvider queryClient={queryClient}>
        <TestComponent />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('user').textContent).toBe('admin@example.com');
    });

    await user.click(screen.getByText('Logout'));

    await waitFor(() => {
      expect(clearSpy).toHaveBeenCalled();
    });
  });
});

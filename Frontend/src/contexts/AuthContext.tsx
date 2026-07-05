import { createContext, useContext, useState, useEffect } from 'react'
import type { ReactNode } from 'react'
import type { UserProfile } from '@/types/api'
import { authApi } from '@/api'
import { jwtDecode } from 'jwt-decode'
import type { QueryClient } from '@tanstack/react-query'

interface AuthContextType {
  user: UserProfile | null
  token: string | null
  login: (token: string) => Promise<void>
  logout: () => void
  isAdmin: boolean
  isManager: boolean
  isUser: boolean
  role: 'Administrator' | 'OrganizationAdmin' | 'Manager' | 'User' | 'Device' | null
  isLoading: boolean
}

const AuthContext = createContext<AuthContextType | null>(null)

interface AuthProviderProps {
  children: ReactNode
  queryClient: QueryClient
}

export function AuthProvider({ children, queryClient }: AuthProviderProps) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'))
  const [role, setRole] = useState<'Administrator' | 'OrganizationAdmin' | 'Manager' | 'User' | 'Device' | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const parseAndSetRole = (jwtToken: string) => {
    try {
      const decoded = jwtDecode<any>(jwtToken)
      let parsedRole =
        decoded['http://schemas.microsoft.com/ws/2008/06/identity/claims/role'] ||
        decoded['role'] ||
        'User'
      
      if (Array.isArray(parsedRole)) {
        if (parsedRole.includes('Administrator')) parsedRole = 'Administrator';
        else if (parsedRole.includes('OrganizationAdmin')) parsedRole = 'OrganizationAdmin';
        else if (parsedRole.includes('Manager')) parsedRole = 'Manager';
        else if (parsedRole.includes('User')) parsedRole = 'User';
        else parsedRole = parsedRole[0];
      }
      
      setRole(parsedRole as 'Administrator' | 'OrganizationAdmin' | 'Manager' | 'User' | 'Device')
    } catch {
      setRole(null)
    }
  }

  useEffect(() => {
    const storedToken = localStorage.getItem('token')
    if (storedToken) {
      parseAndSetRole(storedToken)
      authApi.me()
        .then(setUser)
        .catch(() => {
          localStorage.removeItem('token')
          setToken(null)
        })
        .finally(() => setIsLoading(false))
    } else {
      setIsLoading(false)
    }
  }, [])

  const login = async (newToken: string) => {
    // Очищаємо кеш попереднього аккаунту перед входом нового
    queryClient.clear()
    localStorage.setItem('token', newToken)
    setToken(newToken)
    parseAndSetRole(newToken)
    const profile = await authApi.me()
    setUser(profile)
  }

  const logout = () => {
    // Очищаємо весь React Query кеш при виході
    queryClient.clear()
    localStorage.removeItem('token')
    setToken(null)
    setUser(null)
    setRole(null)
  }

  const isAdmin = role === 'Administrator' || role === 'OrganizationAdmin'
  const isManager = role === 'Manager'
  const isUser = role === 'User'

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAdmin, isManager, isUser, role, isLoading }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be inside AuthProvider')
  return ctx
}

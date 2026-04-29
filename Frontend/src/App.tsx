import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '@/contexts/AuthContext'
import { ThemeProvider } from '@/contexts/ThemeContext'
import { AppLayout } from '@/components/layout/AppLayout'
import LoginPage from '@/pages/LoginPage'
import RegisterPage from '@/pages/RegisterPage'
import DashboardPage from '@/pages/DashboardPage'
import MedicinesPage from '@/pages/MedicinesPage'
import MedicineDetailPage from '@/pages/MedicineDetailPage'
import IoTDevicesPage from '@/pages/IoTDevicesPage'
import StorageLocationsPage from '@/pages/StorageLocationsPage'
import IncidentsPage from '@/pages/IncidentsPage'
import NotificationsPage from '@/pages/NotificationsPage'
import AuditLogPage from '@/pages/AuditLogPage'
import UsersPage from '@/pages/UsersPage'

// queryClient як singleton — очищення кешу при login/logout в AuthContext
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
})

export default function App() {
  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <AuthProvider queryClient={queryClient}>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route element={<AppLayout />}>
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/medicines" element={<MedicinesPage />} />
                <Route path="/medicines/:id" element={<MedicineDetailPage />} />
                <Route path="/iot-devices" element={<IoTDevicesPage />} />
                <Route path="/storage-locations" element={<StorageLocationsPage />} />
                <Route path="/incidents" element={<IncidentsPage />} />
                <Route path="/notifications" element={<NotificationsPage />} />
                <Route path="/audit-log" element={<AuditLogPage />} />
                <Route path="/users" element={<UsersPage />} />
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
              </Route>
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  )
}

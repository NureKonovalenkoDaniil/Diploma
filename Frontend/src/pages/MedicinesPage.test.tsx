import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import MedicinesPage from './MedicinesPage'
import { medicineApi, locationApi } from '@/api'
import type { MedicineDto, StorageLocationDto } from '@/types/api'

vi.mock('@/api', () => ({
  medicineApi: {
    getAll: vi.fn(),
    delete: vi.fn(),
  },
  locationApi: {
    getAll: vi.fn(),
  },
}))

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}))

const mockMedicines: MedicineDto[] = [
  {
    medicineID: 1,
    name: 'Amoxicillin',
    type: 'Antibiotic',
    expiryDate: '2027-12-31',
    quantity: 100,
    category: 'Prescription',
    status: 'Active',
    manufacturer: 'PharmaCorp',
    batchNumber: 'BATCH001',
  },
  {
    medicineID: 2,
    name: 'Ibuprofen',
    type: 'Painkiller',
    expiryDate: '2026-06-15',
    quantity: 5,
    category: 'OTC',
    status: 'Active',
  },
]

const mockLocations: StorageLocationDto[] = [
  {
    locationId: 1,
    name: 'Warehouse A',
    locationType: 'Warehouse',
  },
]

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>{ui}</BrowserRouter>
    </QueryClientProvider>,
  )
}

describe('MedicinesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(medicineApi.getAll).mockResolvedValue(mockMedicines)
    vi.mocked(locationApi.getAll).mockResolvedValue(mockLocations)
  })

  it('should show "Додати" button for Administrator', async () => {
    const { useAuth } = await import('@/contexts/AuthContext')
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', email: 'admin@test.com', userName: 'admin', roles: ['Administrator'], organizationId: 'org-1' },
      token: 'token',
      login: vi.fn(),
      logout: vi.fn(),
      isAdmin: true,
      isManager: false,
      isUser: false,
      role: 'Administrator',
      isLoading: false,
    })

    renderWithProviders(<MedicinesPage />)

    await waitFor(() => {
      expect(screen.getByText('Додати')).toBeInTheDocument()
    })
  })

  it('should show "Додати" button for Manager', async () => {
    const { useAuth } = await import('@/contexts/AuthContext')
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '2', email: 'manager@test.com', userName: 'manager', roles: ['Manager'], organizationId: 'org-1' },
      token: 'token',
      login: vi.fn(),
      logout: vi.fn(),
      isAdmin: false,
      isManager: true,
      isUser: false,
      role: 'Manager',
      isLoading: false,
    })

    renderWithProviders(<MedicinesPage />)

    await waitFor(() => {
      expect(screen.getByText('Додати')).toBeInTheDocument()
    })
  })

  it('should show "Додати" button for User', async () => {
    const { useAuth } = await import('@/contexts/AuthContext')
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '3', email: 'user@test.com', userName: 'user', roles: ['User'], organizationId: 'org-1' },
      token: 'token',
      login: vi.fn(),
      logout: vi.fn(),
      isAdmin: false,
      isManager: false,
      isUser: true,
      role: 'User',
      isLoading: false,
    })

    renderWithProviders(<MedicinesPage />)

    await waitFor(() => {
      expect(screen.getByText('Amoxicillin')).toBeInTheDocument()
    })

    expect(screen.getByText('Додати')).toBeInTheDocument()
  })

  it('should show edit and delete buttons for Administrator', async () => {
    const { useAuth } = await import('@/contexts/AuthContext')
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', email: 'admin@test.com', userName: 'admin', roles: ['Administrator'], organizationId: 'org-1' },
      token: 'token',
      login: vi.fn(),
      logout: vi.fn(),
      isAdmin: true,
      isManager: false,
      isUser: false,
      role: 'Administrator',
      isLoading: false,
    })

    renderWithProviders(<MedicinesPage />)

    await waitFor(() => {
      expect(screen.getByText('Amoxicillin')).toBeInTheDocument()
    })

    expect(screen.getByText('Дії')).toBeInTheDocument()
  })

  it('should show edit and delete buttons for User', async () => {
    const { useAuth } = await import('@/contexts/AuthContext')
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '3', email: 'user@test.com', userName: 'user', roles: ['User'], organizationId: 'org-1' },
      token: 'token',
      login: vi.fn(),
      logout: vi.fn(),
      isAdmin: false,
      isManager: false,
      isUser: true,
      role: 'User',
      isLoading: false,
    })

    renderWithProviders(<MedicinesPage />)

    await waitFor(() => {
      expect(screen.getByText('Amoxicillin')).toBeInTheDocument()
    })

    expect(screen.getByText('Дії')).toBeInTheDocument()
  })

  it('should display medicines list correctly', async () => {
    const { useAuth } = await import('@/contexts/AuthContext')
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', email: 'admin@test.com', userName: 'admin', roles: ['Administrator'], organizationId: 'org-1' },
      token: 'token',
      login: vi.fn(),
      logout: vi.fn(),
      isAdmin: true,
      isManager: false,
      isUser: false,
      role: 'Administrator',
      isLoading: false,
    })

    renderWithProviders(<MedicinesPage />)

    await waitFor(() => {
      expect(screen.getByText('Amoxicillin')).toBeInTheDocument()
      expect(screen.getByText('Ibuprofen')).toBeInTheDocument()
    })

    expect(screen.getByText('Antibiotic')).toBeInTheDocument()
    expect(screen.getByText('Painkiller')).toBeInTheDocument()
  })

  it('should show loading skeletons while fetching data', async () => {
    const { useAuth } = await import('@/contexts/AuthContext')
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', email: 'admin@test.com', userName: 'admin', roles: ['Administrator'], organizationId: 'org-1' },
      token: 'token',
      login: vi.fn(),
      logout: vi.fn(),
      isAdmin: true,
      isManager: false,
      isUser: false,
      role: 'Administrator',
      isLoading: false,
    })

    vi.mocked(medicineApi.getAll).mockImplementation(() => new Promise(() => {}))

    renderWithProviders(<MedicinesPage />)

    const skeletons = document.querySelectorAll('[class*="animate-pulse"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('should show empty state when no medicines found', async () => {
    const { useAuth } = await import('@/contexts/AuthContext')
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', email: 'admin@test.com', userName: 'admin', roles: ['Administrator'], organizationId: 'org-1' },
      token: 'token',
      login: vi.fn(),
      logout: vi.fn(),
      isAdmin: true,
      isManager: false,
      isUser: false,
      role: 'Administrator',
      isLoading: false,
    })

    vi.mocked(medicineApi.getAll).mockResolvedValue([])

    renderWithProviders(<MedicinesPage />)

    await waitFor(() => {
      expect(screen.getByText('Препарати не знайдені')).toBeInTheDocument()
    })
  })

  it('should filter medicines by search query', async () => {
    const { useAuth } = await import('@/contexts/AuthContext')
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', email: 'admin@test.com', userName: 'admin', roles: ['Administrator'], organizationId: 'org-1' },
      token: 'token',
      login: vi.fn(),
      logout: vi.fn(),
      isAdmin: true,
      isManager: false,
      isUser: false,
      role: 'Administrator',
      isLoading: false,
    })

    renderWithProviders(<MedicinesPage />)

    await waitFor(() => {
      expect(screen.getByText('Amoxicillin')).toBeInTheDocument()
    })

    const searchInput = screen.getByPlaceholderText('Пошук за назвою або типом...')
    fireEvent.change(searchInput, { target: { value: 'Ibuprofen' } })

    await waitFor(() => {
      expect(screen.queryByText('Amoxicillin')).not.toBeInTheDocument()
      expect(screen.getByText('Ibuprofen')).toBeInTheDocument()
    })
  })
})

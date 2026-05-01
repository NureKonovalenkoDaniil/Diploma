import { api } from './client';
import type {
  MedicineDto,
  IoTDeviceDto,
  StorageLocationDto,
  StorageIncidentDto,
  MedicineLifecycleEventDto,
  StorageConditionDto,
  NotificationDto,
  AuditLogDto,
  LoginRequest,
  LoginResponse,
  UserProfile,
  ReplenishmentRecommendation,
  CreateNotificationDto,
  CreateMedicineRequest,
} from '@/types/api';

// ──────────────────────────────────────────
// Auth
// ──────────────────────────────────────────
export const authApi = {
  login: (data: LoginRequest) =>
    api.post<{ Token?: string; token?: string }>('/api/auth/login', data).then((r) => ({
      token: r.data.token || r.data.Token || '',
    })),
  confirmEmail: (userId: string, token: string) =>
    api.get('/api/auth/confirm-email', { params: { userId, token } }),
  resendConfirmation: (email: string) => api.post('/api/auth/resend-confirmation', { email }),
  me: () =>
    api
      .get<{
        Id?: string;
        id?: string;
        UserName?: string;
        userName?: string;
        Email?: string;
        email?: string;
        Roles?: string[];
        roles?: string[];
        OrganizationId?: string;
        organizationId?: string;
      }>('/api/auth/me')
      .then((r) => ({
        id: r.data.id || r.data.Id || '',
        userName: r.data.userName || r.data.UserName || '',
        email: r.data.email || r.data.Email || '',
        roles: r.data.roles || r.data.Roles || [],
        organizationId: r.data.organizationId || r.data.OrganizationId || '',
      })),
};

// ──────────────────────────────────────────
// Medicines
// ──────────────────────────────────────────
export const medicineApi = {
  getAll: () => api.get<MedicineDto[]>('/api/medicine').then((r) => r.data),
  getById: (id: number) => api.get<MedicineDto>(`/api/medicine/${id}`).then((r) => r.data),
  getLowStock: (threshold = 10) =>
    api.get<MedicineDto[]>(`/api/medicine/low-stock?threshold=${threshold}`).then((r) => r.data),
  getExpiring: (days = 7) =>
    api.get<MedicineDto[]>(`/api/medicine/expiring?daysThreshold=${days}`).then((r) => r.data),
  getReplenishment: () =>
    api
      .get<ReplenishmentRecommendation[]>('/api/medicine/replenishment-recommendations')
      .then((r) => r.data),
  create: (data: CreateMedicineRequest) =>
    api.post<MedicineDto>('/api/medicine', data).then((r) => r.data),
  move: (
    id: number,
    data: { storageLocationId: number; description?: string; quantity?: number },
  ) => api.post<MedicineDto>(`/api/medicine/${id}/move`, data).then((r) => r.data),
  receive: (
    id: number,
    data: {
      quantity: number;
      description?: string;
      relatedLocationId?: number;
      storageLocationId?: number;
    },
  ) => api.post<MedicineDto>(`/api/medicine/${id}/receive`, data).then((r) => r.data),
  issue: (
    id: number,
    data: { quantity: number; description?: string; relatedLocationId?: number },
  ) => api.post<MedicineDto>(`/api/medicine/${id}/issue`, data).then((r) => r.data),
  dispose: (
    id: number,
    data: { quantity: number; description?: string; relatedLocationId?: number },
  ) => api.post<MedicineDto>(`/api/medicine/${id}/dispose`, data).then((r) => r.data),
  update: (id: number, patch: object[]) =>
    api.patch<MedicineDto>(`/api/medicine/${id}`, patch).then((r) => r.data),
  delete: (id: number) => api.delete(`/api/medicine/${id}`),
};

// ──────────────────────────────────────────
// IoT Devices
// ──────────────────────────────────────────
export const iotApi = {
  getAll: () => api.get<IoTDeviceDto[]>('/api/iotdevice').then((r) => r.data),
  getById: (id: string) => api.get<IoTDeviceDto>(`/api/iotdevice/${id}`).then((r) => r.data),
  getConditions: (deviceId: string) =>
    api.get<StorageConditionDto[]>(`/api/iotdevice/conditions/${deviceId}`).then((r) => r.data),
  create: (data: Omit<IoTDeviceDto, 'deviceID'>) =>
    api.post<IoTDeviceDto>('/api/iotdevice', data).then((r) => r.data),
  update: (id: string, patch: object[]) =>
    api.patch<IoTDeviceDto>(`/api/iotdevice/${id}`, patch).then((r) => r.data),
  setStatus: (deviceId: string, isActive: boolean) =>
    api.patch(`/api/iotdevice/setstatus/${deviceId}?isActive=${isActive}`),
  delete: (id: string) => api.delete(`/api/iotdevice/${id}`),
};

// ──────────────────────────────────────────
// Storage Conditions
// ──────────────────────────────────────────
export const conditionApi = {
  getAll: () => api.get<StorageConditionDto[]>('/api/storagecondition').then((r) => r.data),
};

// ──────────────────────────────────────────
// Storage Locations
// ──────────────────────────────────────────
export const locationApi = {
  getAll: () => api.get<StorageLocationDto[]>('/api/storagelocation').then((r) => r.data),
  getById: (id: number) =>
    api.get<StorageLocationDto>(`/api/storagelocation/${id}`).then((r) => r.data),
  create: (data: Omit<StorageLocationDto, 'locationId' | 'ioTDeviceLocation'>) =>
    api.post<StorageLocationDto>('/api/storagelocation', data).then((r) => r.data),
  update: (id: number, data: Omit<StorageLocationDto, 'locationId' | 'ioTDeviceLocation'>) =>
    api.put<StorageLocationDto>(`/api/storagelocation/${id}`, data).then((r) => r.data),
  delete: (id: number) => api.delete(`/api/storagelocation/${id}`),
};

// ──────────────────────────────────────────
// Incidents
// ──────────────────────────────────────────
export const incidentApi = {
  getAll: () => api.get<StorageIncidentDto[]>('/api/storageincident').then((r) => r.data),
  getActive: () => api.get<StorageIncidentDto[]>('/api/storageincident/active').then((r) => r.data),
  getById: (id: number) =>
    api.get<StorageIncidentDto>(`/api/storageincident/${id}`).then((r) => r.data),
  resolve: (id: number) =>
    api.patch<StorageIncidentDto>(`/api/storageincident/${id}/resolve`).then((r) => r.data),
};

// ──────────────────────────────────────────
// Lifecycle
// ──────────────────────────────────────────
export const lifecycleApi = {
  getAll: () => api.get<MedicineLifecycleEventDto[]>('/api/medicinelifecycle').then((r) => r.data),
  getByMedicine: (medicineId: number) =>
    api
      .get<MedicineLifecycleEventDto[]>(`/api/medicinelifecycle/medicine/${medicineId}`)
      .then((r) => r.data),
  addEvent: (
    data: Omit<
      MedicineLifecycleEventDto,
      'eventId' | 'medicineName' | 'relatedLocationName' | 'performedBy' | 'performedAt'
    >,
  ) => api.post<MedicineLifecycleEventDto>('/api/medicinelifecycle', data).then((r) => r.data),
};

// ──────────────────────────────────────────
// Notifications
// ──────────────────────────────────────────
export const notificationApi = {
  getAll: (role?: string) =>
    api
      .get<NotificationDto[]>(`/api/notification${role ? `?role=${role}` : ''}`)
      .then((r) => r.data),
  getUnread: () => api.get<NotificationDto[]>('/api/notification/unread').then((r) => r.data),
  markAsRead: (id: number) => api.patch(`/api/notification/${id}/read`),
  markAllAsRead: () => api.patch('/api/notification/read-all'),
  create: (data: CreateNotificationDto) =>
    api.post<NotificationDto>('/api/notification', data).then((r) => r.data),
};

// ──────────────────────────────────────────
// Audit Log
// ──────────────────────────────────────────
export const auditApi = {
  getLogs: (params?: { from?: string; to?: string; user?: string; action?: string }) =>
    api.get<AuditLogDto[]>('/api/auditlog', { params }).then((r) => r.data),
};

// Response DTOs matching backend Models/DTOs/ResponseDTOs.cs

export interface MedicineDto {
  medicineID: number;
  name: string;
  type: string;
  expiryDate: string;
  quantity: number;
  category: string;
  status: string;
  manufacturer?: string;
  batchNumber?: string;
  description?: string;
  minStorageTemp?: number;
  maxStorageTemp?: number;
  minStorageHumidity?: number;
  maxStorageHumidity?: number;
  storageLocationId?: number;
  storageLocationName?: string;
}

export interface CreateMedicineRequest {
  name: string;
  type: string;
  expiryDate: string;
  quantity: number;
  category: string;
  status?: string;
  manufacturer?: string;
  batchNumber?: string;
  description?: string;
  minStorageTemp?: number;
  maxStorageTemp?: number;
  minStorageHumidity?: number;
  maxStorageHumidity?: number;
  storageLocationId?: number;
}

export interface IoTDeviceDto {
  deviceID: string;
  location: string;
  type: string;
  parameters: string;
  isActive: boolean;
  minTemperature: number;
  maxTemperature: number;
  minHumidity: number;
  maxHumidity: number;
}

export interface StorageLocationDto {
  locationId: number;
  name: string;
  address?: string;
  locationType: string;
  ioTDeviceId?: string;
  ioTDeviceLocation?: string;
}

export interface StorageIncidentDto {
  incidentId: number;
  deviceId: string;
  deviceLocation: string;
  locationId?: number;
  locationName?: string;
  incidentType: string;
  detectedValue: number;
  expectedMin: number;
  expectedMax: number;
  startTime: string;
  endTime?: string;
  status: string;
  createdAt: string;
}

export interface MedicineLifecycleEventDto {
  eventId: number;
  medicineId: number;
  medicineName: string;
  eventType: string;
  description?: string;
  quantity?: number;
  performedBy: string;
  performedAt: string;
  relatedLocationId?: number;
  relatedLocationName?: string;
}

export interface StorageConditionDto {
  conditionID: number;
  temperature: number;
  humidity: number;
  timestamp: string;
  deviceID: string;
  deviceLocation?: string;
}

export interface NotificationDto {
  notificationId: number;
  type: string;
  title: string;
  message: string;
  targetRole: string;
  isRead: boolean;
  createdAt: string;
  relatedEntityType?: string;
  relatedEntityId?: number;
}

export interface AuditLogDto {
  id: number;
  action: string;
  user: string;
  timestamp: string;
  details: string;
  entityType?: string;
  entityId?: number;
  severity: string;
}

export interface ReplenishmentRecommendation {
  medicineId: number;
  medicineName: string;
  recommendedQuantity: number;
}

// Auth types
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export interface UserProfile {
  id: string;
  userName: string;
  email: string;
  roles: string[];
  organizationId?: string;
}

// Request DTOs
export interface CreateNotificationDto {
  type: string;
  title: string;
  message: string;
  targetRole?: string;
  relatedEntityType?: string;
  relatedEntityId?: number;
}

export interface CreateStorageIncidentDto {
  deviceId: string;
  locationId?: number;
  incidentType: string;
  detectedValue: number;
  expectedMin: number;
  expectedMax: number;
}

export type IncidentStatus = 'Active' | 'Resolved' | 'AutoResolved';
export type IncidentType = 'TemperatureViolation' | 'HumidityViolation';
export type NotificationType =
  | 'Expiry'
  | 'LowStock'
  | 'StorageViolation'
  | 'StorageRestored'
  | 'IncidentCreated';

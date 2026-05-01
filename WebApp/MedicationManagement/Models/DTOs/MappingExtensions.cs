using MedicationManagement.Models;
using MedicationManagement.Enums;

namespace MedicationManagement.Models.DTOs
{
    public static class MappingExtensions
    {
        public static MedicineDto ToDto(this Medicine m)
        {
            return new MedicineDto(
                m.MedicineID,
                m.Name,
                m.Type,
                m.ExpiryDate,
                m.Quantity,
                m.Category,
                m.Status.ToString(),
                m.Manufacturer,
                m.BatchNumber,
                m.Description,
                m.MinStorageTemp,
                m.MaxStorageTemp,
                m.MinStorageHumidity,
                m.MaxStorageHumidity,
                m.StorageLocationId,
                m.StorageLocation?.Name
            );
        }

        public static Medicine ToEntity(this CreateMedicineDto dto)
        {
            return new Medicine
            {
                Name = dto.Name,
                Type = dto.Type,
                ExpiryDate = dto.ExpiryDate,
                Quantity = dto.Quantity,
                Category = dto.Category,
                Status = Enum.TryParse<MedicineStatus>(dto.Status, out var st) ? st : MedicineStatus.Active,
                Manufacturer = dto.Manufacturer,
                BatchNumber = dto.BatchNumber,
                Description = dto.Description,
                MinStorageTemp = dto.MinStorageTemp,
                MaxStorageTemp = dto.MaxStorageTemp,
                MinStorageHumidity = dto.MinStorageHumidity,
                MaxStorageHumidity = dto.MaxStorageHumidity,
                StorageLocationId = dto.StorageLocationId
            };
        }

        public static IoTDeviceDto ToDto(this IoTDevice d)
        {
            return new IoTDeviceDto(
                d.DeviceID,
                d.Location,
                d.Type,
                d.Parameters,
                d.IsActive,
                d.MinTemperature,
                d.MaxTemperature,
                d.MinHumidity,
                d.MaxHumidity
            );
        }

        public static IoTDevice ToEntity(this IoTDeviceDto d)
        {
            return new IoTDevice
            {
                DeviceID = d.DeviceID,
                Location = d.Location,
                Type = d.Type,
                Parameters = d.Parameters,
                IsActive = d.IsActive,
                MinTemperature = d.MinTemperature,
                MaxTemperature = d.MaxTemperature,
                MinHumidity = d.MinHumidity,
                MaxHumidity = d.MaxHumidity
            };
        }

        public static StorageLocationDto ToDto(this StorageLocation l)
        {
            return new StorageLocationDto(
                l.LocationId,
                l.Name,
                l.Address,
                l.LocationType.ToString(),
                l.IoTDeviceId,
                l.IoTDevice?.Location
            );
        }

        public static StorageIncidentDto ToDto(this StorageIncident i)
        {
            return new StorageIncidentDto(
                i.IncidentId,
                i.DeviceId,
                i.IoTDevice?.Location ?? "Unknown",
                i.LocationId,
                i.StorageLocation?.Name,
                i.IncidentType.ToString(),
                i.DetectedValue,
                i.ExpectedMin,
                i.ExpectedMax,
                DateTime.SpecifyKind(i.StartTime, DateTimeKind.Utc),
                i.EndTime.HasValue ? DateTime.SpecifyKind(i.EndTime.Value, DateTimeKind.Utc) : null,
                i.Status.ToString(),
                DateTime.SpecifyKind(i.CreatedAt, DateTimeKind.Utc)
            );
        }

        public static MedicineLifecycleEventDto ToDto(this MedicineLifecycleEvent e)
        {
            return new MedicineLifecycleEventDto(
                e.EventId,
                e.MedicineId,
                e.Medicine?.Name ?? "Unknown",
                e.EventType.ToString(),
                e.Description,
                e.Quantity,
                e.PerformedBy,
                DateTime.SpecifyKind(e.PerformedAt, DateTimeKind.Utc),
                e.RelatedLocationId,
                e.RelatedLocation?.Name
            );
        }

        public static StorageConditionDto ToDto(this StorageCondition c)
        {
            return new StorageConditionDto(
                c.ConditionID,
                c.Temperature,
                c.Humidity,
                DateTime.SpecifyKind(c.Timestamp, DateTimeKind.Utc),
                c.DeviceID,
                c.IoTDevice?.Location
            );
        }

        public static NotificationDto ToDto(this Notification n)
        {
            return new NotificationDto(
                n.NotificationId,
                n.Type.ToString(),
                n.Title,
                n.Message,
                n.TargetRole,
                n.IsRead,
                DateTime.SpecifyKind(n.CreatedAt, DateTimeKind.Utc),
                n.RelatedEntityType,
                n.RelatedEntityId
            );
        }

        public static AuditLogDto ToDto(this AuditLog a)
        {
            return new AuditLogDto(
                a.Id,
                a.Action,
                a.User,
                DateTime.SpecifyKind(a.Timestamp, DateTimeKind.Utc),
                a.Details,
                a.EntityType,
                a.EntityId,
                a.Severity.ToString()
            );
        }

        public static StorageIncident ToEntity(this CreateStorageIncidentDto dto)
        {
            return new StorageIncident
            {
                DeviceId = dto.DeviceId,
                LocationId = dto.LocationId,
                IncidentType = Enum.TryParse<IncidentType>(dto.IncidentType, out var it) ? it : IncidentType.TemperatureViolation,
                DetectedValue = dto.DetectedValue,
                ExpectedMin = dto.ExpectedMin,
                ExpectedMax = dto.ExpectedMax,
                StartTime = dto.StartTime,
                EndTime = dto.EndTime,
                Status = Enum.TryParse<IncidentStatus>(dto.Status, out var status) ? status : IncidentStatus.Active
            };
        }

        public static MedicineLifecycleEvent ToEntity(this CreateMedicineLifecycleEventDto dto)
        {
            return new MedicineLifecycleEvent
            {
                MedicineId = dto.MedicineId,
                EventType = Enum.TryParse<LifecycleEventType>(dto.EventType, out var et) ? et : LifecycleEventType.Received,
                Description = dto.Description,
                Quantity = dto.Quantity,
                RelatedLocationId = dto.RelatedLocationId,
                PerformedAt = DateTime.UtcNow
            };
        }
    }
}

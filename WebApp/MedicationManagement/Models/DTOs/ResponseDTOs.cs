using System;
using MedicationManagement.Enums;

namespace MedicationManagement.Models.DTOs
{
    public record MedicineDto(
        int MedicineID,
        string Name,
        string Type,
        DateTime ExpiryDate,
        int Quantity,
        string Category,
        string? Manufacturer,
        string? BatchNumber,
        string? Description,
        float? MinStorageTemp,
        float? MaxStorageTemp,
        int? StorageLocationId,
        string? StorageLocationName
    );

    public record IoTDeviceDto(
        int DeviceID,
        string Location,
        string Type,
        string Parameters,
        bool IsActive,
        float MinTemperature,
        float MaxTemperature,
        float MinHumidity,
        float MaxHumidity
    );

    public record StorageLocationDto(
        int LocationId,
        string Name,
        string? Address,
        string LocationType,
        int? IoTDeviceId,
        string? IoTDeviceLocation
    );

    public record StorageIncidentDto(
        int IncidentId,
        int DeviceId,
        string DeviceLocation,
        int? LocationId,
        string? LocationName,
        string IncidentType,
        float DetectedValue,
        float ExpectedMin,
        float ExpectedMax,
        DateTime StartTime,
        DateTime? EndTime,
        string Status,
        DateTime CreatedAt
    );

    public record MedicineLifecycleEventDto(
        int EventId,
        int MedicineId,
        string MedicineName,
        string EventType,
        string? Description,
        int? Quantity,
        string PerformedBy,
        DateTime PerformedAt,
        int? RelatedLocationId,
        string? RelatedLocationName
    );

    public record StorageConditionDto(
        int ConditionID,
        float Temperature,
        float Humidity,
        DateTime Timestamp,
        int DeviceID,
        string? DeviceLocation
    );

    public record NotificationDto(
        int NotificationId,
        string Type,
        string Title,
        string Message,
        string TargetRole,
        bool IsRead,
        DateTime CreatedAt,
        string? RelatedEntityType,
        int? RelatedEntityId
    );

    public record AuditLogDto(
        int Id,
        string Action,
        string User,
        DateTime Timestamp,
        string Details,
        string? EntityType,
        int? EntityId,
        string Severity
    );
}

using MedicationManagement.DBContext;
using MedicationManagement.Enums;
using MedicationManagement.Models;
using MedicationManagement.Services;
using Microsoft.EntityFrameworkCore;

namespace MedicationManagement.BackgroundServices
{
    /// <summary>
    /// Фоновий сервіс моніторингу умов зберігання.
    /// Перевіряє останні показники всіх активних IoT-пристроїв.
    /// При порушенні — створює StorageIncident і Notification (з debounce).
    /// При відновленні норми — автоматично закриває активний інцидент.
    /// </summary>
    public class StorageConditionMonitoringService : BackgroundService
    {
        private readonly IServiceProvider _serviceProvider;
        private readonly ILogger<StorageConditionMonitoringService> _logger;
        private readonly int _intervalSeconds;

        public StorageConditionMonitoringService(
            IServiceProvider serviceProvider,
            ILogger<StorageConditionMonitoringService> logger,
            IConfiguration configuration)
        {
            _serviceProvider = serviceProvider;
            _logger = logger;
            _intervalSeconds = configuration.GetValue<int>("Monitoring:IntervalSeconds", 60);
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("StorageConditionMonitoringService started. Interval: {Interval}s", _intervalSeconds);

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    await CheckAllDevicesAsync();
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Unexpected error in StorageConditionMonitoringService");
                }

                await Task.Delay(TimeSpan.FromSeconds(_intervalSeconds), stoppingToken);
            }
        }

        private async Task CheckAllDevicesAsync()
        {
            // TD-07 NOTE: Debounce реалізований на рівні SQL-запиту:
            // новий інцидент створюється лише якщо немає активного для device+incidentType.
            // ОБМЕЖЕННЯ: Оскільки перевірка і вставка не є атомарними, теоретично
            // за умов двох паралельних тіків можливе подвійне створення інциденту.
            // Для продакшн-середовища рекомендується додати унікальний частковий індекс:
            //   CREATE UNIQUE INDEX IX_StorageIncidents_Active
            //       ON StorageIncidents (DeviceId, IncidentType)
            //       WHERE Status = 'Active'
            // Для дипломної роботи це прийнятно — BackgroundService є singleton,
            // тіки виконуються послідовно і паралельних запусків не відбувається.

            using var scope = _serviceProvider.CreateScope();
            var db = scope.ServiceProvider.GetRequiredService<MedicineStorageContext>();
            var auditService = scope.ServiceProvider.GetRequiredService<IServiceAuditLog>();
            var notificationService = scope.ServiceProvider.GetRequiredService<IServiceNotification>();

            var devices = await db.IoTDevices
                .Where(d => d.IsActive)
                .ToListAsync();

            foreach (var device in devices)
            {
                var condition = await db.StorageConditions
                    .Where(sc => sc.DeviceID == device.DeviceID)
                    .OrderByDescending(sc => sc.Timestamp)
                    .FirstOrDefaultAsync();

                if (condition is null) continue;

                await HandleTemperatureAsync(db, auditService, notificationService, device, condition);
                await HandleHumidityAsync(db, auditService, notificationService, device, condition);
            }
        }

        // ───────────────────── Temperature ─────────────────────

        private async Task HandleTemperatureAsync(
            MedicineStorageContext db,
            IServiceAuditLog auditService,
            IServiceNotification notificationService,
            IoTDevice device, StorageCondition condition)
        {
            bool isViolation = condition.Temperature < device.MinTemperature
                            || condition.Temperature > device.MaxTemperature;

            var activeIncident = await db.StorageIncidents
                .FirstOrDefaultAsync(i => i.DeviceId == device.DeviceID
                                       && i.IncidentType == IncidentType.TemperatureViolation
                                       && i.Status == IncidentStatus.Active);

            if (isViolation && activeIncident is null)
            {
                // Нове порушення — debounce пройдено (немає активного інциденту)
                var incident = new StorageIncident
                {
                    DeviceId = device.DeviceID,
                    IncidentType = IncidentType.TemperatureViolation,
                    DetectedValue = condition.Temperature,
                    ExpectedMin = device.MinTemperature,
                    ExpectedMax = device.MaxTemperature,
                    Status = IncidentStatus.Active,
                    StartTime = DateTime.UtcNow,
                    CreatedAt = DateTime.UtcNow
                };
                db.StorageIncidents.Add(incident);
                await db.SaveChangesAsync();

                var msg = $"Температурне порушення на пристрої {device.DeviceID}: {condition.Temperature}°C " +
                          $"(норма: {device.MinTemperature}–{device.MaxTemperature}°C)";

                await notificationService.Create(
                    NotificationType.StorageViolation,
                    "⚠️ Порушення температури",
                    msg,
                    targetRole: "Administrator",
                    relatedEntityType: "StorageIncident",
                    relatedEntityId: incident.IncidentId);

                await auditService.LogAction(
                    "StorageIncident_Created", "System", msg, isSensor: true,
                    entityType: "StorageIncident", entityId: incident.IncidentId,
                    severity: AuditSeverity.Warning);

                _logger.LogWarning("Temperature incident #{Id} created for device {DeviceId}", incident.IncidentId, device.DeviceID);
            }
            else if (!isViolation && activeIncident is not null)
            {
                // Норма відновлена — auto-resolve
                activeIncident.Status = IncidentStatus.Resolved;
                activeIncident.EndTime = DateTime.UtcNow;
                await db.SaveChangesAsync();

                var msg = $"Температура нормалізована на пристрої {device.DeviceID}: {condition.Temperature}°C. " +
                          $"Інцидент #{activeIncident.IncidentId} закрито.";

                await notificationService.Create(
                    NotificationType.StorageViolation,
                    "✅ Температура нормалізована",
                    msg,
                    targetRole: "Administrator",
                    relatedEntityType: "StorageIncident",
                    relatedEntityId: activeIncident.IncidentId);

                await auditService.LogAction(
                    "StorageIncident_Resolved", "System", msg, isSensor: true,
                    entityType: "StorageIncident", entityId: activeIncident.IncidentId,
                    severity: AuditSeverity.Info);

                _logger.LogInformation("Temperature incident #{Id} resolved for device {DeviceId}", activeIncident.IncidentId, device.DeviceID);
            }
        }

        // ───────────────────── Humidity ─────────────────────

        private async Task HandleHumidityAsync(
            MedicineStorageContext db,
            IServiceAuditLog auditService,
            IServiceNotification notificationService,
            IoTDevice device, StorageCondition condition)
        {
            bool isViolation = condition.Humidity < device.MinHumidity
                            || condition.Humidity > device.MaxHumidity;

            var activeIncident = await db.StorageIncidents
                .FirstOrDefaultAsync(i => i.DeviceId == device.DeviceID
                                       && i.IncidentType == IncidentType.HumidityViolation
                                       && i.Status == IncidentStatus.Active);

            if (isViolation && activeIncident is null)
            {
                var incident = new StorageIncident
                {
                    DeviceId = device.DeviceID,
                    IncidentType = IncidentType.HumidityViolation,
                    DetectedValue = condition.Humidity,
                    ExpectedMin = device.MinHumidity,
                    ExpectedMax = device.MaxHumidity,
                    Status = IncidentStatus.Active,
                    StartTime = DateTime.UtcNow,
                    CreatedAt = DateTime.UtcNow
                };
                db.StorageIncidents.Add(incident);
                await db.SaveChangesAsync();

                var msg = $"Порушення вологості на пристрої {device.DeviceID}: {condition.Humidity}% " +
                          $"(норма: {device.MinHumidity}–{device.MaxHumidity}%)";

                await notificationService.Create(
                    NotificationType.StorageViolation,
                    "⚠️ Порушення вологості",
                    msg,
                    targetRole: "Administrator",
                    relatedEntityType: "StorageIncident",
                    relatedEntityId: incident.IncidentId);

                await auditService.LogAction(
                    "StorageIncident_Created", "System", msg, isSensor: true,
                    entityType: "StorageIncident", entityId: incident.IncidentId,
                    severity: AuditSeverity.Warning);

                _logger.LogWarning("Humidity incident #{Id} created for device {DeviceId}", incident.IncidentId, device.DeviceID);
            }
            else if (!isViolation && activeIncident is not null)
            {
                activeIncident.Status = IncidentStatus.Resolved;
                activeIncident.EndTime = DateTime.UtcNow;
                await db.SaveChangesAsync();

                var msg = $"Вологість нормалізована на пристрої {device.DeviceID}: {condition.Humidity}%. " +
                          $"Інцидент #{activeIncident.IncidentId} закрито.";

                await notificationService.Create(
                    NotificationType.StorageViolation,
                    "✅ Вологість нормалізована",
                    msg,
                    targetRole: "Administrator",
                    relatedEntityType: "StorageIncident",
                    relatedEntityId: activeIncident.IncidentId);

                await auditService.LogAction(
                    "StorageIncident_Resolved", "System", msg, isSensor: true,
                    entityType: "StorageIncident", entityId: activeIncident.IncidentId,
                    severity: AuditSeverity.Info);

                _logger.LogInformation("Humidity incident #{Id} resolved for device {DeviceId}", activeIncident.IncidentId, device.DeviceID);
            }
        }
    }
}

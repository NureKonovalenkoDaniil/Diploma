using MedicationManagement.DBContext;
using MedicationManagement.Enums;
using MedicationManagement.Services;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Models;

namespace MedicationManagement.BackgroundServices
{
    /// <summary>
    /// Фоновий сервіс сповіщень про закінчення терміну придатності.
    /// Запускається раз на добу. Зберігає Notification у БД (з дедуплікацією — не більше 1 сповіщення на добу на препарат).
    /// </summary>
    public class ExpiryNotificationService : BackgroundService
    {
        private readonly IServiceProvider _serviceProvider;
        private readonly ILogger<ExpiryNotificationService> _logger;
        private readonly int _expiryWarningDays;

        public ExpiryNotificationService(
            IServiceProvider serviceProvider,
            ILogger<ExpiryNotificationService> logger,
            IConfiguration configuration)
        {
            _serviceProvider = serviceProvider;
            _logger = logger;
            _expiryWarningDays = configuration.GetValue<int>("Monitoring:ExpiryWarningDays", 7);
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("ExpiryNotificationService started. Warning window: {Days} days", _expiryWarningDays);

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    await CheckExpiringMedicinesAsync();
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Unexpected error in ExpiryNotificationService");
                }

                await Task.Delay(TimeSpan.FromDays(1), stoppingToken);
            }
        }

        private async Task CheckExpiringMedicinesAsync()
        {
            using var scope = _serviceProvider.CreateScope();
            var db = scope.ServiceProvider.GetRequiredService<MedicineStorageContext>();
            var medicineService = scope.ServiceProvider.GetRequiredService<IServiceMedicine>();
            var auditService = scope.ServiceProvider.GetRequiredService<IServiceAuditLog>();
            var notificationService = scope.ServiceProvider.GetRequiredService<IServiceNotification>();

            // 1) Expired (факт) — разово фіксуємо lifecycle-подію + (за потреби) статус
            await CheckExpiredMedicinesAsync(db, auditService);

            var threshold = DateTime.UtcNow.AddDays(_expiryWarningDays);
            var expiringMedicines = await medicineService.GetExpiringMedicines(threshold);

            var todayUtc = DateTime.UtcNow.Date;

            foreach (var medicine in expiringMedicines)
            {
                // Дедуплікація: чи є вже сповіщення для цього препарату сьогодні?
                var alreadyNotified = await db.Notifications.AnyAsync(n =>
                    n.Type == NotificationType.Expiry &&
                    n.RelatedEntityType == "Medicine" &&
                    n.RelatedEntityId == medicine.MedicineID &&
                    n.OrganizationId == medicine.OrganizationId &&
                    n.CreatedAt.Date == todayUtc);

                if (alreadyNotified)
                {
                    _logger.LogDebug("Expiry notification for Medicine {Id} already sent today. Skipping.", medicine.MedicineID);
                    continue;
                }

                var daysLeft = (medicine.ExpiryDate - DateTime.UtcNow).Days;
                var msg = $"Препарат «{medicine.Name}» (ID: {medicine.MedicineID}) " +
                          $"закінчується {medicine.ExpiryDate:yyyy-MM-dd} (через {daysLeft} д.).";

                await notificationService.Create(
                    NotificationType.Expiry,
                    "⏳ Закінчується термін придатності",
                    msg,
                    targetRole: "All",
                    relatedEntityType: "Medicine",
                    relatedEntityId: medicine.MedicineID,
                    organizationId: medicine.OrganizationId);

                await auditService.LogAction(
                    "ExpiryNotification_Sent", "System", msg, isSensor: false,
                    entityType: "Medicine", entityId: medicine.MedicineID,
                    severity: AuditSeverity.Warning);

                _logger.LogWarning("Expiry notification sent for Medicine {Id} ({Name})", medicine.MedicineID, medicine.Name);
            }
        }

        private static async Task CheckExpiredMedicinesAsync(
            MedicineStorageContext db,
            IServiceAuditLog auditService)
        {
            var nowUtc = DateTime.UtcNow;

            // Беремо лише ті, що вже прострочені, але ще не мають події Expired
            var expired = await db.Medicines
                .AsQueryable()
                .Where(m => m.ExpiryDate <= nowUtc)
                .ToListAsync();

            foreach (var medicine in expired)
            {
                var alreadyHasExpiredEvent = await db.MedicineLifecycleEvents.AnyAsync(e =>
                    e.MedicineId == medicine.MedicineID &&
                    e.EventType == LifecycleEventType.Expired);

                if (alreadyHasExpiredEvent) continue;

                // Оновлюємо статус, якщо він ще Active.
                if (medicine.Status == MedicineStatus.Active)
                {
                    medicine.Status = MedicineStatus.Expired;
                    await db.SaveChangesAsync();
                }

                var evt = new MedicineLifecycleEvent
                {
                    MedicineId = medicine.MedicineID,
                    OrganizationId = medicine.OrganizationId,
                    EventType = LifecycleEventType.Expired,
                    Quantity = null,
                    PerformedBy = "System",
                    PerformedAt = DateTime.UtcNow,
                    RelatedLocationId = medicine.StorageLocationId,
                    Description = $"Авто-прострочення: термін придатності минув {medicine.ExpiryDate:yyyy-MM-dd}"
                };

                db.MedicineLifecycleEvents.Add(evt);
                await db.SaveChangesAsync();

                await auditService.LogAction(
                    "Medicine_AutoExpired",
                    "System",
                    $"Medicine ID {medicine.MedicineID} auto-marked as Expired.",
                    isSensor: false,
                    entityType: "Medicine",
                    entityId: medicine.MedicineID,
                    severity: AuditSeverity.Warning);
            }
        }
    }
}

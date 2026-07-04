using MedicationManagement.DBContext;
using MedicationManagement.Enums;
using MedicationManagement.Services;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Models;

namespace MedicationManagement.BackgroundServices
{
    /// <summary>
    /// Фоновий сервіс сповіщень про закінчення терміну придатності та низький запас медикаментів.
    /// Запускається кожні 15 секунд для оперативної перевірки. 
    /// Зберігає Notification у БД з дедуплікацією (не більше 1 сповіщення на добу на препарат).
    /// </summary>
    public class ExpiryNotificationService : BackgroundService
    {
        private readonly IServiceProvider _serviceProvider;
        private readonly ILogger<ExpiryNotificationService> _logger;
        private readonly IConfiguration _configuration;
        private readonly int _expiryWarningDays;

        public ExpiryNotificationService(
            IServiceProvider serviceProvider,
            ILogger<ExpiryNotificationService> logger,
            IConfiguration configuration)
        {
            _serviceProvider = serviceProvider;
            _logger = logger;
            _configuration = configuration;
            _expiryWarningDays = configuration.GetValue<int>("Monitoring:ExpiryWarningDays", 7);
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("ExpiryNotificationService started. Warning window: {Days} days", _expiryWarningDays);

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    await CheckAllNotificationsAsync();
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Unexpected error in ExpiryNotificationService");
                }

                await Task.Delay(TimeSpan.FromSeconds(15), stoppingToken);
            }
        }

        private async Task CheckAllNotificationsAsync()
        {
            using var scope = _serviceProvider.CreateScope();
            var db = scope.ServiceProvider.GetRequiredService<MedicineStorageContext>();
            var medicineService = scope.ServiceProvider.GetRequiredService<IServiceMedicine>();
            var auditService = scope.ServiceProvider.GetRequiredService<IServiceAuditLog>();
            var notificationService = scope.ServiceProvider.GetRequiredService<IServiceNotification>();

            // 1) Перевірка вже прострочених препаратів (Expired) та створення сповіщень
            await CheckExpiredMedicinesAsync(db, auditService, notificationService);

            // 2) Перевірка низького запасу препаратів (LowStock)
            await CheckLowStockMedicinesAsync(db, medicineService, auditService, notificationService);

            // 3) Перевірка терміну придатності, що закінчується (Expiring)
            await CheckExpiringMedicinesAsync(db, medicineService, auditService, notificationService);
        }

        private async Task CheckExpiredMedicinesAsync(
            MedicineStorageContext db,
            IServiceAuditLog auditService,
            IServiceNotification notificationService)
        {
            var nowUtc = DateTime.UtcNow;
            var todayUtc = DateTime.UtcNow.Date;

            // Беремо лише ті, що вже прострочені
            var expired = await db.Medicines
                .Where(m => m.ExpiryDate <= nowUtc)
                .ToListAsync();

            foreach (var medicine in expired)
            {
                // Оновлюємо статус, якщо він ще Active та створюємо подію життєвого циклу
                var alreadyHasExpiredEvent = await db.MedicineLifecycleEvents.AnyAsync(e =>
                    e.MedicineId == medicine.MedicineID &&
                    e.EventType == LifecycleEventType.Expired);

                if (!alreadyHasExpiredEvent)
                {
                    if (medicine.Status == MedicineStatus.Active)
                    {
                        medicine.Status = MedicineStatus.Expired;
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

                // Створюємо сповіщення про те, що термін придатності вже минув (дедуплікація: 1 раз на добу)
                var alreadyNotified = await db.Notifications.AnyAsync(n =>
                    n.Type == NotificationType.Expiry &&
                    n.RelatedEntityType == "Medicine" &&
                    n.RelatedEntityId == medicine.MedicineID &&
                    n.OrganizationId == medicine.OrganizationId &&
                    n.Title.Contains("минув") &&
                    n.CreatedAt.Date == todayUtc);

                if (!alreadyNotified)
                {
                    var msg = $"Термін придатності препарату «{medicine.Name}» (ID: {medicine.MedicineID}) МИНУВ {medicine.ExpiryDate:yyyy-MM-dd}!";
                    
                    await notificationService.Create(
                        NotificationType.Expiry,
                        "🚨 Термін придатності минув",
                        msg,
                        targetRole: "All",
                        relatedEntityType: "Medicine",
                        relatedEntityId: medicine.MedicineID,
                        organizationId: medicine.OrganizationId);

                    _logger.LogWarning("Expired notification sent for Medicine {Id} ({Name})", medicine.MedicineID, medicine.Name);
                }
            }
        }

        private async Task CheckLowStockMedicinesAsync(
            MedicineStorageContext db,
            IServiceMedicine medicineService,
            IServiceAuditLog auditService,
            IServiceNotification notificationService)
        {
            var threshold = _configuration.GetValue<int>("Business:LowStockThreshold", 10);
            var lowStockMedicines = await medicineService.GetLowStockMedicines(threshold);
            var todayUtc = DateTime.UtcNow.Date;

            foreach (var medicine in lowStockMedicines)
            {
                // Не сповіщаємо для прострочених або утилізованих препаратів
                if (medicine.Status == MedicineStatus.Expired || medicine.Status == MedicineStatus.Disposed)
                {
                    continue;
                }

                // Дедуплікація: чи є вже сповіщення для цього препарату сьогодні?
                var alreadyNotified = await db.Notifications.AnyAsync(n =>
                    n.Type == NotificationType.LowStock &&
                    n.RelatedEntityType == "Medicine" &&
                    n.RelatedEntityId == medicine.MedicineID &&
                    n.OrganizationId == medicine.OrganizationId &&
                    n.CreatedAt.Date == todayUtc);

                if (alreadyNotified)
                {
                    continue;
                }

                var msg = $"Кількість препарату «{medicine.Name}» (ID: {medicine.MedicineID}) критично мала: {medicine.Quantity} шт. (поріг: {threshold} шт.).";

                await notificationService.Create(
                    NotificationType.LowStock,
                    "⚠️ Низький запас препарату",
                    msg,
                    targetRole: "All",
                    relatedEntityType: "Medicine",
                    relatedEntityId: medicine.MedicineID,
                    organizationId: medicine.OrganizationId);

                await auditService.LogAction(
                    "LowStockNotification_Sent", "System", msg, isSensor: false,
                    entityType: "Medicine", entityId: medicine.MedicineID,
                    severity: AuditSeverity.Warning);

                _logger.LogWarning("Low stock notification sent for Medicine {Id} ({Name})", medicine.MedicineID, medicine.Name);
            }
        }

        private async Task CheckExpiringMedicinesAsync(
            MedicineStorageContext db,
            IServiceMedicine medicineService,
            IServiceAuditLog auditService,
            IServiceNotification notificationService)
        {
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
                    !n.Title.Contains("минув") &&
                    n.CreatedAt.Date == todayUtc);

                if (alreadyNotified)
                {
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

                _logger.LogWarning("Expiry warning notification sent for Medicine {Id} ({Name})", medicine.MedicineID, medicine.Name);
            }
        }
    }
}

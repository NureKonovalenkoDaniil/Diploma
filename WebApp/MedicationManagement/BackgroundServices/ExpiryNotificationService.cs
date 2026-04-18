using MedicationManagement.DBContext;
using MedicationManagement.Enums;
using MedicationManagement.Services;
using Microsoft.EntityFrameworkCore;

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

            var threshold = DateTime.Now.AddDays(_expiryWarningDays);
            var expiringMedicines = await medicineService.GetExpiringMedicines(threshold);

            var todayUtc = DateTime.UtcNow.Date;

            foreach (var medicine in expiringMedicines)
            {
                // Дедуплікація: чи є вже сповіщення для цього препарату сьогодні?
                var alreadyNotified = await db.Notifications.AnyAsync(n =>
                    n.Type == NotificationType.Expiry &&
                    n.RelatedEntityType == "Medicine" &&
                    n.RelatedEntityId == medicine.MedicineID &&
                    n.CreatedAt.Date == todayUtc);

                if (alreadyNotified)
                {
                    _logger.LogDebug("Expiry notification for Medicine {Id} already sent today. Skipping.", medicine.MedicineID);
                    continue;
                }

                var daysLeft = (medicine.ExpiryDate - DateTime.Now).Days;
                var msg = $"Препарат «{medicine.Name}» (ID: {medicine.MedicineID}) " +
                          $"закінчується {medicine.ExpiryDate:yyyy-MM-dd} (через {daysLeft} д.).";

                await notificationService.Create(
                    NotificationType.Expiry,
                    "⏳ Закінчується термін придатності",
                    msg,
                    targetRole: "All",
                    relatedEntityType: "Medicine",
                    relatedEntityId: medicine.MedicineID);

                await auditService.LogAction(
                    "ExpiryNotification_Sent", "System", msg, isSensor: false,
                    entityType: "Medicine", entityId: medicine.MedicineID,
                    severity: AuditSeverity.Warning);

                _logger.LogWarning("Expiry notification sent for Medicine {Id} ({Name})", medicine.MedicineID, medicine.Name);
            }
        }
    }
}

using MedicationManagement.Services;

namespace MedicationManagement.BackgroundServices
{
    public class ExpiryNotificationService : BackgroundService
    {
        private readonly IServiceProvider _serviceProvider;
        private readonly ILogger<ExpiryNotificationService> _logger;

        public ExpiryNotificationService(IServiceProvider serviceProvider, ILogger<ExpiryNotificationService> logger)
        {
            _serviceProvider = serviceProvider;
            _logger = logger;
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    using var scope = _serviceProvider.CreateScope();
                    var medicineService = scope.ServiceProvider.GetRequiredService<IServiceMedicine>();
                    var auditService = scope.ServiceProvider.GetRequiredService<IServiceAuditLog>();

                    var expiringMedicines = await medicineService.GetExpiringMedicines(DateTime.Now.AddDays(7));

                    foreach (var medicine in expiringMedicines)
                    {
                        var message = $"Medicine {medicine.Name} is expiring on {medicine.ExpiryDate:yyyy-MM-dd}.";
                        _logger.LogWarning("Notify: {Message}", message);
                        await auditService.LogAction("Expiry Notification", "System", message, false);
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Error occurred in ExpiryNotificationService");
                }

                await Task.Delay(TimeSpan.FromDays(1), stoppingToken);
            }
        }
    }
}

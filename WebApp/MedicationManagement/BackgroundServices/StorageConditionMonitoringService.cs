using MedicationManagement.Services;

namespace MedicationManagement.BackgroundServices
{
    public class StorageConditionMonitoringService : BackgroundService
    {
        private readonly IServiceProvider _serviceProvider;
        private readonly ILogger<StorageConditionMonitoringService> _logger;

        public StorageConditionMonitoringService(IServiceProvider serviceProvider, ILogger<StorageConditionMonitoringService> logger)
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
                    var storageConditionService = scope.ServiceProvider.GetRequiredService<IServiceStorageCondition>();
                    var auditService = scope.ServiceProvider.GetRequiredService<IServiceAuditLog>();

                    var violations = await storageConditionService.CheckStorageConditionsForAllDevices();

                    foreach (var violation in violations)
                    {
                        _logger.LogWarning("Violation Detected: {Violation}", violation);
                        await auditService.LogAction("Storage Condition Violation", "System", violation, true);
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Error occurred in StorageConditionMonitoringService");
                }

                await Task.Delay(TimeSpan.FromSeconds(5), stoppingToken);
            }
        }
    }
}

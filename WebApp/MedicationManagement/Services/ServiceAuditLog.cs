using MedicationManagement.DBContext;
using MedicationManagement.Enums;
using MedicationManagement.Models;

namespace MedicationManagement.Services
{
    // Interface for the audit log service
    public interface IServiceAuditLog
    {
        Task LogAction(string action, string user, string details, bool isSensor,
            string? entityType = null, int? entityId = null,
            AuditSeverity severity = AuditSeverity.Info);
    }
    // Implementation of the audit log service
    public class ServiceAuditLog : IServiceAuditLog
    {
        private readonly MedicineStorageContext _context;

        // Constructor to inject the database context
        public ServiceAuditLog(MedicineStorageContext context)
        {
            _context = context;
        }

        // Method to log an action to the audit log
        public async Task LogAction(string action, string user, string details, bool isSensor,
            string? entityType = null, int? entityId = null,
            AuditSeverity severity = AuditSeverity.Info)
        {
            var auditLog = new AuditLog
            {
                Action = action,
                User = isSensor ? $"[Sensor] {user}" : user,
                Timestamp = DateTime.UtcNow,
                Details = details,
                EntityType = entityType,
                EntityId = entityId,
                Severity = severity
            };

            _context.AuditLogs.Add(auditLog);
            await _context.SaveChangesAsync();
        }

    }
}

using MedicationManagement.DBContext;
using MedicationManagement.Enums;
using MedicationManagement.Models;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Extensions;

namespace MedicationManagement.Services
{
    /// <summary>
    /// Інтерфейс сервісу журналу аудиту.
    /// Містить операції запису і читання аудит-логів.
    /// </summary>
    public interface IServiceAuditLog
    {
        Task LogAction(string action, string user, string details, bool isSensor,
            string? entityType = null, int? entityId = null,
            AuditSeverity severity = AuditSeverity.Info);

        // TD-06: метод читання перенесено сюди з AuditLogController
        Task<IEnumerable<AuditLog>> GetLogs(
            DateTime? from = null,
            DateTime? to = null,
            string? user = null,
            string? action = null);
    }

    /// <summary>
    /// Реалізація сервісу журналу аудиту.
    /// </summary>
    public class ServiceAuditLog : IServiceAuditLog
    {
        private readonly MedicineStorageContext _context;
        private readonly IHttpContextAccessor _httpContextAccessor;

        public ServiceAuditLog(MedicineStorageContext context, IHttpContextAccessor httpContextAccessor)
        {
            _context = context;
            _httpContextAccessor = httpContextAccessor;
        }

        private string? CurrentOrgId => _httpContextAccessor.HttpContext?.User.GetOrganizationId();
        private bool IsAdmin => _httpContextAccessor.HttpContext?.User.IsInRole("Administrator") ?? true;

        /// <summary>Записати подію до журналу аудиту.</summary>
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

            var orgId = CurrentOrgId;
            if (!string.IsNullOrEmpty(orgId))
            {
                auditLog.OrganizationId = orgId;
            }

            _context.AuditLogs.Add(auditLog);
            await _context.SaveChangesAsync();
        }

        /// <summary>
        /// Отримати журнал аудиту з опціональними фільтрами.
        /// Результати відсортовані за часом (новіші — перші).
        /// </summary>
        public async Task<IEnumerable<AuditLog>> GetLogs(
            DateTime? from = null,
            DateTime? to = null,
            string? user = null,
            string? action = null)
        {
            var query = _context.AuditLogs.AsNoTracking().AsQueryable();

            if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                query = query.Where(log => log.OrganizationId == CurrentOrgId);

            if (from.HasValue)
            {
                // Конвертуємо локальний час запиту в UTC для порівняння з базою
                var fromUtc = from.Value.ToUniversalTime();
                query = query.Where(log => log.Timestamp >= fromUtc);
            }

            if (to.HasValue)
            {
                var toUtc = to.Value.ToUniversalTime();
                query = query.Where(log => log.Timestamp <= toUtc);
            }

            if (!string.IsNullOrWhiteSpace(user))
                query = query.Where(log => log.User.Contains(user));

            if (!string.IsNullOrWhiteSpace(action))
            {
                if (action == "medicine_actions")
                {
                    query = query.Where(log => 
                        log.Action.Contains("Medicine") || 
                        log.Action.Contains("Expired") || 
                        log.Action.Contains("Lifecycle") || 
                        log.Action.Contains("Receive") || 
                        log.Action.Contains("Issue") || 
                        log.Action.Contains("Move") || 
                        log.Action.Contains("Dispose") || 
                        log.EntityType == "Medicine" || 
                        log.EntityType == "MedicineLifecycleEvent");
                }
                else if (action == "location_actions")
                {
                    query = query.Where(log => 
                        log.Action.Contains("Location") || 
                        log.EntityType == "StorageLocation");
                }
                else if (action == "incident_actions")
                {
                    query = query.Where(log => 
                        log.Action.Contains("Incident") || 
                        log.EntityType == "StorageIncident");
                }
                else if (action == "device_actions")
                {
                    query = query.Where(log => 
                        log.Action.Contains("Device") || 
                        log.Action.Contains("Sensor") || 
                        log.EntityType == "IoTDevice");
                }
                else if (action == "user_actions")
                {
                    query = query.Where(log => 
                        log.Action.Contains("User") || 
                        log.Action.Contains("Manager") || 
                        log.Action.Contains("Login") || 
                        log.Action.Contains("Register") || 
                        log.Action.Contains("Email") || 
                        log.Action.Contains("Password") || 
                        log.Action.Contains("Role") ||
                        log.EntityType == "ApplicationUser");
                }
                else
                {
                    query = query.Where(log => log.Action.Contains(action));
                }
            }

            return await query.OrderByDescending(log => log.Timestamp).ToListAsync();
        }
    }
}

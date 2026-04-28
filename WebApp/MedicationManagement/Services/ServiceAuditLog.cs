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
                query = query.Where(log => log.Timestamp >= from.Value);

            if (to.HasValue)
                query = query.Where(log => log.Timestamp <= to.Value);

            if (!string.IsNullOrWhiteSpace(user))
                query = query.Where(log => log.User.Contains(user));

            if (!string.IsNullOrWhiteSpace(action))
                query = query.Where(log => log.Action.Contains(action));

            return await query.OrderByDescending(log => log.Timestamp).ToListAsync();
        }
    }
}

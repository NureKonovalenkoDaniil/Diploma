using MedicationManagement.DBContext;
using MedicationManagement.Enums;
using MedicationManagement.Models;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Extensions;

namespace MedicationManagement.Services
{
    public interface IServiceNotification
    {
        Task<IEnumerable<Notification>> GetAll(string? targetRole = null);
        Task<IEnumerable<Notification>> GetUnread(string? targetRole = null);
        Task<Notification> Create(Notification notification);
        Task<Notification> Create(NotificationType type, string title, string message,
            string targetRole = "All", string? relatedEntityType = null, int? relatedEntityId = null,
            string? organizationId = null);
        Task<bool> MarkAsRead(int notificationId);
        Task<int> MarkAllAsRead(string? targetRole = null);
    }

    public class ServiceNotification : IServiceNotification
    {
        private readonly MedicineStorageContext _context;
        private readonly ILogger<ServiceNotification> _logger;
        private readonly IHttpContextAccessor _httpContextAccessor;

        public ServiceNotification(MedicineStorageContext context, ILogger<ServiceNotification> logger, IHttpContextAccessor httpContextAccessor)
        {
            _context = context;
            _logger = logger;
            _httpContextAccessor = httpContextAccessor;
        }

        private string? CurrentOrgId => _httpContextAccessor.HttpContext?.User.GetOrganizationId();
        private bool IsAdmin => _httpContextAccessor.HttpContext?.User.IsInRole("Administrator") ?? true;

        public async Task<IEnumerable<Notification>> GetAll(string? targetRole = null)
        {
            try
            {
                var query = _context.Notifications.AsNoTracking().AsQueryable();

                if (!string.IsNullOrEmpty(targetRole))
                    query = query.Where(n => n.TargetRole == targetRole || n.TargetRole == "All");

                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(n => n.OrganizationId == CurrentOrgId
                                         || string.IsNullOrEmpty(n.OrganizationId));

                return await query.OrderByDescending(n => n.CreatedAt).ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrieving notifications");
                return Enumerable.Empty<Notification>();
            }
        }

        public async Task<IEnumerable<Notification>> GetUnread(string? targetRole = null)
        {
            try
            {
                var query = _context.Notifications.AsNoTracking().Where(n => !n.IsRead).AsQueryable();

                if (!string.IsNullOrEmpty(targetRole))
                    query = query.Where(n => n.TargetRole == targetRole || n.TargetRole == "All");

                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(n => n.OrganizationId == CurrentOrgId
                                         || string.IsNullOrEmpty(n.OrganizationId));

                return await query.OrderByDescending(n => n.CreatedAt).ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrieving unread notifications");
                return Enumerable.Empty<Notification>();
            }
        }

        public async Task<Notification> Create(Notification notification)
        {
            try
            {
                notification.CreatedAt = DateTime.UtcNow;
                notification.IsRead = false;

                var orgId = CurrentOrgId;
                if (!string.IsNullOrEmpty(orgId))
                {
                    notification.OrganizationId = orgId;
                }

                _context.Notifications.Add(notification);
                await _context.SaveChangesAsync();
                return notification;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating notification");
                throw;
            }
        }

        public async Task<Notification> Create(NotificationType type, string title, string message,
            string targetRole = "All", string? relatedEntityType = null, int? relatedEntityId = null,
            string? organizationId = null)
        {
            try
            {
                var notification = new Notification
                {
                    Type = type,
                    Title = title,
                    Message = message,
                    TargetRole = targetRole,
                    RelatedEntityType = relatedEntityType,
                    RelatedEntityId = relatedEntityId,
                    IsRead = false,
                    CreatedAt = DateTime.UtcNow
                };

                // organizationId з параметра (фоновий сервіс) або з HTTP-контексту (API)
                var orgId = organizationId ?? CurrentOrgId;
                if (!string.IsNullOrEmpty(orgId))
                {
                    notification.OrganizationId = orgId;
                }

                _context.Notifications.Add(notification);
                await _context.SaveChangesAsync();
                return notification;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating notification of type {Type}", type);
                throw;
            }
        }

        public async Task<bool> MarkAsRead(int notificationId)
        {
            try
            {
                var query = _context.Notifications.AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(n => n.OrganizationId == CurrentOrgId
                                         || string.IsNullOrEmpty(n.OrganizationId));

                var notification = await query.FirstOrDefaultAsync(n => n.NotificationId == notificationId);
                if (notification is null) return false;

                notification.IsRead = true;
                await _context.SaveChangesAsync();
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error marking notification {Id} as read", notificationId);
                throw;
            }
        }

        public async Task<int> MarkAllAsRead(string? targetRole = null)
        {
            try
            {
                var query = _context.Notifications.Where(n => !n.IsRead).AsQueryable();

                if (!string.IsNullOrEmpty(targetRole))
                    query = query.Where(n => n.TargetRole == targetRole || n.TargetRole == "All");

                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(n => n.OrganizationId == CurrentOrgId
                                         || string.IsNullOrEmpty(n.OrganizationId));

                var notifications = await query.ToListAsync();
                notifications.ForEach(n => n.IsRead = true);

                await _context.SaveChangesAsync();
                return notifications.Count;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error marking all notifications as read");
                throw;
            }
        }
    }
}

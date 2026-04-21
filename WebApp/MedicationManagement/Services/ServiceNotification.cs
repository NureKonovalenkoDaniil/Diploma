using MedicationManagement.DBContext;
using MedicationManagement.Enums;
using MedicationManagement.Models;
using Microsoft.EntityFrameworkCore;

namespace MedicationManagement.Services
{
    public interface IServiceNotification
    {
        Task<IEnumerable<Notification>> GetAll(string? targetRole = null);
        Task<IEnumerable<Notification>> GetUnread(string? targetRole = null);
        Task<Notification> Create(Notification notification);
        Task<Notification> Create(NotificationType type, string title, string message,
            string targetRole = "All", string? relatedEntityType = null, int? relatedEntityId = null);
        Task<bool> MarkAsRead(int notificationId);
        Task<int> MarkAllAsRead(string? targetRole = null);
    }

    public class ServiceNotification : IServiceNotification
    {
        private readonly MedicineStorageContext _context;
        private readonly ILogger<ServiceNotification> _logger;

        public ServiceNotification(MedicineStorageContext context, ILogger<ServiceNotification> logger)
        {
            _context = context;
            _logger = logger;
        }

        public async Task<IEnumerable<Notification>> GetAll(string? targetRole = null)
        {
            try
            {
                var query = _context.Notifications.AsNoTracking().AsQueryable();

                if (!string.IsNullOrEmpty(targetRole))
                    query = query.Where(n => n.TargetRole == targetRole || n.TargetRole == "All");

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
            string targetRole = "All", string? relatedEntityType = null, int? relatedEntityId = null)
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
                var notification = await _context.Notifications.FindAsync(notificationId);
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

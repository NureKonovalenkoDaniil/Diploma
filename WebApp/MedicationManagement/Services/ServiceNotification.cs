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

        public ServiceNotification(MedicineStorageContext context)
        {
            _context = context;
        }

        public async Task<IEnumerable<Notification>> GetAll(string? targetRole = null)
        {
            var query = _context.Notifications.AsQueryable();

            if (!string.IsNullOrEmpty(targetRole))
                query = query.Where(n => n.TargetRole == targetRole || n.TargetRole == "All");

            return await query.OrderByDescending(n => n.CreatedAt).ToListAsync();
        }

        public async Task<IEnumerable<Notification>> GetUnread(string? targetRole = null)
        {
            var query = _context.Notifications.Where(n => !n.IsRead).AsQueryable();

            if (!string.IsNullOrEmpty(targetRole))
                query = query.Where(n => n.TargetRole == targetRole || n.TargetRole == "All");

            return await query.OrderByDescending(n => n.CreatedAt).ToListAsync();
        }

        public async Task<Notification> Create(Notification notification)
        {
            notification.CreatedAt = DateTime.UtcNow;
            notification.IsRead = false;
            _context.Notifications.Add(notification);
            await _context.SaveChangesAsync();
            return notification;
        }

        public async Task<Notification> Create(NotificationType type, string title, string message,
            string targetRole = "All", string? relatedEntityType = null, int? relatedEntityId = null)
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

        public async Task<bool> MarkAsRead(int notificationId)
        {
            var notification = await _context.Notifications.FindAsync(notificationId);
            if (notification is null) return false;

            notification.IsRead = true;
            await _context.SaveChangesAsync();
            return true;
        }

        public async Task<int> MarkAllAsRead(string? targetRole = null)
        {
            var query = _context.Notifications.Where(n => !n.IsRead).AsQueryable();

            if (!string.IsNullOrEmpty(targetRole))
                query = query.Where(n => n.TargetRole == targetRole || n.TargetRole == "All");

            var notifications = await query.ToListAsync();
            notifications.ForEach(n => n.IsRead = true);

            await _context.SaveChangesAsync();
            return notifications.Count;
        }
    }
}

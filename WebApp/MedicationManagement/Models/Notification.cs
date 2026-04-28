using MedicationManagement.Enums;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace MedicationManagement.Models
{
    /// <summary>
    /// Системне сповіщення для адміністраторів або користувачів.
    /// Генерується фоновими сервісами або API.
    /// </summary>
    public class Notification
    {
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public int NotificationId { get; set; }

        [Required]
        [MaxLength(36)]
        public string OrganizationId { get; set; } = string.Empty;

        [Required]
        public NotificationType Type { get; set; }

        [Required]
        [StringLength(200)]
        public string Title { get; set; } = string.Empty;

        [Required]
        public string Message { get; set; } = string.Empty;

        /// <summary>Роль-отримувач: "Administrator", "User" або "All"</summary>
        [StringLength(50)]
        public string TargetRole { get; set; } = "All";

        public bool IsRead { get; set; } = false;

        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        // Необов'язкові поля для навігації до пов'язаної сутності
        [StringLength(50)]
        public string? RelatedEntityType { get; set; }

        public int? RelatedEntityId { get; set; }
    }
}

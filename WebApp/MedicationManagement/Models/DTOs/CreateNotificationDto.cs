using MedicationManagement.Enums;
using System.ComponentModel.DataAnnotations;

namespace MedicationManagement.Models.DTOs
{
    public class CreateNotificationDto
    {
        [Required]
        public NotificationType Type { get; set; }

        [Required]
        [StringLength(100)]
        public string Title { get; set; } = string.Empty;

        [Required]
        [StringLength(500)]
        public string Message { get; set; } = string.Empty;

        public string TargetRole { get; set; } = "All";

        public string? RelatedEntityType { get; set; }

        public int? RelatedEntityId { get; set; }
    }
}

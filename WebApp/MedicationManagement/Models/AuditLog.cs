using MedicationManagement.Enums;

namespace MedicationManagement.Models
{
    public class AuditLog
    {
        public int Id { get; set; }

        public string Action { get; set; } = string.Empty;

        public string User { get; set; } = string.Empty;

        public DateTime Timestamp { get; set; }

        public string Details { get; set; } = string.Empty;

        // --- Нові поля для дипломного рівня ---

        /// <summary>Тип сутності, якої стосується дія (наприклад, "Medicine", "IoTDevice")</summary>
        public string? EntityType { get; set; }

        /// <summary>ID конкретної сутності, якої стосується дія</summary>
        public int? EntityId { get; set; }

        /// <summary>Рівень серйозності запису аудиту</summary>
        public AuditSeverity Severity { get; set; } = AuditSeverity.Info;
    }
}


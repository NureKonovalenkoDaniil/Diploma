using MedicationManagement.Enums;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace MedicationManagement.Models
{
    /// <summary>
    /// Інцидент порушення умов зберігання препаратів.
    /// Створюється автоматично фоновим сервісом моніторингу.
    /// </summary>
    public class StorageIncident
    {
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public int IncidentId { get; set; }

        // FK до IoT-пристрою, що зафіксував порушення
        [Required]
        public int DeviceId { get; set; }

        [ForeignKey(nameof(DeviceId))]
        public IoTDevice IoTDevice { get; set; } = null!;

        // Nullable FK до локації (якщо прив'язана)
        public int? LocationId { get; set; }

        [ForeignKey(nameof(LocationId))]
        public StorageLocation? StorageLocation { get; set; }

        [Required]
        public IncidentType IncidentType { get; set; }

        /// <summary>Зафіксоване відхилення значення (температура або вологість)</summary>
        public float DetectedValue { get; set; }

        public float ExpectedMin { get; set; }

        public float ExpectedMax { get; set; }

        public DateTime StartTime { get; set; } = DateTime.UtcNow;

        public DateTime? EndTime { get; set; }

        public IncidentStatus Status { get; set; } = IncidentStatus.Active;

        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }
}

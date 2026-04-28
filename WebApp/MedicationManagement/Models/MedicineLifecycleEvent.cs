using MedicationManagement.Enums;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace MedicationManagement.Models
{
    /// <summary>
    /// Подія в житті препарату: надходження, видача, переміщення, списання тощо.
    /// Формує повний аудит-журнал руху конкретного препарату.
    /// </summary>
    public class MedicineLifecycleEvent
    {
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public int EventId { get; set; }

        [Required]
        [MaxLength(36)]
        public string OrganizationId { get; set; } = string.Empty;

        // FK до препарату
        [Required]
        public int MedicineId { get; set; }

        [ForeignKey(nameof(MedicineId))]
        public Medicine Medicine { get; set; } = null!;

        [Required]
        public LifecycleEventType EventType { get; set; }

        [StringLength(500)]
        public string? Description { get; set; }

        /// <summary>Кількість препаратів, задіяних у події (nullable — не завжди релевантно)</summary>
        public int? Quantity { get; set; }

        /// <summary>Email або ім'я користувача, що здійснив дію</summary>
        [Required]
        [StringLength(256)]
        public string PerformedBy { get; set; } = string.Empty;

        public DateTime PerformedAt { get; set; } = DateTime.UtcNow;

        // Nullable FK до локації — куди/звідки переміщено, де видано тощо
        public int? RelatedLocationId { get; set; }

        [ForeignKey(nameof(RelatedLocationId))]
        public StorageLocation? RelatedLocation { get; set; }
    }
}

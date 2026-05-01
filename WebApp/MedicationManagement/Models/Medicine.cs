using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using MedicationManagement.Enums;

namespace MedicationManagement.Models
{
    // Medicine class
    public class Medicine
    {
        // Medicine property
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public int MedicineID { get; set; }

        [Required]
        [MaxLength(36)]
        public string OrganizationId { get; set; } = string.Empty;

        [Required]
        [StringLength(50)]
        public string Name { get; set; } = string.Empty;

        [Required]
        [StringLength(50)]
        public string Type { get; set; } = string.Empty;

        public DateTime ExpiryDate { get; set; }

        public int Quantity { get; set; }

        public string Category { get; set; } = string.Empty;

        [Required]
        public MedicineStatus Status { get; set; } = MedicineStatus.Active;

        // --- Нові поля для дипломного рівня ---

        [StringLength(100)]
        public string? Manufacturer { get; set; }

        [StringLength(50)]
        public string? BatchNumber { get; set; }

        public string? Description { get; set; }

        /// <summary>Мінімальна допустима температура зберігання (°C)</summary>
        public float? MinStorageTemp { get; set; }

        /// <summary>Максимальна допустима температура зберігання (°C)</summary>
        public float? MaxStorageTemp { get; set; }

        /// <summary>Мінімальна допустима вологість зберігання (%)</summary>
        public float? MinStorageHumidity { get; set; }

        /// <summary>Максимальна допустима вологість зберігання (%)</summary>
        public float? MaxStorageHumidity { get; set; }

        // Nullable FK до місця зберігання
        public int? StorageLocationId { get; set; }

        [ForeignKey(nameof(StorageLocationId))]
        public StorageLocation? StorageLocation { get; set; }
    }
}


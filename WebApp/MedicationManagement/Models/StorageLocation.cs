using MedicationManagement.Enums;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace MedicationManagement.Models
{
    /// <summary>
    /// Місце зберігання препаратів (холодильник, полиця, сховище тощо).
    /// Може бути прив'язане до IoT-пристрою для моніторингу умов.
    /// </summary>
    public class StorageLocation
    {
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public int LocationId { get; set; }

        [Required]
        [MaxLength(36)]
        public string OrganizationId { get; set; } = string.Empty;

        [Required]
        [StringLength(100)]
        public string Name { get; set; } = string.Empty;

        [StringLength(250)]
        public string? Address { get; set; }

        [Required]
        public StorageLocationType LocationType { get; set; }

        // Nullable FK до IoTDevice — місце може не мати прив'язаного пристрою
        public int? IoTDeviceId { get; set; }

        [ForeignKey(nameof(IoTDeviceId))]
        public IoTDevice? IoTDevice { get; set; }

        // Навігаційна властивість до препаратів у цій локації
        public ICollection<Medicine> Medicines { get; set; } = new List<Medicine>();
    }
}

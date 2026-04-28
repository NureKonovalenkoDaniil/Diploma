using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace MedicationManagement.Models
{
    /// <summary>
    /// IoT-пристрій для моніторингу умов зберігання препаратів.
    /// Містить порогові значення температури та вологості.
    /// Останні показники надходять через POST /api/storagecondition.
    /// </summary>
    public class IoTDevice
    {
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public int DeviceID { get; set; }

        [Required]
        [MaxLength(36)]
        public string OrganizationId { get; set; } = string.Empty;

        /// <summary>Фізичне розташування пристрою (наприклад, "Warehouse A", "Refrigerator 2")</summary>
        [Required]
        [StringLength(100)]
        public string Location { get; set; } = string.Empty;

        /// <summary>Тип пристрою (наприклад, "DHT22", "SHT31", "virtual")</summary>
        [Required]
        [StringLength(50)]
        public string Type { get; set; } = string.Empty;

        /// <summary>
        /// Додаткові параметри пристрою у вільному форматі.
        /// Рекомендований формат: JSON-рядок або "key=value;key=value".
        /// Приклад: {"firmware":"1.2.0","interval_sec":10}
        /// </summary>
        [StringLength(500)]
        public string Parameters { get; set; } = string.Empty;

        /// <summary>Чи активний пристрій. Неактивні пристрої ігноруються фоновим моніторингом.</summary>
        public bool IsActive { get; set; } = false;

        /// <summary>Мінімально допустима температура (°C)</summary>
        public float MinTemperature { get; set; }

        /// <summary>Максимально допустима температура (°C)</summary>
        public float MaxTemperature { get; set; }

        /// <summary>Мінімально допустима відносна вологість (%)</summary>
        public float MinHumidity { get; set; }

        /// <summary>Максимально допустима відносна вологість (%)</summary>
        public float MaxHumidity { get; set; }
    }
}

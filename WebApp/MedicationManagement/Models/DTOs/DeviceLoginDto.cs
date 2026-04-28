using System.ComponentModel.DataAnnotations;

namespace MedicationManagement.Models.DTOs
{
    public class DeviceLoginDto
    {
        [Required]
        public string DeviceId { get; set; } = string.Empty;
    }
}

using System.ComponentModel.DataAnnotations;

namespace MedicationManagement.Models.DTOs
{
    public class DeviceLoginDto
    {
        [Required]
        public int DeviceId { get; set; }
    }
}

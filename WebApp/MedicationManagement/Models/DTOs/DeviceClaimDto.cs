using System.ComponentModel.DataAnnotations;

namespace MedicationManagement.Models.DTOs
{
    public class DeviceClaimDto
    {
        [Required]
        public string DeviceId { get; set; } = string.Empty;
    }
}

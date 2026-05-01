using System.ComponentModel.DataAnnotations;

namespace MedicationManagement.Models.DTOs
{
    public class ResendConfirmationDto
    {
        [Required]
        [EmailAddress]
        public string Email { get; set; } = string.Empty;
    }
}

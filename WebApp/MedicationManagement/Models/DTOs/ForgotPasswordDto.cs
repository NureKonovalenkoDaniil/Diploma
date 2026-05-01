using System.ComponentModel.DataAnnotations;

namespace MedicationManagement.Models.DTOs
{
    public class ForgotPasswordDto
    {
        [Required]
        [EmailAddress]
        public string Email { get; set; } = string.Empty;
    }
}

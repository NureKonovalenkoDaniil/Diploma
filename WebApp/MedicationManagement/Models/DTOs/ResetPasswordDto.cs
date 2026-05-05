using System.ComponentModel.DataAnnotations;

namespace MedicationManagement.Models.DTOs
{
    public class ResetPasswordDto
    {
        [Required]
        [EmailAddress]
        public string Email { get; set; } = string.Empty;

        [Required(ErrorMessage = "Code is required")]
        public string Code { get; set; } = string.Empty;

        [Required]
        [StringLength(100, MinimumLength = 4, ErrorMessage = "Password must be at least {2} characters long.")]
        public string NewPassword { get; set; } = string.Empty;
    }
}

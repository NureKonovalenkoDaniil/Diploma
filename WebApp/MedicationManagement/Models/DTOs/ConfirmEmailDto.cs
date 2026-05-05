using System.ComponentModel.DataAnnotations;

namespace MedicationManagement.Models.DTOs
{
    public class ConfirmEmailDto
    {
        [Required(ErrorMessage = "Email is required")]
        [EmailAddress(ErrorMessage = "Invalid Email Address")]
        public string Email { get; set; } = null!;

        [Required(ErrorMessage = "Code is required")]
        public string Code { get; set; } = null!;
    }
}

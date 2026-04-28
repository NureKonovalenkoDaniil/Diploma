using System.ComponentModel.DataAnnotations;

namespace MedicationManagement.Models.DTOs
{
    public class CreateManagerDto
    {
        [Required]
        [EmailAddress]
        public string Email { get; set; } = string.Empty;

        [Required]
        [StringLength(100, ErrorMessage = "The {0} must be at least {2} characters long.", MinimumLength = 4)]
        public string Password { get; set; } = string.Empty;

        [Required]
        public string OrganizationId { get; set; } = string.Empty;
    }
}

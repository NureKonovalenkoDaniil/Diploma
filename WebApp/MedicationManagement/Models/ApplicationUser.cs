using Microsoft.AspNetCore.Identity;

namespace MedicationManagement.Models
{
    public class ApplicationUser : IdentityUser
    {
        public string OrganizationId { get; set; } = string.Empty;
    }
}

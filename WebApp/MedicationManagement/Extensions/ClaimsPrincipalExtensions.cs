using System.Security.Claims;

namespace MedicationManagement.Extensions
{
    public static class ClaimsPrincipalExtensions
    {
        public static string? GetOrganizationId(this ClaimsPrincipal principal)
        {
            return principal.FindFirst("OrganizationId")?.Value;
        }
    }
}

using MedicationManagement.DBContext;
using MedicationManagement.Models;
using MedicationManagement.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Threading.Tasks;

namespace MedicationManagement.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(AuthenticationSchemes = JwtBearerDefaults.AuthenticationScheme, Roles = "Administrator")]
    public class OrganizationController : ControllerBase
    {
        private readonly MedicineStorageContext _context;
        private readonly IServiceAuditLog _auditLogService;

        public OrganizationController(MedicineStorageContext context, IServiceAuditLog auditLogService)
        {
            _context = context;
            _auditLogService = auditLogService;
        }

        [HttpGet]
        public async Task<IActionResult> GetAll()
        {
            var orgs = await _context.Organizations.OrderBy(o => o.Name).ToListAsync();
            return Ok(orgs);
        }

        [HttpPost]
        public async Task<IActionResult> Create([FromBody] Organization model)
        {
            if (string.IsNullOrWhiteSpace(model.Name))
                return BadRequest("Organization Name is required");

            if (string.IsNullOrEmpty(model.Id))
            {
                model.Id = Guid.NewGuid().ToString();
            }

            _context.Organizations.Add(model);
            await _context.SaveChangesAsync();

            await _auditLogService.LogAction(
                "Create Organization", 
                User.Identity?.Name ?? "Unknown", 
                $"Created organization {model.Name} (ID: {model.Id})", 
                false
            );

            return Ok(model);
        }
    }
}

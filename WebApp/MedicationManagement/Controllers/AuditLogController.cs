using MedicationManagement.Models;
using MedicationManagement.DBContext;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace MedicationManagement.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(Roles = "Administrator")]
    public class AuditLogController : ControllerBase
    {
        private readonly MedicineStorageContext _context;
        private readonly ILogger<AuditLogController> _logger;

        public AuditLogController(MedicineStorageContext context, ILogger<AuditLogController> logger)
        {
            _context = context;
            _logger = logger;
        }

        [HttpGet]
        public async Task<IActionResult> GetLogs([FromQuery] DateTime? from = null, [FromQuery] DateTime? to = null, [FromQuery] string? user = null, [FromQuery] string? action = null)
        {
            try
            {
                var query = _context.AuditLogs.AsQueryable();

                if (from.HasValue)
                    query = query.Where(log => log.Timestamp >= from);

                if (to.HasValue)
                    query = query.Where(log => log.Timestamp <= to);

                if (!string.IsNullOrWhiteSpace(user))
                    query = query.Where(log => log.User.Contains(user));

                if (!string.IsNullOrWhiteSpace(action))
                    query = query.Where(log => log.Action.Contains(action));

                var logs = await query.OrderByDescending(log => log.Timestamp).ToListAsync();

                return Ok(logs);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrieving audit logs");
                return StatusCode(500, "Internal server error");
            }
        }
    }
}

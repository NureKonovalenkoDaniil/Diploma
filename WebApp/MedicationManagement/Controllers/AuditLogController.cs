using MedicationManagement.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MedicationManagement.Models.DTOs;

namespace MedicationManagement.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(Roles = "Administrator")]
    public class AuditLogController : ControllerBase
    {
        private readonly IServiceAuditLog _auditLogService;
        private readonly ILogger<AuditLogController> _logger;

        public AuditLogController(IServiceAuditLog auditLogService, ILogger<AuditLogController> logger)
        {
            _auditLogService = auditLogService;
            _logger = logger;
        }

        /// <summary>
        /// Отримати журнал аудиту з опціональними фільтрами.
        /// Доступно лише для Administrator.
        /// </summary>
        [HttpGet]
        public async Task<IActionResult> GetLogs(
            [FromQuery] DateTime? from = null,
            [FromQuery] DateTime? to = null,
            [FromQuery] string? user = null,
            [FromQuery] string? action = null)
        {
            try
            {
                var logs = await _auditLogService.GetLogs(from, to, user, action);
                return Ok(logs.Select(l => l.ToDto()));
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrieving audit logs");
                return StatusCode(500, "Internal server error");
            }
        }
    }
}

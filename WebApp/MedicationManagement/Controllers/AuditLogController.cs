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
            [FromQuery] string? from = null,
            [FromQuery] string? to = null,
            [FromQuery] string? user = null,
            [FromQuery] string? action = null)
        {
            try
            {
                DateTime? fromDate = null;
                DateTime? toDate = null;

                // Наївний парсинг дат (очікується формат "yyyy-MM-dd" без часового поясу)
                if (DateTime.TryParse(from, out var f))
                {
                    // Встановлюємо 00:00:00, але розглядаємо як локальний час (unspecified)
                    fromDate = new DateTime(f.Year, f.Month, f.Day, 0, 0, 0, DateTimeKind.Unspecified);
                }

                if (DateTime.TryParse(to, out var t))
                {
                    // Встановлюємо кінець доби 23:59:59
                    toDate = new DateTime(t.Year, t.Month, t.Day, 23, 59, 59, 999, DateTimeKind.Unspecified);
                }

                var logs = await _auditLogService.GetLogs(fromDate, toDate, user, action);
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

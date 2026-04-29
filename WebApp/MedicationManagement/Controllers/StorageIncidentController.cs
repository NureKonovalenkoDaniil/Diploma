using MedicationManagement.Enums;
using MedicationManagement.Models;
using MedicationManagement.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MedicationManagement.Models.DTOs;

namespace MedicationManagement.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(AuthenticationSchemes = JwtBearerDefaults.AuthenticationScheme)]
    public class StorageIncidentController : ControllerBase
    {
        private readonly IServiceStorageIncident _incidentService;
        private readonly IServiceAuditLog _auditLogService;

        public StorageIncidentController(
            IServiceStorageIncident incidentService,
            IServiceAuditLog auditLogService)
        {
            _incidentService = incidentService;
            _auditLogService = auditLogService;
        }

        /// <summary>Отримати всі інциденти</summary>
        [HttpGet]
        public async Task<IActionResult> GetAll()
        {
            var incidents = await _incidentService.GetAll();
            return Ok(incidents.Select(i => i.ToDto()));
        }

        /// <summary>Отримати лише активні інциденти</summary>
        [HttpGet("active")]
        public async Task<IActionResult> GetActive()
        {
            var incidents = await _incidentService.GetActive();
            return Ok(incidents.Select(i => i.ToDto()));
        }

        /// <summary>Отримати інцидент за ID</summary>
        [HttpGet("{id}")]
        public async Task<IActionResult> GetById(int id)
        {
            var incident = await _incidentService.GetById(id);
            if (incident is null) return NotFound();
            return Ok(incident.ToDto());
        }

        /// <summary>Створити новий інцидент (зазвичай викликається фоновим сервісом)</summary>
        [HttpPost]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> Create([FromBody] CreateStorageIncidentDto dto)
        {
            if (!ModelState.IsValid) return BadRequest(ModelState);

            var incident = dto.ToEntity();
            var created = await _incidentService.Create(incident);
            var user = User.Identity?.Name ?? "system";
            await _auditLogService.LogAction("CreateStorageIncident", user,
                $"Storage incident created. Device: {incident.DeviceId}, Type: {incident.IncidentType}",
                false, entityType: "StorageIncident", entityId: created.IncidentId,
                severity: AuditSeverity.Warning);

            return CreatedAtAction(nameof(GetById), new { id = created.IncidentId }, created.ToDto());
        }

        /// <summary>Позначити інцидент як вирішений</summary>
        [HttpPatch("{id}/resolve")]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> Resolve(int id)
        {
            var resolved = await _incidentService.Resolve(id);
            if (resolved is null) return NotFound();

            var user = User.Identity?.Name ?? "unknown";
            await _auditLogService.LogAction("ResolveStorageIncident", user,
                $"Incident {id} resolved.", false,
                entityType: "StorageIncident", entityId: id);

            return Ok(resolved.ToDto());
        }
    }
}

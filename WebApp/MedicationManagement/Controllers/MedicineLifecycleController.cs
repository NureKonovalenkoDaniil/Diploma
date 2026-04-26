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
    public class MedicineLifecycleController : ControllerBase
    {
        private readonly IServiceMedicineLifecycle _lifecycleService;
        private readonly IServiceAuditLog _auditLogService;

        public MedicineLifecycleController(
            IServiceMedicineLifecycle lifecycleService,
            IServiceAuditLog auditLogService)
        {
            _lifecycleService = lifecycleService;
            _auditLogService = auditLogService;
        }

        /// <summary>Отримати всі події lifecycle (всі препарати)</summary>
        [HttpGet]
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> GetAll()
        {
            var events = await _lifecycleService.GetAll();
            return Ok(events.Select(e => e.ToDto()));
        }

        /// <summary>Отримати всі події для конкретного препарату</summary>
        [HttpGet("medicine/{medicineId}")]
        public async Task<IActionResult> GetByMedicineId(int medicineId)
        {
            var events = await _lifecycleService.GetByMedicineId(medicineId);
            return Ok(events.Select(e => e.ToDto()));
        }

        /// <summary>Отримати подію за ID</summary>
        [HttpGet("{id}")]
        public async Task<IActionResult> GetById(int id)
        {
            var evt = await _lifecycleService.GetById(id);
            if (evt is null) return NotFound();
            return Ok(evt.ToDto());
        }

        /// <summary>Додати нову подію lifecycle для препарату</summary>
        [HttpPost]
        public async Task<IActionResult> AddEvent([FromBody] MedicineLifecycleEvent lifecycleEvent)
        {
            if (!ModelState.IsValid) return BadRequest(ModelState);

            var user = User.Identity?.Name ?? "unknown";
            lifecycleEvent.PerformedBy = user;

            var created = await _lifecycleService.AddEvent(lifecycleEvent);
            await _auditLogService.LogAction("AddLifecycleEvent", user,
                $"Lifecycle event '{lifecycleEvent.EventType}' added for Medicine ID: {lifecycleEvent.MedicineId}",
                false, entityType: "Medicine", entityId: lifecycleEvent.MedicineId);

            return CreatedAtAction(nameof(GetById), new { id = created.EventId }, created.ToDto());
        }
    }
}

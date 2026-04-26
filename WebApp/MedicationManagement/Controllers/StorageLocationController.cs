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
    public class StorageLocationController : ControllerBase
    {
        private readonly IServiceStorageLocation _locationService;
        private readonly IServiceAuditLog _auditLogService;

        public StorageLocationController(
            IServiceStorageLocation locationService,
            IServiceAuditLog auditLogService)
        {
            _locationService = locationService;
            _auditLogService = auditLogService;
        }

        /// <summary>Отримати всі локації зберігання</summary>
        [HttpGet]
        public async Task<IActionResult> GetAll()
        {
            var locations = await _locationService.GetAll();
            return Ok(locations.Select(l => l.ToDto()));
        }

        /// <summary>Отримати локацію за ID</summary>
        [HttpGet("{id}")]
        public async Task<IActionResult> GetById(int id)
        {
            var location = await _locationService.GetById(id);
            if (location is null) return NotFound();
            return Ok(location.ToDto());
        }

        /// <summary>Створити нову локацію зберігання</summary>
        [HttpPost]
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> Create([FromBody] StorageLocation location)
        {
            if (!ModelState.IsValid) return BadRequest(ModelState);

            var created = await _locationService.Create(location);
            var user = User.Identity?.Name ?? "unknown";
            await _auditLogService.LogAction("CreateStorageLocation", user,
                $"Created storage location: {created.Name}", false,
                entityType: "StorageLocation", entityId: created.LocationId);

            return CreatedAtAction(nameof(GetById), new { id = created.LocationId }, created.ToDto());
        }

        /// <summary>Оновити локацію зберігання</summary>
        [HttpPut("{id}")]
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> Update(int id, [FromBody] StorageLocation location)
        {
            if (!ModelState.IsValid) return BadRequest(ModelState);

            var updated = await _locationService.Update(id, location);
            if (updated is null) return NotFound();

            var user = User.Identity?.Name ?? "unknown";
            await _auditLogService.LogAction("UpdateStorageLocation", user,
                $"Updated storage location: {updated.Name}", false,
                entityType: "StorageLocation", entityId: id);

            return Ok(updated.ToDto());
        }

        /// <summary>Видалити локацію зберігання</summary>
        [HttpDelete("{id}")]
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> Delete(int id)
        {
            var success = await _locationService.Delete(id);
            if (!success) return NotFound();

            var user = User.Identity?.Name ?? "unknown";
            await _auditLogService.LogAction("DeleteStorageLocation", user,
                $"Deleted storage location ID: {id}", false,
                entityType: "StorageLocation", entityId: id);

            return NoContent();
        }
    }
}

using MedicationManagement.Models;
using MedicationManagement.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.JsonPatch;
using Microsoft.AspNetCore.Mvc;
using MedicationManagement.Models.DTOs;

namespace MedicationManagement.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(AuthenticationSchemes = JwtBearerDefaults.AuthenticationScheme)]
    public class MedicineController : ControllerBase
    {
        private readonly IServiceMedicine _medicineService;
        private readonly IServiceAuditLog _auditLogService;
        private readonly ILogger<MedicineController> _logger;

        // Constructor to inject dependencies
        public MedicineController(IServiceMedicine medicineService, IServiceAuditLog auditLogService, ILogger<MedicineController> logger)
        {
            _medicineService = medicineService;
            _auditLogService = auditLogService;
            _logger = logger;
        }

        // Endpoint to get medicines with low stock
        [HttpGet("low-stock")]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> GetLowStockMedicines([FromQuery] int threshold = 10)
        {
            try
            {
                var medicines = await _medicineService.GetLowStockMedicines(threshold);
                return Ok(medicines.Select(m => m.ToDto()));
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to retrieve low stock medicines");
                return StatusCode(500, "Internal server error");
            }
        }


        // Endpoint to get medicines that are expiring before a certain date
        [HttpGet("expiring")]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> GetExpiringMedicines([FromQuery] int daysThreshold = 7)
        {
            try
            {
                var targetDate = DateTime.UtcNow.AddDays(daysThreshold);
                var result = await _medicineService.GetExpiringMedicines(targetDate);
                return Ok(result.Select(m => m.ToDto()));
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to retrieve expiring medicines");
                return StatusCode(500, "Internal server error");
            }
        }

        // Endpoint to get replenishment recommendations for low stock medicines
        [HttpGet("replenishment-recommendations")]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> GetReplenishmentRecommendations()
        {
            try
            {
                var recommendations = await _medicineService.GetReplenishmentRecommendations();
                return Ok(recommendations);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to get replenishment recommendations");
                return StatusCode(500, "Internal server error");
            }
        }

        // Endpoint to create a new medicine
        [HttpPost]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> Create([FromBody] CreateMedicineDto medicineDto)
        {
            if (!ModelState.IsValid)
                return BadRequest(ModelState);

            try
            {
                var medicine = medicineDto.ToEntity();
                var result = await _medicineService.Create(medicine);
                if (result != null)
                {
                    await _auditLogService.LogAction("Create Medicine", User.Identity?.Name ?? "Unknown", $"Created medicine: {result.Name}.", false);
                    return Ok(result.ToDto());
                }
                return StatusCode(500, "Failed to create medicine");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error while creating medicine");
                return StatusCode(500, "Internal server error");
            }
        }

        // Endpoint to read all medicines
        [HttpGet]
        public async Task<IActionResult> Read()
        {
            try
            {
                var result = await _medicineService.Read();
                return Ok(result.Select(m => m.ToDto()));
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to retrieve medicines");
                return StatusCode(500, "Internal server error");
            }
        }

        // Endpoint to read a medicine by ID
        [HttpGet("{id}")]
        public async Task<IActionResult> ReadById(int id)
        {
            try
            {
                var result = await _medicineService.ReadById(id);
                if (result != null)
                {
                    return Ok(result.ToDto());
                }
                return NotFound($"Medication with id: {id} not found");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Failed to retrieve medicine with ID {id}");
                return StatusCode(500, "Internal server error");
            }
        }

        // Endpoint to update an existing medicine
        [HttpPatch("{id}")]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> Update(int id, [FromBody] JsonPatchDocument<Medicine> patchDoc)
        {
            if (patchDoc == null)
            {
                return BadRequest("Patch document is null");
            }

            try
            {
                var result = await _medicineService.Update(id, patchDoc);
                if (result != null)
                {
                    return Ok(result.ToDto());
                }
                return NotFound($"Medication with id: {id} not found");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Failed to update medicine with ID {id}");
                return StatusCode(500, "Internal server error");
            }
        }

        /// <summary>
        /// Атомарне переміщення препарату: оновлює StorageLocationId і створює lifecycle-подію Moved.
        /// </summary>
        [HttpPost("{id}/move")]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> Move(int id, [FromBody] MoveMedicineDto dto)
        {
            if (dto is null) return BadRequest("Request body is required");
            if (dto.StorageLocationId <= 0) return BadRequest("StorageLocationId must be a positive integer");

            try
            {
                var user = User.Identity?.Name ?? "Unknown";
                var result = await _medicineService.Move(
                    id,
                    dto.StorageLocationId,
                    performedBy: user,
                    description: dto.Description,
                    quantity: dto.Quantity);

                if (result is null)
                {
                    // Could be: medicine not found, or location not found / not accessible within tenant.
                    return NotFound("Medicine or target location not found");
                }

                await _auditLogService.LogAction(
                    "MoveMedicine",
                    user,
                    $"Moved medicine ID {id} to StorageLocationId {dto.StorageLocationId}.",
                    false,
                    entityType: "Medicine",
                    entityId: id);

                return Ok(result.ToDto());
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to move medicine with ID {Id}", id);
                return StatusCode(500, "Internal server error");
            }
        }

        /// <summary>Надходження препарату: збільшує Quantity та створює lifecycle-подію Received.</summary>
        [HttpPost("{id}/receive")]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> Receive(int id, [FromBody] ChangeMedicineQuantityDto dto)
        {
            if (dto is null) return BadRequest("Request body is required");
            if (dto.Quantity <= 0) return BadRequest("Quantity must be a positive integer");

            try
            {
                var user = User.Identity?.Name ?? "Unknown";
                var (medicine, error) = await _medicineService.Receive(
                    id,
                    dto.Quantity,
                    performedBy: user,
                    storageLocationId: dto.StorageLocationId,
                    description: dto.Description,
                    relatedLocationId: dto.RelatedLocationId);

                if (medicine is null)
                {
                    if (error == "Medicine not found") return NotFound(error);
                    return BadRequest(error ?? "Failed to receive stock");
                }

                await _auditLogService.LogAction(
                    "ReceiveMedicine",
                    user,
                    $"Received +{dto.Quantity} for medicine ID {id}.",
                    false,
                    entityType: "Medicine",
                    entityId: id);

                return Ok(medicine.ToDto());
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to receive stock for medicine with ID {Id}", id);
                return StatusCode(500, "Internal server error");
            }
        }

        /// <summary>Видача препарату: зменшує Quantity та створює lifecycle-подію Issued.</summary>
        [HttpPost("{id}/issue")]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> Issue(int id, [FromBody] ChangeMedicineQuantityDto dto)
        {
            if (dto is null) return BadRequest("Request body is required");
            if (dto.Quantity <= 0) return BadRequest("Quantity must be a positive integer");

            try
            {
                var user = User.Identity?.Name ?? "Unknown";
                var (medicine, error) = await _medicineService.Issue(
                    id,
                    dto.Quantity,
                    performedBy: user,
                    description: dto.Description,
                    relatedLocationId: dto.RelatedLocationId);

                if (medicine is null)
                {
                    if (error == "Medicine not found") return NotFound(error);
                    return BadRequest(error ?? "Failed to issue stock");
                }

                await _auditLogService.LogAction(
                    "IssueMedicine",
                    user,
                    $"Issued -{dto.Quantity} for medicine ID {id}.",
                    false,
                    entityType: "Medicine",
                    entityId: id);

                return Ok(medicine.ToDto());
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to issue stock for medicine with ID {Id}", id);
                return StatusCode(500, "Internal server error");
            }
        }

        /// <summary>Утилізація препарату: зменшує Quantity та створює lifecycle-подію Disposed.</summary>
        [HttpPost("{id}/dispose")]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> Dispose(int id, [FromBody] ChangeMedicineQuantityDto dto)
        {
            if (dto is null) return BadRequest("Request body is required");
            if (dto.Quantity < 0) return BadRequest("Quantity must be >= 0");

            try
            {
                var user = User.Identity?.Name ?? "Unknown";
                // For dispose: Quantity==0 means "dispose all".
                int? quantity = dto.Quantity == 0 ? null : dto.Quantity;

                var (medicine, error) = await _medicineService.Dispose(
                    id,
                    quantity,
                    performedBy: user,
                    description: dto.Description,
                    relatedLocationId: dto.RelatedLocationId);

                if (medicine is null)
                {
                    if (error == "Medicine not found") return NotFound(error);
                    return BadRequest(error ?? "Failed to dispose stock");
                }

                await _auditLogService.LogAction(
                    "DisposeMedicine",
                    user,
                    $"Disposed {(quantity.HasValue ? $"-{quantity.Value}" : "all")} for medicine ID {id}.",
                    false,
                    entityType: "Medicine",
                    entityId: id);

                return Ok(medicine.ToDto());
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to dispose stock for medicine with ID {Id}", id);
                return StatusCode(500, "Internal server error");
            }
        }

        // Endpoint to delete a medicine by ID
        [HttpDelete("{id}")]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> Delete(int id)
        {
            try
            {
                var result = await _medicineService.Delete(id);
                if (result)
                {
                    await _auditLogService.LogAction("Delete Medicine", User.Identity?.Name ?? "Unknown", $"Deleted medicine with ID: {id}.", false);
                    return Ok($"Medication with id: {id} deleted");
                }
                return NotFound($"Medication with id: {id} not found");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Failed to delete medicine with ID {id}");
                return StatusCode(500, "Internal server error");
            }
        }
    }
}

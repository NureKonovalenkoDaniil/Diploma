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
        [Authorize(Roles = "Administrator")]
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
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> GetExpiringMedicines([FromQuery] int daysThreshold = 7)
        {
            try
            {
                var targetDate = DateTime.Now.AddDays(daysThreshold);
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
        [Authorize(Roles = "Administrator")]
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
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> Create([FromBody] Medicine medicine)
        {
            if (!ModelState.IsValid)
                return BadRequest(ModelState);

            try
            {
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
        [Authorize(Roles = "Administrator")]
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


        // Endpoint to delete a medicine by ID
        [HttpDelete("{id}")]
        [Authorize(Roles = "Administrator")]
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

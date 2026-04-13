using MedicationManagement.Models;
using MedicationManagement.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.JsonPatch;
using Microsoft.AspNetCore.Mvc;

namespace MedicationManagement.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(AuthenticationSchemes = JwtBearerDefaults.AuthenticationScheme)]
    public class StorageConditionController : ControllerBase
    {
        private readonly IServiceStorageCondition _storageConditionService;
        private readonly IServiceAuditLog _auditLogService;
        private readonly ILogger<StorageConditionController> _logger;

        public StorageConditionController(IServiceStorageCondition storageConditionService, IServiceAuditLog auditLogService, ILogger<StorageConditionController> logger)
        {
            _storageConditionService = storageConditionService;
            _auditLogService = auditLogService;
            _logger = logger;
        }

        [HttpGet("checkCondition")]
        public async Task<IActionResult> CheckStorageConditionsForAllDevices()
        {
            try
            {
                var result = await _storageConditionService.CheckStorageConditionsForAllDevices();
                return Ok(result);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error checking storage conditions");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpPost]
        public async Task<IActionResult> Create([FromBody] StorageCondition storageCondition)
        {
            if (!ModelState.IsValid)
                return BadRequest(ModelState);

            try
            {
                var result = await _storageConditionService.Create(storageCondition);
                if (result != null)
                {
                    string source = User.Identity?.Name ?? $"Sensor {storageCondition.DeviceID}";
                    bool isSensor = User.Identity == null;
                    await _auditLogService.LogAction("Create Condition", source, $"Created Condition: {result.ConditionID}.", isSensor);
                    return Ok(result);
                }
                return BadRequest("Could not create condition");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating storage condition");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpGet]
        public async Task<IActionResult> Read()
        {
            try
            {
                var result = await _storageConditionService.Read();
                return Ok(result);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error reading storage conditions");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpGet("{id}")]
        public async Task<IActionResult> ReadById(int id)
        {
            try
            {
                var result = await _storageConditionService.ReadById(id);
                if (result != null)
                {
                    return Ok(result);
                }
                return NotFound($"Condition with id: {id} not found");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error reading condition with ID {id}");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpPatch("{id}")]
        public async Task<IActionResult> Update(int id, [FromBody] JsonPatchDocument<StorageCondition> patchDoc)
        {
            if (patchDoc == null)
                return BadRequest("Patch document is null");

            try
            {
                var result = await _storageConditionService.Update(id, patchDoc);
                if (result != null)
                {
                    return Ok(result);
                }
                return NotFound($"Condition with id: {id} not found");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error updating condition with ID {id}");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpDelete("{id}")]
        public async Task<IActionResult> Delete(int id)
        {
            try
            {
                var result = await _storageConditionService.Delete(id);
                if (result)
                {
                    string source = User.Identity?.Name ?? $"Sensor {id}";
                    bool isSensor = User.Identity == null;
                    await _auditLogService.LogAction("Delete Condition", source, $"Deleted Condition: {id}.", isSensor);
                    return Ok(result);
                }
                return NotFound($"Condition with id: {id} not found");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error deleting condition with ID {id}");
                return StatusCode(500, "Internal server error");
            }
        }
    }
}

using MedicationManagement.Models;
using MedicationManagement.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.JsonPatch;
using Microsoft.AspNetCore.Mvc;
using MedicationManagement.Models.DTOs;

namespace MedicationManagement.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(AuthenticationSchemes = JwtBearerDefaults.AuthenticationScheme)]
    public class IoTDeviceController : ControllerBase
    {
        private readonly IServiceIoTDevice _iotDeviceService;
        private readonly IServiceAuditLog _auditLogService;
        private readonly ILogger<IoTDeviceController> _logger;

        public IoTDeviceController(IServiceIoTDevice iotDeviceService, IServiceAuditLog auditLogService, ILogger<IoTDeviceController> logger)
        {
            _iotDeviceService = iotDeviceService;
            _auditLogService = auditLogService;
            _logger = logger;
        }

        [HttpPatch("setstatus/{deviceId}")]
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> SetSensorStatus(string deviceId, bool isActive)
        {
            try
            {
                var result = await _iotDeviceService.SetSensorStatus(deviceId, isActive);
                if (!result)
                    return NotFound("Sensor not found");

                var action = isActive ? "Activate Sensor" : "Deactivate Sensor";
                await _auditLogService.LogAction(action, User.Identity?.Name ?? "Unknown", $"Sensor {deviceId} status set to {(isActive ? "active" : "inactive")}.", false);
                return Ok($"Sensor {deviceId} status set to {(isActive ? "active" : "inactive")}.");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error setting sensor status for sensor {deviceId}");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpGet("conditions/{deviceId}")]
        public async Task<IActionResult> GetConditionsByDeviceId(string deviceId)
        {
            try
            {
                var conditions = await _iotDeviceService.GetConditionsByDeviceId(deviceId);
                return Ok(conditions.Select(c => c.ToDto()));
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error fetching conditions for device {deviceId}");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpPost]
        [Authorize(Roles = "Administrator,Manager")]
        public async Task<IActionResult> Create([FromBody] IoTDeviceDto iotDeviceDto)
        {
            if (!ModelState.IsValid)
                return BadRequest(ModelState);

            try
            {
                var iotDevice = iotDeviceDto.ToEntity();
                var result = await _iotDeviceService.Create(iotDevice);
                if (result != null)
                {
                    await _auditLogService.LogAction("Create Sensor", User.Identity?.Name ?? "Unknown", $"Created sensor: {result.DeviceID}.", false);
                    return Ok(result.ToDto());
                }
                return BadRequest("Could not create IoT device");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating IoT device");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpGet]
        public async Task<IActionResult> Read()
        {
            try
            {
                var result = await _iotDeviceService.Read();
                return Ok(result.Select(d => d.ToDto()));
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error reading IoT devices");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpGet("{id}")]
        public async Task<IActionResult> ReadById(string id)
        {
            try
            {
                var result = await _iotDeviceService.ReadById(id);
                if (result != null)
                {
                    return Ok(result.ToDto());
                }
                return NotFound($"IoT Device with id: {id} not found");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error reading IoT device with ID {id}");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpPatch("{id}")]
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> Update(string id, [FromBody] JsonPatchDocument<IoTDevice> patchDoc)
        {
            if (patchDoc == null)
                return BadRequest("Patch document is null");

            try
            {
                var result = await _iotDeviceService.Update(id, patchDoc);
                if (result != null)
                {
                    return Ok(result.ToDto());
                }
                return NotFound($"Device with id: {id} not found");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error updating IoT device with ID {id}");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpDelete("{id}")]
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> Delete(string id)
        {
            try
            {
                var result = await _iotDeviceService.Delete(id);
                if (result)
                {
                    await _auditLogService.LogAction("Delete Sensor", User.Identity?.Name ?? "Unknown", $"Deleted sensor: {id}.", false);
                    return Ok($"IoT Device with id: {id} deleted");
                }
                return NotFound($"IoT Device with id: {id} not found");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error deleting IoT device with ID {id}");
                return StatusCode(500, "Internal server error");
            }
        }
    }
}

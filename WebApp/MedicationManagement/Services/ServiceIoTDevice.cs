using MedicationManagement.DBContext;
using MedicationManagement.Models;
using Microsoft.AspNetCore.JsonPatch;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Extensions;
using Microsoft.AspNetCore.WebUtilities;
using System.Security.Cryptography;

namespace MedicationManagement.Services
{
    public interface IServiceIoTDevice
    {
        Task<bool> SetSensorStatus(string deviceId, bool isActive);
        Task<List<StorageCondition>> GetConditionsByDeviceId(string deviceId);
        Task<IoTDevice?> Create(IoTDevice IoTDevice);
        Task<(IoTDevice? device, string? deviceSecret)> CreateWithSecret(IoTDevice IoTDevice);
        Task<IoTDevice?> CreateWithoutSecret(IoTDevice IoTDevice);
        Task<(IoTDevice? device, string? deviceSecret)> ClaimDeviceSecret(string deviceId);
        Task<IEnumerable<IoTDevice>> Read();
        Task<IoTDevice?> ReadById(string id);
        Task<IoTDevice?> ValidateDeviceSecret(string deviceId, string deviceSecret);
        Task<IoTDevice?> Update(string id, JsonPatchDocument<IoTDevice> patchDocument);
        Task<bool> Delete(string id);
    }

    public class ServiceIoTDevice : IServiceIoTDevice
    {
        private readonly MedicineStorageContext _context;
        private readonly ILogger<ServiceIoTDevice> _logger;
        private readonly IHttpContextAccessor _httpContextAccessor;
        private readonly PasswordHasher<IoTDevice> _secretHasher = new();

        public ServiceIoTDevice(MedicineStorageContext context, ILogger<ServiceIoTDevice> logger, IHttpContextAccessor httpContextAccessor)
        {
            _context = context;
            _logger = logger;
            _httpContextAccessor = httpContextAccessor;
        }

        private string? CurrentOrgId => _httpContextAccessor.HttpContext?.User.GetOrganizationId();
        private bool IsAdmin => _httpContextAccessor.HttpContext?.User.IsInRole("Administrator") ?? true;

        public async Task<bool> SetSensorStatus(string deviceId, bool isActive)
        {
            try
            {
                var query = _context.IoTDevices.AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(d => d.OrganizationId == CurrentOrgId);
                var sensor = await query.FirstOrDefaultAsync(d => d.DeviceID == deviceId);
                if (sensor == null)
                {
                    _logger.LogWarning($"Sensor with ID {deviceId} not found");
                    return false;
                }

                sensor.IsActive = isActive;
                _context.IoTDevices.Update(sensor);
                await _context.SaveChangesAsync();
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error updating sensor status for ID {deviceId}");
                return false;
            }
        }

        public async Task<List<StorageCondition>> GetConditionsByDeviceId(string deviceId)
        {
            try
            {
                var query = _context.StorageConditions.AsNoTracking()
                    .Where(sc => sc.DeviceID == deviceId)
                    .Include(sc => sc.IoTDevice).AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(sc => sc.OrganizationId == CurrentOrgId);
                return await query.ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error fetching conditions for device ID {deviceId}");
                return new List<StorageCondition>();
            }
        }

        public async Task<IoTDevice?> Create(IoTDevice IoTDevice)
        {
            return await CreateWithoutSecret(IoTDevice);
        }

        public async Task<(IoTDevice? device, string? deviceSecret)> CreateWithSecret(IoTDevice IoTDevice)
        {
            if (IoTDevice == null)
            {
                _logger.LogWarning("Attempted to create null IoTDevice");
                return (null, null);
            }

            try
            {
                var orgId = CurrentOrgId;
                if (!string.IsNullOrEmpty(orgId))
                {
                    IoTDevice.OrganizationId = orgId;
                }

                var deviceSecret = GenerateDeviceSecret();
                IoTDevice.DeviceSecretHash = _secretHasher.HashPassword(IoTDevice, deviceSecret);

                await _context.IoTDevices.AddAsync(IoTDevice);
                await _context.SaveChangesAsync();
                return (IoTDevice, deviceSecret);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating IoT device");
                return (null, null);
            }
        }

        public async Task<IoTDevice?> CreateWithoutSecret(IoTDevice IoTDevice)
        {
            if (IoTDevice == null)
            {
                _logger.LogWarning("Attempted to create null IoTDevice");
                return null;
            }

            try
            {
                var orgId = CurrentOrgId;
                if (!string.IsNullOrEmpty(orgId))
                {
                    IoTDevice.OrganizationId = orgId;
                }

                IoTDevice.DeviceSecretHash = string.Empty;
                await _context.IoTDevices.AddAsync(IoTDevice);
                await _context.SaveChangesAsync();
                return IoTDevice;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating IoT device without secret");
                return null;
            }
        }

        public async Task<(IoTDevice? device, string? deviceSecret)> ClaimDeviceSecret(string deviceId)
        {
            if (string.IsNullOrWhiteSpace(deviceId))
            {
                _logger.LogWarning("DeviceId is required for claim");
                return (null, null);
            }

            try
            {
                var device = await _context.IoTDevices.FirstOrDefaultAsync(d => d.DeviceID == deviceId);
                if (device == null)
                {
                    _logger.LogWarning("Device {Id} not found for claim", deviceId);
                    return (null, null);
                }

                if (!device.IsActive)
                {
                    _logger.LogWarning("Device {Id} is inactive for claim", deviceId);
                    return (null, null);
                }

                if (!string.IsNullOrEmpty(device.DeviceSecretHash))
                {
                    _logger.LogWarning("Device {Id} already claimed", deviceId);
                    return (null, null);
                }

                var deviceSecret = GenerateDeviceSecret();
                device.DeviceSecretHash = _secretHasher.HashPassword(device, deviceSecret);

                _context.IoTDevices.Update(device);
                await _context.SaveChangesAsync();
                return (device, deviceSecret);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error claiming IoT device {Id}", deviceId);
                return (null, null);
            }
        }

        public async Task<IEnumerable<IoTDevice>> Read()
        {
            try
            {
                var query = _context.IoTDevices.AsNoTracking();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(d => d.OrganizationId == CurrentOrgId);
                return await query.ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error reading IoT devices");
                return Enumerable.Empty<IoTDevice>();
            }
        }

        public async Task<IoTDevice?> ReadById(string id)
        {
            try
            {
                var query = _context.IoTDevices.AsNoTracking().Where(d => d.DeviceID == id);
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(d => d.OrganizationId == CurrentOrgId);
                var device = await query.FirstOrDefaultAsync();
                if (device == null)
                {
                    _logger.LogWarning($"IoTDevice with ID {id} not found");
                }
                return device;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error reading IoT device with ID {id}");
                return null;
            }
        }

        public async Task<IoTDevice?> ValidateDeviceSecret(string deviceId, string deviceSecret)
        {
            try
            {
                var device = await _context.IoTDevices.AsNoTracking()
                    .FirstOrDefaultAsync(d => d.DeviceID == deviceId);

                if (device == null)
                {
                    _logger.LogWarning("IoTDevice with ID {Id} not found for secret validation", deviceId);
                    return null;
                }

                var result = _secretHasher.VerifyHashedPassword(device, device.DeviceSecretHash, deviceSecret);
                return result == PasswordVerificationResult.Success ? device : null;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error validating device secret for ID {Id}", deviceId);
                return null;
            }
        }

        public async Task<IoTDevice?> Update(string id, JsonPatchDocument<IoTDevice> patchDocument)
        {
            if (patchDocument == null)
            {
                _logger.LogWarning("Patch document is null");
                return null;
            }

            try
            {
                var query = _context.IoTDevices.AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(d => d.OrganizationId == CurrentOrgId);
                var deviceToUpdate = await query.FirstOrDefaultAsync(d => d.DeviceID == id);
                if (deviceToUpdate == null)
                {
                    _logger.LogWarning($"IoTDevice with ID {id} not found");
                    return null;
                }

                patchDocument.ApplyTo(deviceToUpdate);
                await _context.SaveChangesAsync();
                return deviceToUpdate;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error updating IoT device with ID {id}");
                return null;
            }
        }

        public async Task<bool> Delete(string id)
        {
            try
            {
                var query = _context.IoTDevices.AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(d => d.OrganizationId == CurrentOrgId);
                var device = await query.FirstOrDefaultAsync(d => d.DeviceID == id);
                if (device == null)
                {
                    _logger.LogWarning($"IoTDevice with ID {id} not found");
                    return false;
                }

                var incidents = _context.StorageIncidents.Where(i => i.DeviceId == id);
                var conditions = _context.StorageConditions.Where(c => c.DeviceID == id);
                if (!IsAdmin && !string.IsNullOrEmpty(device.OrganizationId))
                {
                    incidents = incidents.Where(i => i.OrganizationId == device.OrganizationId);
                    conditions = conditions.Where(c => c.OrganizationId == device.OrganizationId);
                }

                _context.StorageIncidents.RemoveRange(incidents);
                _context.StorageConditions.RemoveRange(conditions);

                _context.IoTDevices.Remove(device);
                await _context.SaveChangesAsync();
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error deleting IoT device with ID {id}");
                return false;
            }
        }

        private static string GenerateDeviceSecret()
        {
            Span<byte> bytes = stackalloc byte[32];
            RandomNumberGenerator.Fill(bytes);
            return WebEncoders.Base64UrlEncode(bytes);
        }
    }
}

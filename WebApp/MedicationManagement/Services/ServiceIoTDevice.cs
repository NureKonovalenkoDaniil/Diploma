using MedicationManagement.DBContext;
using MedicationManagement.Models;
using Microsoft.AspNetCore.JsonPatch;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Extensions;

namespace MedicationManagement.Services
{
    public interface IServiceIoTDevice
    {
        Task<bool> SetSensorStatus(string deviceId, bool isActive);
        Task<List<StorageCondition>> GetConditionsByDeviceId(string deviceId);
        Task<IoTDevice?> Create(IoTDevice IoTDevice);
        Task<IEnumerable<IoTDevice>> Read();
        Task<IoTDevice?> ReadById(string id);
        Task<IoTDevice?> Update(string id, JsonPatchDocument<IoTDevice> patchDocument);
        Task<bool> Delete(string id);
    }

    public class ServiceIoTDevice : IServiceIoTDevice
    {
        private readonly MedicineStorageContext _context;
        private readonly ILogger<ServiceIoTDevice> _logger;
        private readonly IHttpContextAccessor _httpContextAccessor;

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
                await _context.IoTDevices.AddAsync(IoTDevice);
                await _context.SaveChangesAsync();
                return IoTDevice;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating IoT device");
                return null;
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
    }
}

using MedicationManagement.DBContext;
using MedicationManagement.Models;
using Microsoft.AspNetCore.JsonPatch;
using Microsoft.EntityFrameworkCore;

namespace MedicationManagement.Services
{
    public interface IServiceIoTDevice
    {
        Task<bool> SetSensorStatus(int deviceId, bool isActive);
        Task<List<StorageCondition>> GetConditionsByDeviceId(int deviceId);
        Task<IoTDevice> Create(IoTDevice IoTDevice);
        Task<IEnumerable<IoTDevice>> Read();
        Task<IoTDevice> ReadById(int id);
        Task<IoTDevice> Update(int id, JsonPatchDocument<IoTDevice> patchDocument);
        Task<bool> Delete(int id);
    }

    public class ServiceIoTDevice : IServiceIoTDevice
    {
        private readonly MedicineStorageContext _context;
        private readonly ILogger<ServiceIoTDevice> _logger;

        public ServiceIoTDevice(MedicineStorageContext context, ILogger<ServiceIoTDevice> logger)
        {
            _context = context;
            _logger = logger;
        }

        public async Task<bool> SetSensorStatus(int deviceId, bool isActive)
        {
            try
            {
                var sensor = await _context.IoTDevices.FindAsync(deviceId);
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

        public async Task<List<StorageCondition>> GetConditionsByDeviceId(int deviceId)
        {
            try
            {
                return await _context.StorageConditions
                    .Where(sc => sc.DeviceID == deviceId)
                    .Include(sc => sc.IoTDevice)
                    .ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error fetching conditions for device ID {deviceId}");
                return new List<StorageCondition>();
            }
        }

        public async Task<IoTDevice> Create(IoTDevice IoTDevice)
        {
            if (IoTDevice == null)
            {
                _logger.LogWarning("Attempted to create null IoTDevice");
                return null;
            }

            try
            {
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
                return await _context.IoTDevices.ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error reading IoT devices");
                return Enumerable.Empty<IoTDevice>();
            }
        }

        public async Task<IoTDevice> ReadById(int id)
        {
            try
            {
                var device = await _context.IoTDevices.FindAsync(id);
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

        public async Task<IoTDevice> Update(int id, JsonPatchDocument<IoTDevice> patchDocument)
        {
            if (patchDocument == null)
            {
                _logger.LogWarning("Patch document is null");
                return null;
            }

            try
            {
                var deviceToUpdate = await _context.IoTDevices.FindAsync(id);
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

        public async Task<bool> Delete(int id)
        {
            try
            {
                var device = await _context.IoTDevices.FindAsync(id);
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

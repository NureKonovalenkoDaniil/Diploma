using MedicationManagement.DBContext;
using MedicationManagement.Models;
using Microsoft.AspNetCore.JsonPatch;
using Microsoft.EntityFrameworkCore;

namespace MedicationManagement.Services
{
    public interface IServiceStorageCondition
    {
        Task<List<string>> CheckStorageConditionsForAllDevices();
        Task<StorageCondition> Create(StorageCondition storageCondition);
        Task<IEnumerable<StorageCondition>> Read();
        Task<StorageCondition> ReadById(int id);
        Task<StorageCondition> Update(int id, JsonPatchDocument<StorageCondition> patchDocument);
        Task<bool> Delete(int id);
    }

    public class ServiceStorageCondition : IServiceStorageCondition
    {
        private readonly MedicineStorageContext _context;
        private readonly ILogger<ServiceStorageCondition> _logger;

        public ServiceStorageCondition(MedicineStorageContext context, ILogger<ServiceStorageCondition> logger)
        {
            _context = context;
            _logger = logger;
        }

        public async Task<List<string>> CheckStorageConditionsForAllDevices()
        {
            var violations = new List<string>();
            try
            {
                var devices = await _context.IoTDevices.ToListAsync();

                foreach (var device in devices)
                {
                    if (device.IsActive == false)
                        continue;
                    var condition = await _context.StorageConditions
                        .Where(sc => sc.DeviceID == device.DeviceID)
                        .OrderByDescending(sc => sc.Timestamp)
                        .FirstOrDefaultAsync();

                    if (condition != null)
                    {
                        if (condition.Temperature < device.MinTemperature || condition.Temperature > device.MaxTemperature)
                        {
                            violations.Add($"Temperature violation for Device {device.DeviceID} at {condition.Timestamp}: {condition.Temperature}°C (Expected: {device.MinTemperature}–{device.MaxTemperature}°C)");
                        }

                        if (condition.Humidity < device.MinHumidity || condition.Humidity > device.MaxHumidity)
                        {
                            violations.Add($"Humidity violation for Device {device.DeviceID} at {condition.Timestamp}: {condition.Humidity}% (Expected: {device.MinHumidity}–{device.MaxHumidity}%)");
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error checking storage conditions for devices");
            }

            return violations;
        }

        public async Task<StorageCondition> Create(StorageCondition storageCondition)
        {
            if (storageCondition == null)
            {
                _logger.LogWarning("Attempted to create null StorageCondition");
                return null;
            }

            try
            {
                var device = await _context.IoTDevices.FindAsync(storageCondition.DeviceID);
                if (device == null)
                {
                    _logger.LogWarning($"Device with ID {storageCondition.DeviceID} not found");
                    return null;
                }

                storageCondition.Timestamp = DateTime.Now;
                await _context.StorageConditions.AddAsync(storageCondition);
                await _context.SaveChangesAsync();
                return storageCondition;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating storage condition");
                return null;
            }
        }

        public async Task<IEnumerable<StorageCondition>> Read()
        {
            try
            {
                return await _context.StorageConditions.Include(sc => sc.IoTDevice).ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error reading storage conditions");
                return Enumerable.Empty<StorageCondition>();
            }
        }

        public async Task<StorageCondition> ReadById(int id)
        {
            try
            {
                var condition = await _context.StorageConditions.Include(sc => sc.IoTDevice).FirstOrDefaultAsync(sc => sc.ConditionID == id);
                if (condition == null)
                {
                    _logger.LogWarning($"StorageCondition with ID {id} not found");
                }
                return condition;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error reading storage condition with ID {id}");
                return null;
            }
        }

        public async Task<StorageCondition> Update(int id, JsonPatchDocument<StorageCondition> patchDocument)
        {
            if (patchDocument == null)
            {
                _logger.LogWarning("Patch document is null");
                return null;
            }

            try
            {
                var conditionToUpdate = await _context.StorageConditions.FindAsync(id);
                if (conditionToUpdate == null)
                {
                    _logger.LogWarning($"StorageCondition with ID {id} not found");
                    return null;
                }

                patchDocument.ApplyTo(conditionToUpdate);
                await _context.SaveChangesAsync();
                return conditionToUpdate;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error updating storage condition with ID {id}");
                return null;
            }
        }

        public async Task<bool> Delete(int id)
        {
            try
            {
                var condition = await _context.StorageConditions.FindAsync(id);
                if (condition == null)
                {
                    _logger.LogWarning($"StorageCondition with ID {id} not found");
                    return false;
                }

                _context.StorageConditions.Remove(condition);
                await _context.SaveChangesAsync();
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error deleting storage condition with ID {id}");
                return false;
            }
        }
    }
}
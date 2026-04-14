using MedicationManagement.DBContext;
using MedicationManagement.Models;
using Microsoft.EntityFrameworkCore;

namespace MedicationManagement.Services
{
    public interface IServiceStorageLocation
    {
        Task<IEnumerable<StorageLocation>> GetAll();
        Task<StorageLocation?> GetById(int id);
        Task<StorageLocation> Create(StorageLocation location);
        Task<StorageLocation?> Update(int id, StorageLocation location);
        Task<bool> Delete(int id);
    }

    public class ServiceStorageLocation : IServiceStorageLocation
    {
        private readonly MedicineStorageContext _context;

        public ServiceStorageLocation(MedicineStorageContext context)
        {
            _context = context;
        }

        public async Task<IEnumerable<StorageLocation>> GetAll()
        {
            return await _context.StorageLocations
                .Include(l => l.IoTDevice)
                .ToListAsync();
        }

        public async Task<StorageLocation?> GetById(int id)
        {
            return await _context.StorageLocations
                .Include(l => l.IoTDevice)
                .Include(l => l.Medicines)
                .FirstOrDefaultAsync(l => l.LocationId == id);
        }

        public async Task<StorageLocation> Create(StorageLocation location)
        {
            _context.StorageLocations.Add(location);
            await _context.SaveChangesAsync();
            return location;
        }

        public async Task<StorageLocation?> Update(int id, StorageLocation updated)
        {
            var existing = await _context.StorageLocations.FindAsync(id);
            if (existing is null) return null;

            existing.Name = updated.Name;
            existing.Address = updated.Address;
            existing.LocationType = updated.LocationType;
            existing.IoTDeviceId = updated.IoTDeviceId;

            await _context.SaveChangesAsync();
            return existing;
        }

        public async Task<bool> Delete(int id)
        {
            var location = await _context.StorageLocations.FindAsync(id);
            if (location is null) return false;

            _context.StorageLocations.Remove(location);
            await _context.SaveChangesAsync();
            return true;
        }
    }
}

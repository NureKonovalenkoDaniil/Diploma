using MedicationManagement.DBContext;
using MedicationManagement.Models;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Extensions;

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
        private readonly ILogger<ServiceStorageLocation> _logger;
        private readonly IHttpContextAccessor _httpContextAccessor;

        public ServiceStorageLocation(MedicineStorageContext context, ILogger<ServiceStorageLocation> logger, IHttpContextAccessor httpContextAccessor)
        {
            _context = context;
            _logger = logger;
            _httpContextAccessor = httpContextAccessor;
        }

        private string? CurrentOrgId => _httpContextAccessor.HttpContext?.User.GetOrganizationId();
        private bool IsAdmin => _httpContextAccessor.HttpContext?.User.IsInRole("Administrator") ?? true;

        public async Task<IEnumerable<StorageLocation>> GetAll()
        {
            try
            {
                var query = _context.StorageLocations
                    .AsNoTracking()
                    .Include(l => l.IoTDevice).AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(l => l.OrganizationId == CurrentOrgId);
                return await query.ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrieving storage locations");
                return Enumerable.Empty<StorageLocation>();
            }
        }

        public async Task<StorageLocation?> GetById(int id)
        {
            try
            {
                var query = _context.StorageLocations
                    .AsNoTracking()
                    .Include(l => l.IoTDevice)
                    .Include(l => l.Medicines).AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(l => l.OrganizationId == CurrentOrgId);
                return await query.FirstOrDefaultAsync(l => l.LocationId == id);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrieving storage location with ID {Id}", id);
                return null;
            }
        }

        public async Task<StorageLocation> Create(StorageLocation location)
        {
            try
            {
                var orgId = CurrentOrgId;
                if (!string.IsNullOrEmpty(orgId))
                {
                    location.OrganizationId = orgId;
                }
                _context.StorageLocations.Add(location);
                await _context.SaveChangesAsync();
                return location;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating storage location");
                throw;
            }
        }

        public async Task<StorageLocation?> Update(int id, StorageLocation updated)
        {
            try
            {
                var query = _context.StorageLocations.AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(l => l.OrganizationId == CurrentOrgId);
                var existing = await query.FirstOrDefaultAsync(l => l.LocationId == id);
                if (existing is null) return null;

                existing.Name = updated.Name;
                existing.Address = updated.Address;
                existing.LocationType = updated.LocationType;
                existing.IoTDeviceId = updated.IoTDeviceId;

                await _context.SaveChangesAsync();
                return existing;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error updating storage location with ID {Id}", id);
                throw;
            }
        }

        public async Task<bool> Delete(int id)
        {
            try
            {
                var query = _context.StorageLocations.AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(l => l.OrganizationId == CurrentOrgId);
                var location = await query.FirstOrDefaultAsync(l => l.LocationId == id);
                if (location is null) return false;

                _context.StorageLocations.Remove(location);
                await _context.SaveChangesAsync();
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error deleting storage location with ID {Id}", id);
                throw;
            }
        }
    }
}

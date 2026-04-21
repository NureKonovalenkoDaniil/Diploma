using MedicationManagement.DBContext;
using MedicationManagement.Enums;
using MedicationManagement.Models;
using Microsoft.EntityFrameworkCore;

namespace MedicationManagement.Services
{
    public interface IServiceStorageIncident
    {
        Task<IEnumerable<StorageIncident>> GetAll();
        Task<IEnumerable<StorageIncident>> GetActive();
        Task<StorageIncident?> GetById(int id);
        Task<StorageIncident> Create(StorageIncident incident);
        Task<StorageIncident?> Resolve(int id);
    }

    public class ServiceStorageIncident : IServiceStorageIncident
    {
        private readonly MedicineStorageContext _context;
        private readonly ILogger<ServiceStorageIncident> _logger;

        public ServiceStorageIncident(MedicineStorageContext context, ILogger<ServiceStorageIncident> logger)
        {
            _context = context;
            _logger = logger;
        }

        public async Task<IEnumerable<StorageIncident>> GetAll()
        {
            try
            {
                return await _context.StorageIncidents
                    .AsNoTracking()
                    .Include(i => i.IoTDevice)
                    .Include(i => i.StorageLocation)
                    .OrderByDescending(i => i.CreatedAt)
                    .ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrieving storage incidents");
                return Enumerable.Empty<StorageIncident>();
            }
        }

        public async Task<IEnumerable<StorageIncident>> GetActive()
        {
            try
            {
                return await _context.StorageIncidents
                    .AsNoTracking()
                    .Include(i => i.IoTDevice)
                    .Include(i => i.StorageLocation)
                    .Where(i => i.Status == IncidentStatus.Active)
                    .OrderByDescending(i => i.StartTime)
                    .ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrieving active storage incidents");
                return Enumerable.Empty<StorageIncident>();
            }
        }

        public async Task<StorageIncident?> GetById(int id)
        {
            try
            {
                return await _context.StorageIncidents
                    .AsNoTracking()
                    .Include(i => i.IoTDevice)
                    .Include(i => i.StorageLocation)
                    .FirstOrDefaultAsync(i => i.IncidentId == id);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrieving incident with ID {Id}", id);
                return null;
            }
        }

        public async Task<StorageIncident> Create(StorageIncident incident)
        {
            try
            {
                incident.CreatedAt = DateTime.UtcNow;
                incident.StartTime = DateTime.UtcNow;
                incident.Status = IncidentStatus.Active;

                _context.StorageIncidents.Add(incident);
                await _context.SaveChangesAsync();
                return incident;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating storage incident");
                throw;
            }
        }

        public async Task<StorageIncident?> Resolve(int id)
        {
            try
            {
                var incident = await _context.StorageIncidents.FindAsync(id);
                if (incident is null) return null;

                incident.Status = IncidentStatus.Resolved;
                incident.EndTime = DateTime.UtcNow;

                await _context.SaveChangesAsync();
                return incident;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error resolving incident with ID {Id}", id);
                throw;
            }
        }
    }
}

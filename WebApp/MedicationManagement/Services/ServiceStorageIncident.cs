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

        public ServiceStorageIncident(MedicineStorageContext context)
        {
            _context = context;
        }

        public async Task<IEnumerable<StorageIncident>> GetAll()
        {
            return await _context.StorageIncidents
                .Include(i => i.IoTDevice)
                .Include(i => i.StorageLocation)
                .OrderByDescending(i => i.CreatedAt)
                .ToListAsync();
        }

        public async Task<IEnumerable<StorageIncident>> GetActive()
        {
            return await _context.StorageIncidents
                .Include(i => i.IoTDevice)
                .Include(i => i.StorageLocation)
                .Where(i => i.Status == IncidentStatus.Active)
                .OrderByDescending(i => i.StartTime)
                .ToListAsync();
        }

        public async Task<StorageIncident?> GetById(int id)
        {
            return await _context.StorageIncidents
                .Include(i => i.IoTDevice)
                .Include(i => i.StorageLocation)
                .FirstOrDefaultAsync(i => i.IncidentId == id);
        }

        public async Task<StorageIncident> Create(StorageIncident incident)
        {
            incident.CreatedAt = DateTime.UtcNow;
            incident.StartTime = DateTime.UtcNow;
            incident.Status = IncidentStatus.Active;

            _context.StorageIncidents.Add(incident);
            await _context.SaveChangesAsync();
            return incident;
        }

        public async Task<StorageIncident?> Resolve(int id)
        {
            var incident = await _context.StorageIncidents.FindAsync(id);
            if (incident is null) return null;

            incident.Status = IncidentStatus.Resolved;
            incident.EndTime = DateTime.UtcNow;

            await _context.SaveChangesAsync();
            return incident;
        }
    }
}

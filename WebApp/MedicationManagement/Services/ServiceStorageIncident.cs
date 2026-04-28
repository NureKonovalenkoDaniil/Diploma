using MedicationManagement.DBContext;
using MedicationManagement.Enums;
using MedicationManagement.Models;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Extensions;

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
        private readonly IHttpContextAccessor _httpContextAccessor;

        public ServiceStorageIncident(MedicineStorageContext context, ILogger<ServiceStorageIncident> logger, IHttpContextAccessor httpContextAccessor)
        {
            _context = context;
            _logger = logger;
            _httpContextAccessor = httpContextAccessor;
        }

        private string? CurrentOrgId => _httpContextAccessor.HttpContext?.User.GetOrganizationId();
        private bool IsAdmin => _httpContextAccessor.HttpContext?.User.IsInRole("Administrator") ?? true;

        public async Task<IEnumerable<StorageIncident>> GetAll()
        {
            try
            {
                var query = _context.StorageIncidents
                    .AsNoTracking()
                    .Include(i => i.IoTDevice)
                    .Include(i => i.StorageLocation).AsQueryable();

                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(i => i.OrganizationId == CurrentOrgId);

                return await query.OrderByDescending(i => i.CreatedAt)
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
                var query = _context.StorageIncidents
                    .AsNoTracking()
                    .Include(i => i.IoTDevice)
                    .Include(i => i.StorageLocation)
                    .Where(i => i.Status == IncidentStatus.Active).AsQueryable();

                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(i => i.OrganizationId == CurrentOrgId);

                return await query.OrderByDescending(i => i.StartTime)
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
                var query = _context.StorageIncidents
                    .AsNoTracking()
                    .Include(i => i.IoTDevice)
                    .Include(i => i.StorageLocation).AsQueryable();

                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(i => i.OrganizationId == CurrentOrgId);

                return await query.FirstOrDefaultAsync(i => i.IncidentId == id);
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

                var orgId = CurrentOrgId;
                if (!string.IsNullOrEmpty(orgId))
                {
                    incident.OrganizationId = orgId;
                }

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
                var query = _context.StorageIncidents.AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(i => i.OrganizationId == CurrentOrgId);

                var incident = await query.FirstOrDefaultAsync(i => i.IncidentId == id);
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

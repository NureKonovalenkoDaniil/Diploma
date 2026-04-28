using MedicationManagement.DBContext;
using MedicationManagement.Models;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Extensions;

namespace MedicationManagement.Services
{
    public interface IServiceMedicineLifecycle
    {
        Task<IEnumerable<MedicineLifecycleEvent>> GetByMedicineId(int medicineId);
        Task<IEnumerable<MedicineLifecycleEvent>> GetAll();
        Task<MedicineLifecycleEvent?> GetById(int id);
        Task<MedicineLifecycleEvent> AddEvent(MedicineLifecycleEvent lifecycleEvent);
    }

    public class ServiceMedicineLifecycle : IServiceMedicineLifecycle
    {
        private readonly MedicineStorageContext _context;
        private readonly ILogger<ServiceMedicineLifecycle> _logger;
        private readonly IHttpContextAccessor _httpContextAccessor;

        public ServiceMedicineLifecycle(MedicineStorageContext context, ILogger<ServiceMedicineLifecycle> logger, IHttpContextAccessor httpContextAccessor)
        {
            _context = context;
            _logger = logger;
            _httpContextAccessor = httpContextAccessor;
        }

        private string? CurrentOrgId => _httpContextAccessor.HttpContext?.User.GetOrganizationId();
        private bool IsAdmin => _httpContextAccessor.HttpContext?.User.IsInRole("Administrator") ?? true;

        public async Task<IEnumerable<MedicineLifecycleEvent>> GetAll()
        {
            try
            {
                var query = _context.MedicineLifecycleEvents
                    .AsNoTracking()
                    .Include(e => e.Medicine)
                    .Include(e => e.RelatedLocation).AsQueryable();

                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(e => e.OrganizationId == CurrentOrgId);

                return await query.OrderByDescending(e => e.PerformedAt)
                    .ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrieving all lifecycle events");
                return Enumerable.Empty<MedicineLifecycleEvent>();
            }
        }

        public async Task<IEnumerable<MedicineLifecycleEvent>> GetByMedicineId(int medicineId)
        {
            try
            {
                var query = _context.MedicineLifecycleEvents
                    .AsNoTracking()
                    .Include(e => e.Medicine)
                    .Include(e => e.RelatedLocation)
                    .Where(e => e.MedicineId == medicineId).AsQueryable();

                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(e => e.OrganizationId == CurrentOrgId);

                return await query.OrderByDescending(e => e.PerformedAt)
                    .ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrieving lifecycle events for Medicine ID {Id}", medicineId);
                return Enumerable.Empty<MedicineLifecycleEvent>();
            }
        }

        public async Task<MedicineLifecycleEvent?> GetById(int id)
        {
            try
            {
                var query = _context.MedicineLifecycleEvents
                    .AsNoTracking()
                    .Include(e => e.Medicine)
                    .Include(e => e.RelatedLocation).AsQueryable();

                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(e => e.OrganizationId == CurrentOrgId);

                return await query.FirstOrDefaultAsync(e => e.EventId == id);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrieving lifecycle event with ID {Id}", id);
                return null;
            }
        }

        public async Task<MedicineLifecycleEvent> AddEvent(MedicineLifecycleEvent lifecycleEvent)
        {
            try
            {
                lifecycleEvent.PerformedAt = DateTime.UtcNow;

                var orgId = CurrentOrgId;
                if (!string.IsNullOrEmpty(orgId))
                {
                    lifecycleEvent.OrganizationId = orgId;
                }

                _context.MedicineLifecycleEvents.Add(lifecycleEvent);
                await _context.SaveChangesAsync();
                return lifecycleEvent;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error adding lifecycle event for Medicine ID {Id}", lifecycleEvent.MedicineId);
                throw;
            }
        }
    }
}

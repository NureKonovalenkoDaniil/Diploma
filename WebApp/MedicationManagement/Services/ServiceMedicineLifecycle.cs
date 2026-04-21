using MedicationManagement.DBContext;
using MedicationManagement.Models;
using Microsoft.EntityFrameworkCore;

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

        public ServiceMedicineLifecycle(MedicineStorageContext context, ILogger<ServiceMedicineLifecycle> logger)
        {
            _context = context;
            _logger = logger;
        }

        public async Task<IEnumerable<MedicineLifecycleEvent>> GetAll()
        {
            try
            {
                return await _context.MedicineLifecycleEvents
                    .AsNoTracking()
                    .Include(e => e.Medicine)
                    .Include(e => e.RelatedLocation)
                    .OrderByDescending(e => e.PerformedAt)
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
                return await _context.MedicineLifecycleEvents
                    .AsNoTracking()
                    .Include(e => e.Medicine)
                    .Include(e => e.RelatedLocation)
                    .Where(e => e.MedicineId == medicineId)
                    .OrderByDescending(e => e.PerformedAt)
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
                return await _context.MedicineLifecycleEvents
                    .AsNoTracking()
                    .Include(e => e.Medicine)
                    .Include(e => e.RelatedLocation)
                    .FirstOrDefaultAsync(e => e.EventId == id);
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

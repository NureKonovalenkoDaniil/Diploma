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

        public ServiceMedicineLifecycle(MedicineStorageContext context)
        {
            _context = context;
        }

        public async Task<IEnumerable<MedicineLifecycleEvent>> GetAll()
        {
            return await _context.MedicineLifecycleEvents
                .Include(e => e.Medicine)
                .Include(e => e.RelatedLocation)
                .OrderByDescending(e => e.PerformedAt)
                .ToListAsync();
        }

        public async Task<IEnumerable<MedicineLifecycleEvent>> GetByMedicineId(int medicineId)
        {
            return await _context.MedicineLifecycleEvents
                .Include(e => e.Medicine)
                .Include(e => e.RelatedLocation)
                .Where(e => e.MedicineId == medicineId)
                .OrderByDescending(e => e.PerformedAt)
                .ToListAsync();
        }

        public async Task<MedicineLifecycleEvent?> GetById(int id)
        {
            return await _context.MedicineLifecycleEvents
                .Include(e => e.Medicine)
                .Include(e => e.RelatedLocation)
                .FirstOrDefaultAsync(e => e.EventId == id);
        }

        public async Task<MedicineLifecycleEvent> AddEvent(MedicineLifecycleEvent lifecycleEvent)
        {
            lifecycleEvent.PerformedAt = DateTime.UtcNow;
            _context.MedicineLifecycleEvents.Add(lifecycleEvent);
            await _context.SaveChangesAsync();
            return lifecycleEvent;
        }
    }
}

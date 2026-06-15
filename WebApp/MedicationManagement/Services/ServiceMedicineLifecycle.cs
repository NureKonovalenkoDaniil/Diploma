using MedicationManagement.DBContext;
using MedicationManagement.Models;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Extensions;
using MedicationManagement.Enums;

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

                await using var tx = await _context.Database.BeginTransactionAsync();

                var medicine = await _context.Medicines.FirstOrDefaultAsync(m => m.MedicineID == lifecycleEvent.MedicineId);
                if (medicine != null)
                {
                    switch (lifecycleEvent.EventType)
                    {
                        case LifecycleEventType.Received:
                            if (lifecycleEvent.Quantity.HasValue && lifecycleEvent.Quantity.Value > 0)
                            {
                                medicine.Quantity += lifecycleEvent.Quantity.Value;
                            }
                            if (medicine.Status == MedicineStatus.Disposed && medicine.Quantity > 0)
                            {
                                medicine.Status = medicine.ExpiryDate <= DateTime.UtcNow 
                                    ? MedicineStatus.Expired 
                                    : MedicineStatus.Active;
                            }
                            break;

                        case LifecycleEventType.Issued:
                            if (lifecycleEvent.Quantity.HasValue && lifecycleEvent.Quantity.Value > 0)
                            {
                                if (medicine.Quantity < lifecycleEvent.Quantity.Value)
                                {
                                    throw new InvalidOperationException($"Недостатньо запасів для видачі. Доступно: {medicine.Quantity}");
                                }
                                medicine.Quantity -= lifecycleEvent.Quantity.Value;
                            }
                            break;

                        case LifecycleEventType.Disposed:
                            var qtyToDispose = lifecycleEvent.Quantity ?? medicine.Quantity;
                            if (qtyToDispose > 0)
                            {
                                if (medicine.Quantity < qtyToDispose)
                                {
                                    throw new InvalidOperationException($"Недостатньо запасів для утилізації. Доступно: {medicine.Quantity}");
                                }
                                medicine.Quantity -= qtyToDispose;
                            }
                            else
                            {
                                medicine.Quantity = 0;
                            }

                            if (medicine.Quantity == 0)
                            {
                                medicine.Status = MedicineStatus.Disposed;
                            }
                            break;

                        case LifecycleEventType.Moved:
                            if (lifecycleEvent.RelatedLocationId.HasValue)
                            {
                                medicine.StorageLocationId = lifecycleEvent.RelatedLocationId.Value;
                            }
                            break;

                        case LifecycleEventType.Expired:
                            medicine.Status = MedicineStatus.Expired;
                            break;

                        case LifecycleEventType.Recalled:
                            medicine.Status = MedicineStatus.Recalled;
                            break;
                    }
                }

                _context.MedicineLifecycleEvents.Add(lifecycleEvent);
                await _context.SaveChangesAsync();

                await tx.CommitAsync();

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

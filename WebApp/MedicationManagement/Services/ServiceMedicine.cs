using MedicationManagement.DBContext;
using MedicationManagement.Models;
using Microsoft.AspNetCore.JsonPatch;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Extensions;
using MedicationManagement.Enums;

namespace MedicationManagement.Services
{
    // Interface for the medicine service
    public interface IServiceMedicine
    {
        Task<List<ReplenishmentRecommendation>> GetReplenishmentRecommendations();
        Task<IEnumerable<Medicine>> GetExpiringMedicines(DateTime thresholdDate);
        Task<List<Medicine>> GetLowStockMedicines(int threshold);
        Task<Medicine?> Create(Medicine medicine);
        Task<IEnumerable<Medicine>> Read();
        Task<Medicine?> ReadById(int id);
        Task<Medicine?> Update(int id, JsonPatchDocument<Medicine> patchDocument);
        Task<Medicine?> Move(int id, int storageLocationId, string performedBy, string? description = null, int? quantity = null);
        Task<(Medicine? medicine, string? error)> Receive(int id, int quantity, string performedBy, int? storageLocationId = null, string? description = null, int? relatedLocationId = null);
        Task<(Medicine? medicine, string? error)> Issue(int id, int quantity, string performedBy, string? description = null, int? relatedLocationId = null);
        Task<(Medicine? medicine, string? error)> Dispose(int id, int? quantity, string performedBy, string? description = null, int? relatedLocationId = null);
        Task<bool> Delete(int id);
    }
    // Implementation of the medicine service
    public class ServiceMedicine : IServiceMedicine
    {
        private readonly MedicineStorageContext _context;
        private readonly ILogger<ServiceMedicine> _logger;
        private readonly IConfiguration _configuration;
        private readonly IHttpContextAccessor _httpContextAccessor;

        public ServiceMedicine(MedicineStorageContext context, ILogger<ServiceMedicine> logger, IConfiguration configuration, IHttpContextAccessor httpContextAccessor)
        {
            _context = context;
            _logger = logger;
            _configuration = configuration;
            _httpContextAccessor = httpContextAccessor;
        }

        private string? CurrentOrgId => _httpContextAccessor.HttpContext?.User.GetOrganizationId();
        private bool IsAdmin => _httpContextAccessor.HttpContext?.User.IsInRole("Administrator") ?? true;

        // Method to get medicines with low stock
        public async Task<List<Medicine>> GetLowStockMedicines(int threshold)
        {
            try
            {
                var query = _context.Medicines.Include(m => m.StorageLocation).AsNoTracking().Where(m => m.Quantity < threshold);
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(m => m.OrganizationId == CurrentOrgId);
                return await query.ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error fetching low stock medicines");
                return new List<Medicine>();
            }
        }

        // Method to get medicines that are expiring before a certain date
        public async Task<IEnumerable<Medicine>> GetExpiringMedicines(DateTime thresholdDate)
        {
            try
            {
                var query = _context.Medicines.Include(m => m.StorageLocation).AsNoTracking().Where(m => m.ExpiryDate > DateTime.UtcNow && m.ExpiryDate <= thresholdDate);
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(m => m.OrganizationId == CurrentOrgId);
                return await query.ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error fetching expiring medicines");
                return Enumerable.Empty<Medicine>();
            }
        }

        // Method to get replenishment recommendations for low stock medicines
        public async Task<List<ReplenishmentRecommendation>> GetReplenishmentRecommendations()
        {
            try
            {
                var threshold = _configuration.GetValue<int>("Business:LowStockThreshold", 10);
                var replenishTo = _configuration.GetValue<int>("Business:ReplenishToQuantity", 100);

                var lowStockMedicines = await GetLowStockMedicines(threshold);
                return lowStockMedicines.Select(m => new ReplenishmentRecommendation
                {
                    MedicineId = m.MedicineID,
                    MedicineName = m.Name,
                    RecommendedQuantity = replenishTo - m.Quantity
                }).ToList();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error generating replenishment recommendations");
                return new List<ReplenishmentRecommendation>();
            }
        }

        // Method to create a new medicine
        public async Task<Medicine?> Create(Medicine medicine)
        {
            if (medicine == null)
            {
                _logger.LogWarning("Attempted to create a null medicine object");
                return null;
            }

            try
            {
                var orgId = CurrentOrgId;
                if (!string.IsNullOrEmpty(orgId))
                {
                    medicine.OrganizationId = orgId;
                }
                
                await _context.Medicines.AddAsync(medicine);
                await _context.SaveChangesAsync();
                return medicine;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating medicine");
                return null;
            }
        }

        // Method to read all medicines
        public async Task<IEnumerable<Medicine>> Read()
        {
            try
            {
                var query = _context.Medicines.Include(m => m.StorageLocation).AsNoTracking();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(m => m.OrganizationId == CurrentOrgId);
                return await query.ToListAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error reading medicines");
                return Enumerable.Empty<Medicine>();
            }
        }

        // Method to read a medicine by ID
        public async Task<Medicine?> ReadById(int id)
        {
            try
            {
                var query = _context.Medicines.Include(m => m.StorageLocation).AsNoTracking().Where(m => m.MedicineID == id);
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(m => m.OrganizationId == CurrentOrgId);
                var medicine = await query.FirstOrDefaultAsync();
                if (medicine == null)
                {
                    _logger.LogWarning($"Medicine with ID {id} not found");
                }
                return medicine;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error reading medicine by ID {id}");
                return null;
            }
        }

        // Method to update an existing medicine
        public async Task<Medicine?> Update(int id, JsonPatchDocument<Medicine> patchDocument)
        {
            if (patchDocument == null)
            {
                _logger.LogWarning("Patch document is null");
                return null;
            }

            try
            {
                var query = _context.Medicines.AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(m => m.OrganizationId == CurrentOrgId);
                    
                var medicineToUpdate = await query.FirstOrDefaultAsync(m => m.MedicineID == id);
                if (medicineToUpdate == null)
                {
                    _logger.LogWarning($"Medicine with ID {id} not found");
                    return null;
                }

                patchDocument.ApplyTo(medicineToUpdate);
                await _context.SaveChangesAsync();
                return medicineToUpdate;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error updating medicine with ID {id}");
                return null;
            }
        }

        public async Task<Medicine?> Move(int id, int storageLocationId, string performedBy, string? description = null, int? quantity = null)
        {
            try
            {
                // Read medicine + current location for a meaningful description.
                var medQuery = _context.Medicines
                    .Include(m => m.StorageLocation)
                    .AsQueryable();

                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    medQuery = medQuery.Where(m => m.OrganizationId == CurrentOrgId);

                var medicine = await medQuery.FirstOrDefaultAsync(m => m.MedicineID == id);
                if (medicine is null)
                {
                    _logger.LogWarning("Medicine with ID {Id} not found for move", id);
                    return null;
                }

                // Validate target location exists and belongs to the same tenant (for non-admin).
                var locQuery = _context.StorageLocations.AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    locQuery = locQuery.Where(l => l.OrganizationId == CurrentOrgId);

                var targetLocation = await locQuery.FirstOrDefaultAsync(l => l.LocationId == storageLocationId);
                if (targetLocation is null)
                {
                    _logger.LogWarning("Target StorageLocation with ID {Id} not found for move", storageLocationId);
                    return null;
                }

                var fromName = medicine.StorageLocation?.Name;
                var toName = targetLocation.Name;

                // Atomic update + lifecycle event.
                await using var tx = await _context.Database.BeginTransactionAsync();

                medicine.StorageLocationId = targetLocation.LocationId;
                await _context.SaveChangesAsync();

                var orgId = CurrentOrgId;
                if (string.IsNullOrEmpty(orgId))
                {
                    // For admin flows, keep the event in the same tenant as the medicine.
                    orgId = medicine.OrganizationId;
                }

                var evt = new MedicineLifecycleEvent
                {
                    MedicineId = medicine.MedicineID,
                    OrganizationId = orgId ?? string.Empty,
                    EventType = LifecycleEventType.Moved,
                    RelatedLocationId = targetLocation.LocationId,
                    Quantity = quantity,
                    PerformedBy = performedBy,
                    PerformedAt = DateTime.UtcNow,
                    Description = string.IsNullOrWhiteSpace(description)
                        ? $"Переміщення: {(string.IsNullOrWhiteSpace(fromName) ? "—" : fromName)} → {toName}"
                        : description
                };

                _context.MedicineLifecycleEvents.Add(evt);
                await _context.SaveChangesAsync();

                await tx.CommitAsync();

                return medicine;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error moving medicine with ID {Id} to location {LocationId}", id, storageLocationId);
                return null;
            }
        }

        public async Task<(Medicine? medicine, string? error)> Receive(
            int id, int quantity, string performedBy, int? storageLocationId = null, string? description = null, int? relatedLocationId = null)
        {
            if (quantity <= 0) return (null, "Quantity must be a positive integer");

            try
            {
                var query = _context.Medicines.Include(m => m.StorageLocation).AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(m => m.OrganizationId == CurrentOrgId);

                var medicine = await query.FirstOrDefaultAsync(m => m.MedicineID == id);
                if (medicine is null) return (null, "Medicine not found");

                StorageLocation? targetLocation = null;
                if (storageLocationId.HasValue)
                {
                    var locQuery = _context.StorageLocations.AsQueryable();
                    if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                        locQuery = locQuery.Where(l => l.OrganizationId == CurrentOrgId);

                    targetLocation = await locQuery.FirstOrDefaultAsync(l => l.LocationId == storageLocationId.Value);
                    if (targetLocation is null) return (null, "Target location not found");
                }

                await using var tx = await _context.Database.BeginTransactionAsync();

                medicine.Quantity += quantity;
                if (targetLocation is not null)
                {
                    medicine.StorageLocationId = targetLocation.LocationId;
                }

                await _context.SaveChangesAsync();

                var orgId = CurrentOrgId;
                if (string.IsNullOrEmpty(orgId)) orgId = medicine.OrganizationId;

                var evt = new MedicineLifecycleEvent
                {
                    MedicineId = medicine.MedicineID,
                    OrganizationId = orgId ?? string.Empty,
                    EventType = LifecycleEventType.Received,
                    Quantity = quantity,
                    PerformedBy = performedBy,
                    PerformedAt = DateTime.UtcNow,
                    RelatedLocationId = relatedLocationId ?? targetLocation?.LocationId ?? medicine.StorageLocationId,
                    Description = string.IsNullOrWhiteSpace(description)
                        ? $"Надходження: +{quantity}"
                        : description
                };

                _context.MedicineLifecycleEvents.Add(evt);
                await _context.SaveChangesAsync();

                await tx.CommitAsync();
                return (medicine, null);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error receiving stock for medicine ID {Id}", id);
                return (null, "Internal error");
            }
        }

        public async Task<(Medicine? medicine, string? error)> Issue(
            int id, int quantity, string performedBy, string? description = null, int? relatedLocationId = null)
        {
            if (quantity <= 0) return (null, "Quantity must be a positive integer");

            try
            {
                var query = _context.Medicines.Include(m => m.StorageLocation).AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(m => m.OrganizationId == CurrentOrgId);

                var medicine = await query.FirstOrDefaultAsync(m => m.MedicineID == id);
                if (medicine is null) return (null, "Medicine not found");

                if (medicine.Quantity < quantity)
                    return (null, $"Insufficient stock. Available: {medicine.Quantity}");

                await using var tx = await _context.Database.BeginTransactionAsync();

                medicine.Quantity -= quantity;
                await _context.SaveChangesAsync();

                var orgId = CurrentOrgId;
                if (string.IsNullOrEmpty(orgId)) orgId = medicine.OrganizationId;

                var evt = new MedicineLifecycleEvent
                {
                    MedicineId = medicine.MedicineID,
                    OrganizationId = orgId ?? string.Empty,
                    EventType = LifecycleEventType.Issued,
                    Quantity = quantity,
                    PerformedBy = performedBy,
                    PerformedAt = DateTime.UtcNow,
                    RelatedLocationId = relatedLocationId ?? medicine.StorageLocationId,
                    Description = string.IsNullOrWhiteSpace(description)
                        ? $"Видача: -{quantity}"
                        : description
                };

                _context.MedicineLifecycleEvents.Add(evt);
                await _context.SaveChangesAsync();

                await tx.CommitAsync();
                return (medicine, null);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error issuing stock for medicine ID {Id}", id);
                return (null, "Internal error");
            }
        }

        public async Task<(Medicine? medicine, string? error)> Dispose(
            int id, int? quantity, string performedBy, string? description = null, int? relatedLocationId = null)
        {
            try
            {
                var query = _context.Medicines.Include(m => m.StorageLocation).AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(m => m.OrganizationId == CurrentOrgId);

                var medicine = await query.FirstOrDefaultAsync(m => m.MedicineID == id);
                if (medicine is null) return (null, "Medicine not found");

                var toDispose = quantity ?? medicine.Quantity;
                if (toDispose <= 0) return (null, "Quantity must be a positive integer");
                if (medicine.Quantity < toDispose)
                    return (null, $"Insufficient stock. Available: {medicine.Quantity}");

                await using var tx = await _context.Database.BeginTransactionAsync();

                medicine.Quantity -= toDispose;
                await _context.SaveChangesAsync();

                var orgId = CurrentOrgId;
                if (string.IsNullOrEmpty(orgId)) orgId = medicine.OrganizationId;

                var evt = new MedicineLifecycleEvent
                {
                    MedicineId = medicine.MedicineID,
                    OrganizationId = orgId ?? string.Empty,
                    EventType = LifecycleEventType.Disposed,
                    Quantity = toDispose,
                    PerformedBy = performedBy,
                    PerformedAt = DateTime.UtcNow,
                    RelatedLocationId = relatedLocationId ?? medicine.StorageLocationId,
                    Description = string.IsNullOrWhiteSpace(description)
                        ? $"Утилізація: -{toDispose}"
                        : description
                };

                _context.MedicineLifecycleEvents.Add(evt);
                await _context.SaveChangesAsync();

                await tx.CommitAsync();
                return (medicine, null);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error disposing stock for medicine ID {Id}", id);
                return (null, "Internal error");
            }
        }

        // Method to delete a medicine by ID
        public async Task<bool> Delete(int id)
        {
            try
            {
                var query = _context.Medicines.AsQueryable();
                if (!IsAdmin && !string.IsNullOrEmpty(CurrentOrgId))
                    query = query.Where(m => m.OrganizationId == CurrentOrgId);
                    
                var medicine = await query.FirstOrDefaultAsync(m => m.MedicineID == id);
                if (medicine == null)
                {
                    _logger.LogWarning($"Medicine with ID {id} not found");
                    return false;
                }

                _context.Medicines.Remove(medicine);
                await _context.SaveChangesAsync();
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error deleting medicine with ID {id}");
                return false;
            }
        }
    }
}

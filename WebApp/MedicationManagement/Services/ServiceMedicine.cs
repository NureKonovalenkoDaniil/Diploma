using MedicationManagement.DBContext;
using MedicationManagement.Models;
using Microsoft.AspNetCore.JsonPatch;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Extensions;

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

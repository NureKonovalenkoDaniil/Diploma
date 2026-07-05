using MedicationManagement.Models;
using Microsoft.EntityFrameworkCore;
using MedicationManagement.Enums;

namespace MedicationManagement.DBContext
{
    public class MedicineStorageContext : DbContext
    {
        // --- Існуючі DbSet ---
        public DbSet<StorageCondition> StorageConditions { get; set; }
        public DbSet<IoTDevice> IoTDevices { get; set; }
        public DbSet<Medicine> Medicines { get; set; }
        public DbSet<AuditLog> AuditLogs { get; set; }

        // --- Нові DbSet (Фаза 2) ---
        public DbSet<StorageLocation> StorageLocations { get; set; }
        public DbSet<StorageIncident> StorageIncidents { get; set; }
        public DbSet<MedicineLifecycleEvent> MedicineLifecycleEvents { get; set; }
        public DbSet<Notification> Notifications { get; set; }
        public DbSet<Organization> Organizations { get; set; }

        public MedicineStorageContext(DbContextOptions<MedicineStorageContext> options) : base(options)
        {
        }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);

            // --- StorageLocation ---
            modelBuilder.Entity<StorageLocation>(entity =>
            {
                entity.HasKey(e => e.LocationId);

                        entity.HasIndex(e => e.OrganizationId);

                // Enum зберігається як рядок у БД (читабельніше)
                entity.Property(e => e.LocationType)
                      .HasConversion<string>()
                      .HasMaxLength(50);

                // Зв'язок StorageLocation → IoTDevice (nullable 1:1)
                entity.HasOne(e => e.IoTDevice)
                      .WithMany()
                      .HasForeignKey(e => e.IoTDeviceId)
                      .OnDelete(DeleteBehavior.SetNull);
            });

            // --- Medicine (оновлений) ---
            modelBuilder.Entity<Medicine>(entity =>
            {
                entity.Property(e => e.Status)
                      .HasConversion<string>()
                      .HasMaxLength(20);

                        entity.HasIndex(e => new { e.OrganizationId, e.ExpiryDate });
                        entity.HasIndex(e => new { e.OrganizationId, e.Status });

                // Зв'язок Medicine → StorageLocation (nullable N:1)
                entity.HasOne(e => e.StorageLocation)
                      .WithMany(l => l.Medicines)
                      .HasForeignKey(e => e.StorageLocationId)
                      .OnDelete(DeleteBehavior.SetNull);
            });

            // --- AuditLog (оновлений) ---
            modelBuilder.Entity<AuditLog>(entity =>
            {
                entity.Property(e => e.Severity)
                      .HasConversion<string>()
                      .HasMaxLength(20);

                entity.HasIndex(e => new { e.OrganizationId, e.Timestamp })
                      .IsDescending(false, true);
                entity.HasIndex(e => new { e.OrganizationId, e.Severity });
            });

            // --- StorageIncident ---
            modelBuilder.Entity<StorageIncident>(entity =>
            {
                entity.HasKey(e => e.IncidentId);

                entity.Property(e => e.IncidentType)
                      .HasConversion<string>()
                      .HasMaxLength(50);

                entity.Property(e => e.Status)
                      .HasConversion<string>()
                      .HasMaxLength(20);

                entity.HasIndex(e => new { e.OrganizationId, e.Status });
                entity.HasIndex(e => new { e.DeviceId, e.OrganizationId });
                entity.HasIndex(e => new { e.OrganizationId, e.CreatedAt })
                      .IsDescending(false, true);

                // StorageIncident → IoTDevice (required N:1)
                entity.HasOne(e => e.IoTDevice)
                      .WithMany()
                      .HasForeignKey(e => e.DeviceId)
                      .OnDelete(DeleteBehavior.Restrict);

                // StorageIncident → StorageLocation (nullable N:1)
                entity.HasOne(e => e.StorageLocation)
                      .WithMany()
                      .HasForeignKey(e => e.LocationId)
                      .OnDelete(DeleteBehavior.SetNull);
            });

            // --- MedicineLifecycleEvent ---
            modelBuilder.Entity<MedicineLifecycleEvent>(entity =>
            {
                entity.HasKey(e => e.EventId);

                entity.Property(e => e.EventType)
                      .HasConversion<string>()
                      .HasMaxLength(50);

                entity.HasIndex(e => e.OrganizationId);
                entity.HasIndex(e => new { e.MedicineId, e.OrganizationId });

                // MedicineLifecycleEvent → Medicine (required N:1)
                entity.HasOne(e => e.Medicine)
                      .WithMany()
                      .HasForeignKey(e => e.MedicineId)
                      .OnDelete(DeleteBehavior.Cascade);

                // MedicineLifecycleEvent → StorageLocation (nullable N:1)
                entity.HasOne(e => e.RelatedLocation)
                      .WithMany()
                      .HasForeignKey(e => e.RelatedLocationId)
                      .OnDelete(DeleteBehavior.SetNull);
            });

            // --- Notification ---
            modelBuilder.Entity<Notification>(entity =>
            {
                entity.HasKey(e => e.NotificationId);

                entity.Property(e => e.Type)
                      .HasConversion<string>()
                      .HasMaxLength(50);

                        entity.HasIndex(e => new { e.OrganizationId, e.CreatedAt })
                                .IsDescending(false, true);
                        entity.HasIndex(e => new { e.OrganizationId, e.IsRead });
                        entity.HasIndex(e => e.TargetRole);
            });

                  // --- IoTDevice ---
                  modelBuilder.Entity<IoTDevice>(entity =>
                  {
                        entity.HasIndex(e => new { e.OrganizationId, e.IsActive });
                  });

                  // --- StorageCondition ---
                  modelBuilder.Entity<StorageCondition>(entity =>
                  {
                        entity.HasIndex(e => new { e.OrganizationId, e.Timestamp })
                                .IsDescending(false, true);
                        entity.HasIndex(e => new { e.DeviceID, e.Timestamp })
                                .IsDescending(false, true);
                  });
        }
    }
}


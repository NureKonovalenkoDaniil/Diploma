using MedicationManagement.Models;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;

namespace MedicationManagement.DBContext
{
    public class UserContext : IdentityDbContext<ApplicationUser>
    {
        public DbSet<Organization> Organizations { get; set; }

        public UserContext(DbContextOptions<UserContext> options) : base(options)
        {
        }

        protected override void OnModelCreating(ModelBuilder builder)
        {
            base.OnModelCreating(builder);

            // Ignore Identity properties that are intentionally removed from the schema
            builder.Entity<ApplicationUser>(b =>
            {
                b.Ignore(u => u.PhoneNumber);
                b.Ignore(u => u.PhoneNumberConfirmed);
                b.Ignore(u => u.TwoFactorEnabled);
                b.Ignore(u => u.LockoutEnd);
                b.Ignore(u => u.LockoutEnabled);
                b.Ignore(u => u.AccessFailedCount);
            });
        }
    }
}

using MedicationManagement.DBContext;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;

namespace MedicationManagement.IntegrationTests;

/// <summary>
/// Кастомна WebApplicationFactory, яка замінює реальні SQL Server бази даних
/// на InMemory варіанти для ізольованого тестування без зовнішньої інфраструктури.
/// </summary>
public class TestWebApplicationFactory : WebApplicationFactory<Program>
{
    // Окремі підключення для кожного контексту
    private Microsoft.Data.Sqlite.SqliteConnection? _mainConnection;
    private Microsoft.Data.Sqlite.SqliteConnection? _usersConnection;

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.ConfigureServices(services =>
        {
            // Видаляємо реальні SQL Server реєстрації
            services.RemoveAll<DbContextOptions<MedicineStorageContext>>();
            services.RemoveAll<DbContextOptions<UserContext>>();

            // Створюємо окремі SQLite InMemory підключення
            _mainConnection = new Microsoft.Data.Sqlite.SqliteConnection("DataSource=:memory:");
            _mainConnection.Open();

            _usersConnection = new Microsoft.Data.Sqlite.SqliteConnection("DataSource=:memory:");
            _usersConnection.Open();

            services.AddDbContext<MedicineStorageContext>(options =>
            {
                options.UseSqlite(_mainConnection);
            });

            services.AddDbContext<UserContext>(options =>
            {
                options.UseSqlite(_usersConnection);
            });

            // Запускаємо міграції (створення таблиць) для SQLite БД
            var sp = services.BuildServiceProvider();
            using (var scope = sp.CreateScope())
            {
                var scopedServices = scope.ServiceProvider;
                var dbMain = scopedServices.GetRequiredService<MedicineStorageContext>();
                var dbUsers = scopedServices.GetRequiredService<UserContext>();
                
                dbMain.Database.EnsureCreated();
                dbUsers.Database.EnsureCreated();
            }
        });

        // Перевизначаємо конфігурацію для тестів
        builder.UseSetting("Jwt:Key", "TestSuperSecretKey_ForIntegrationTests_32chars!");
        builder.UseSetting("Jwt:Issuer", "http://localhost:5001");
        builder.UseSetting("Jwt:Audience", "http://localhost:5001");
        builder.UseSetting("Jwt:ExpireDays", "1");
        builder.UseSetting("AdminSeeding:Email", "testadmin@test.com");
        builder.UseSetting("AdminSeeding:Password", "Test1234!");
        builder.UseSetting("Swagger:Enabled", "false");
        builder.UseSetting("Monitoring:IntervalSeconds", "3600"); // Не запускати під час тестів
        builder.UseSetting("Email:Host", "localhost"); // Заглушка для email

        builder.UseEnvironment("Testing");
    }
}

using MedicationManagement.DBContext;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;

namespace MedicationManagement.IntegrationTests;

/// <summary>
/// Кастомна WebApplicationFactory, яка замінює реальні SQL Server бази даних
/// на InMemory варіанти для ізольованого тестування без зовнішньої інфраструктури.
/// </summary>
public class TestWebApplicationFactory : WebApplicationFactory<Program>
{
    // Унікальне ім'я БД для кожного екземпляру фабрики
    private readonly string _dbName = Guid.NewGuid().ToString();

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.ConfigureServices(services =>
        {
            // Видаляємо реальні SQL Server реєстрації
            services.RemoveAll<DbContextOptions<MedicineStorageContext>>();
            services.RemoveAll<DbContextOptions<UserContext>>();

            // Реєструємо InMemory бази даних
            services.AddDbContext<MedicineStorageContext>(options =>
                options.UseInMemoryDatabase(_dbName + "_main"));

            services.AddDbContext<UserContext>(options =>
                options.UseInMemoryDatabase(_dbName + "_users"));
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

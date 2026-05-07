using System.Security.Claims;
using MedicationManagement.DBContext;
using MedicationManagement.Enums;
using MedicationManagement.Models;
using MedicationManagement.Services;
using Microsoft.AspNetCore.Http;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Moq;

namespace MedicationManagement.UnitTests;

/// <summary>
/// Базовий клас, що надає спільну логіку для тестів ServiceMedicine.
/// Використовує SQLite InMemory (замість EF InMemory), оскільки ServiceMedicine
/// використовує транзакції (BeginTransactionAsync), які EF InMemory не підтримує.
/// </summary>
public abstract class ServiceMedicineTestBase : IDisposable
{
    protected MedicineStorageContext Context { get; }
    protected IHttpContextAccessor HttpContextAccessor { get; }
    private readonly Mock<IHttpContextAccessor> _httpContextAccessorMock;
    private readonly SqliteConnection _connection;

    protected ServiceMedicineTestBase(string orgId = "org-1")
    {
        // SQLite InMemory: підтримує транзакції на відміну від EF InMemory провайдера
        _connection = new SqliteConnection("DataSource=:memory:");
        _connection.Open();

        var options = new DbContextOptionsBuilder<MedicineStorageContext>()
            .UseSqlite(_connection)
            .Options;

        Context = new MedicineStorageContext(options);
        Context.Database.EnsureCreated(); // Створюємо схему

        _httpContextAccessorMock = new Mock<IHttpContextAccessor>();
        SetCurrentUser(orgId, "Manager");
        HttpContextAccessor = _httpContextAccessorMock.Object;
    }

    /// <summary>
    /// Дозволяє змінити поточного користувача посеред тесту (для перевірки Multi-tenancy).
    /// </summary>
    protected void SetCurrentUser(string orgId, string role)
    {
        var claims = new[]
        {
            new Claim("OrganizationId", orgId),
            new Claim(ClaimTypes.Role, role)
        };

        var identity = new ClaimsIdentity(claims, "TestAuthType");
        var principal = new ClaimsPrincipal(identity);
        var httpContext = new DefaultHttpContext { User = principal };

        _httpContextAccessorMock.Setup(x => x.HttpContext).Returns(httpContext);
    }

    protected ServiceMedicine CreateService()
    {
        var configMock = new Mock<IConfiguration>();
        configMock.Setup(c => c["Business:LowStockThreshold"]).Returns("10");
        configMock.Setup(c => c["Business:ReplenishToQuantity"]).Returns("100");

        return new ServiceMedicine(
            Context,
            NullLogger<ServiceMedicine>.Instance,
            configMock.Object,
            HttpContextAccessor
        );
    }

    public void Dispose()
    {
        Context.Dispose();
        _connection.Dispose();
    }
}

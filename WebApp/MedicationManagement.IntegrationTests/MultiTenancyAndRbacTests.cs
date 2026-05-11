using System.IdentityModel.Tokens.Jwt;
using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Security.Claims;
using System.Text;
using FluentAssertions;
using Microsoft.IdentityModel.Tokens;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace MedicationManagement.IntegrationTests;

/// <summary>
/// Інтеграційні тести для перевірки Multi-tenancy та RBAC (Role-Based Access Control).
///
/// Це КЛЮЧОВІ тести для дипломної роботи — вони доводять комісії,
/// що архітектура ізоляції даних між організаціями реалізована коректно.
///
/// Сценарій: Два окремі "тенанти" (org-A та org-B) не повинні бачити дані один одного.
/// </summary>
public class MultiTenancyAndRbacTests : IClassFixture<TestWebApplicationFactory>
{
    private readonly TestWebApplicationFactory _factory;
    private readonly HttpClient _client;

    // JWT конфігурація синхронізована з TestWebApplicationFactory
    private const string JwtKey = "TestSuperSecretKey_ForIntegrationTests_32chars!";
    private const string JwtIssuer = "http://localhost:5001";
    private const string JwtAudience = "http://localhost:5001";

    public MultiTenancyAndRbacTests(TestWebApplicationFactory factory)
    {
        _factory = factory;
        _client = factory.CreateClient();
    }

    /// <summary>
    /// Генерує валідний тестовий JWT токен для заданої організації та ролі.
    /// Це дозволяє нам тестувати Multi-tenancy без реального процесу логіну.
    /// </summary>
    private string GenerateTestToken(string orgId, string role, string email = "test@test.com")
    {
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(JwtKey));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, Guid.NewGuid().ToString()),
            new Claim(ClaimTypes.Name, email),
            new Claim(ClaimTypes.Email, email),
            new Claim(ClaimTypes.Role, role),
            new Claim("OrganizationId", orgId)
        };

        var token = new JwtSecurityToken(
            issuer: JwtIssuer,
            audience: JwtAudience,
            claims: claims,
            expires: DateTime.UtcNow.AddHours(1),
            signingCredentials: credentials
        );

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    private HttpClient CreateClientWithToken(string orgId, string role)
    {
        var client = _factory.CreateClient();
        var token = GenerateTestToken(orgId, role);
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        return client;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MULTI-TENANCY: ІЗОЛЯЦІЯ ДАНИХ
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task MultiTenancy_UserA_CannotSee_UserB_Medicines()
    {
        // Arrange
        var clientOrgA = CreateClientWithToken("org-isolation-A", "Manager");
        var clientOrgB = CreateClientWithToken("org-isolation-B", "Manager");

        // Org-A створює препарат
        var medicinePayload = new
        {
            name = "Унікальний препарат Org-A",
            type = "Таблетки",
            category = "Тест",
            quantity = 10,
            expiryDate = DateTime.UtcNow.AddYears(1)
        };

        var createResponse = await clientOrgA.PostAsJsonAsync("/api/medicine", medicinePayload);
        createResponse.StatusCode.Should().Be(HttpStatusCode.OK,
            "org-A має право створювати препарати");

        // Act: Org-B намагається отримати список препаратів
        var getResponse = await clientOrgB.GetAsync("/api/medicine");

        // Assert
        getResponse.StatusCode.Should().Be(HttpStatusCode.OK);
        var json = await getResponse.Content.ReadAsStringAsync();
        var medicines = JArray.Parse(json);

        medicines.Should().NotContain(
            m => (string?)m["name"] == "Унікальний препарат Org-A",
            "Org-B не повинна бачити препарати Org-A — дані мають бути ізольовані по OrganizationId");
    }

    [Fact]
    public async Task MultiTenancy_UserA_CanSee_OwnMedicines()
    {
        // Arrange
        var uniqueName = $"Препарат Org-C_{Guid.NewGuid():N}";
        var clientOrgC = CreateClientWithToken("org-isolation-C", "Manager");

        var createPayload = new
        {
            name = uniqueName,
            type = "Капсули",
            category = "Вітаміни",
            quantity = 50,
            expiryDate = DateTime.UtcNow.AddYears(2)
        };

        await clientOrgC.PostAsJsonAsync("/api/medicine", createPayload);

        // Act
        var getResponse = await clientOrgC.GetAsync("/api/medicine");

        // Assert
        getResponse.StatusCode.Should().Be(HttpStatusCode.OK);
        var json = await getResponse.Content.ReadAsStringAsync();
        var medicines = JArray.Parse(json);

        medicines.Should().Contain(
            m => (string?)m["name"] == uniqueName,
            "Org-C повинна бачити власні препарати");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RBAC: ПЕРЕВІРКА РОЛЕЙ
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task RBAC_UserRole_CanCreateMedicine()
    {
        // Arrange: звичайний User (після нашого виправлення має доступ до POST /api/medicine)
        var clientUser = CreateClientWithToken("org-rbac-1", "User");

        var payload = new
        {
            name = "Тестовий препарат User",
            type = "Таблетки",
            category = "Антибіотики",
            quantity = 5,
            expiryDate = DateTime.UtcNow.AddYears(1)
        };

        // Act
        var response = await clientUser.PostAsJsonAsync("/api/medicine", payload);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK,
            "роль User має право створювати препарати (за нашою конфігурацією)");
    }

    [Fact]
    public async Task RBAC_GetUsers_Returns200ForManager()
    {
        // Arrange: менеджер отримує список користувачів (тепер дозволено)
        var clientManager = CreateClientWithToken("org-rbac-2", "Manager");

        // Act
        var response = await clientManager.GetAsync("/api/auth/users");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK,
            "ендпоінт GET /api/auth/users тепер доступний для Manager та User");
    }

    [Fact]
    public async Task RBAC_AdminOnly_GetUsers_Returns200ForAdmin()
    {
        // Arrange: адміністратор отримує список користувачів
        var clientAdmin = CreateClientWithToken("org-admin-rbac", "Administrator");

        // Act
        var response = await clientAdmin.GetAsync("/api/auth/users");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK,
            "адміністратор має доступ до GET /api/auth/users");
    }

    [Fact]
    public async Task RBAC_AnyAuthenticatedUser_CanGetMedicines()
    {
        // Arrange
        var clientUser = CreateClientWithToken("org-rbac-3", "User");

        // Act
        var response = await clientUser.GetAsync("/api/medicine");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK,
            "отримання списку препаратів доступне для будь-якого авторизованого користувача");
    }

    [Fact]
    public async Task RBAC_Unauthenticated_CannotAccessProtectedResources()
    {
        // Arrange: клієнт без токену
        var unauthClient = _factory.CreateClient();

        // Act & Assert: перевіряємо кілька захищених ендпоінтів
        var medicinesResponse = await unauthClient.GetAsync("/api/medicine");
        medicinesResponse.StatusCode.Should().Be(HttpStatusCode.Unauthorized);

        var devicesResponse = await unauthClient.GetAsync("/api/iotdevice");
        devicesResponse.StatusCode.Should().Be(HttpStatusCode.Unauthorized);

        var notificationsResponse = await unauthClient.GetAsync("/api/notification");
        notificationsResponse.StatusCode.Should().Be(HttpStatusCode.Unauthorized);
    }

    [Fact]
    public async Task RBAC_CreateStorageLocation_AllowedForUserRole()
    {
        // Arrange: звичайний User створює локацію (тепер дозволено)
        var clientUser = CreateClientWithToken("org-rbac-4", "User");

        var payload = new
        {
            name = "Нова локація від User",
            address = "вул. Тестова, 1",
            locationType = "Pharmacy"
        };

        // Act
        var response = await clientUser.PostAsJsonAsync("/api/storagelocation", payload);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Created,
            "тепер Administrator, Manager та User можуть створювати локації зберігання");
    }
}

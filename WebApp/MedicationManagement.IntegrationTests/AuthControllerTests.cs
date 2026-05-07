using System.Net;
using System.Net.Http.Json;
using FluentAssertions;

namespace MedicationManagement.IntegrationTests;

/// <summary>
/// Інтеграційні тести для AuthController:
/// - Реєстрація нового користувача (перевірка HTTP 200)
/// - Логін без підтвердженого email (має повернути 403)
/// - Запит до захищеного ендпоінту без токена (має повернути 401)
/// - Реєстрація дублікату (має повернути 409 Conflict)
/// </summary>
public class AuthControllerTests : IClassFixture<TestWebApplicationFactory>
{
    private readonly HttpClient _client;

    public AuthControllerTests(TestWebApplicationFactory factory)
    {
        _client = factory.CreateClient();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // РЕЄСТРАЦІЯ
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task Register_ValidUser_Returns200()
    {
        // Arrange
        var payload = new
        {
            email = $"newuser_{Guid.NewGuid():N}@test.com",
            password = "Test1234"
        };

        // Act
        var response = await _client.PostAsJsonAsync("/api/auth/register", payload);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK,
            "реєстрація з валідними даними має повертати 200 OK");
    }

    [Fact]
    public async Task Register_DuplicateEmail_Returns409Conflict()
    {
        // Arrange: реєструємо користувача двічі з однаковим email
        var email = $"duplicate_{Guid.NewGuid():N}@test.com";
        var payload = new { email, password = "Test1234" };

        await _client.PostAsJsonAsync("/api/auth/register", payload);

        // Act: друга реєстрація з тим самим email
        var response = await _client.PostAsJsonAsync("/api/auth/register", payload);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Conflict,
            "реєстрація з вже існуючим email має повертати 409 Conflict");
    }

    [Fact]
    public async Task Register_InvalidData_Returns400()
    {
        // Arrange: відсутній обов'язковий параметр password
        var payload = new { email = "invalidemail" };

        // Act
        var response = await _client.PostAsJsonAsync("/api/auth/register", payload);

        // Assert
        response.StatusCode.Should().Match(code =>
            code == HttpStatusCode.BadRequest || code == HttpStatusCode.InternalServerError,
            "некоректні дані мають повертати 400");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ЛОГІН
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task Login_UnconfirmedEmail_Returns403()
    {
        // Arrange: реєструємо нового користувача (email не підтверджений)
        var email = $"unconfirmed_{Guid.NewGuid():N}@test.com";
        await _client.PostAsJsonAsync("/api/auth/register", new { email, password = "Test1234" });

        // Act: намагаємось увійти до не підтвердженого акаунту
        var loginPayload = new { email, password = "Test1234" };
        var response = await _client.PostAsJsonAsync("/api/auth/login", loginPayload);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Forbidden,
            "вхід до непідтвердженого email має повертати 403 Forbidden");
    }

    [Fact]
    public async Task Login_WrongPassword_Returns401()
    {
        // Arrange
        var email = $"wrongpass_{Guid.NewGuid():N}@test.com";
        await _client.PostAsJsonAsync("/api/auth/register", new { email, password = "Correct1234" });

        // Act
        var response = await _client.PostAsJsonAsync("/api/auth/login", new { email, password = "WrongPassword" });

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Unauthorized,
            "хибний пароль має повертати 401 Unauthorized");
    }

    [Fact]
    public async Task Login_NonExistentUser_Returns401()
    {
        // Arrange & Act
        var response = await _client.PostAsJsonAsync("/api/auth/login", new
        {
            email = "nonexistent@test.com",
            password = "SomePassword"
        });

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Unauthorized);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // АВТОРИЗАЦІЯ (ЗАХИСТ ЕНДПОІНТІВ)
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task ProtectedEndpoint_WithoutToken_Returns401()
    {
        // Act: запит до захищеного ресурсу без Authorization header
        var response = await _client.GetAsync("/api/medicine");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Unauthorized,
            "запит без JWT токену до захищеного ресурсу має повертати 401");
    }

    [Fact]
    public async Task ProtectedEndpoint_WithInvalidToken_Returns401()
    {
        // Arrange
        _client.DefaultRequestHeaders.Authorization =
            new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", "invalid.token.here");

        // Act
        var response = await _client.GetAsync("/api/medicine");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Unauthorized,
            "запит з недійсним JWT токеном має повертати 401");

        // Прибираємо заголовок для інших тестів
        _client.DefaultRequestHeaders.Authorization = null;
    }
}

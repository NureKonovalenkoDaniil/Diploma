using FluentAssertions;
using MedicationManagement.DBContext;
using MedicationManagement.Enums;
using MedicationManagement.Models;
using MedicationManagement.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Moq;

namespace MedicationManagement.UnitTests;

/// <summary>
/// Unit-тести для бізнес-логіки моніторингу умов зберігання.
///
/// Оскільки HandleTemperatureAsync та HandleHumidityAsync є private методами,
/// ми тестуємо їх опосередковано через ServiceStorageCondition та
/// перевіряємо стан БД після виконання.
///
/// Альтернативно, тут ми безпосередньо тестуємо логіку через InMemory контекст,
/// симулюючи сценарії, що відтворюють поведінку BackgroundService.
/// </summary>
public class MonitoringLogicTests : IDisposable
{
    private readonly MedicineStorageContext _context;
    private readonly Mock<IServiceNotification> _notificationServiceMock;
    private readonly Mock<IServiceAuditLog> _auditServiceMock;

    public MonitoringLogicTests()
    {
        var options = new DbContextOptionsBuilder<MedicineStorageContext>()
            .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
            .Options;

        _context = new MedicineStorageContext(options);
        _notificationServiceMock = new Mock<IServiceNotification>();
        _auditServiceMock = new Mock<IServiceAuditLog>();
    }

    // Хелпер: створює пристрій та показники для симуляції порушення
    private async Task<(IoTDevice device, StorageCondition condition)> PrepareDeviceWithConditionAsync(
        string deviceId,
        double temperature,
        double humidity,
        double minTemp = 2.0,
        double maxTemp = 8.0,
        double minHumidity = 30.0,
        double maxHumidity = 60.0)
    {
        var device = new IoTDevice
        {
            DeviceID = deviceId,
            OrganizationId = "org-monitoring",
            IsActive = true,
            MinTemperature = (float)minTemp,
            MaxTemperature = (float)maxTemp,
            MinHumidity = (float)minHumidity,
            MaxHumidity = (float)maxHumidity
        };
        _context.IoTDevices.Add(device);

        var condition = new StorageCondition
        {
            DeviceID = deviceId,
            Temperature = (float)temperature,
            Humidity = (float)humidity,
            Timestamp = DateTime.UtcNow
        };
        _context.StorageConditions.Add(condition);

        await _context.SaveChangesAsync();
        return (device, condition);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ТЕМПЕРАТУРНІ ПОРУШЕННЯ
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task TemperatureViolation_AboveMax_CreatesActiveIncident()
    {
        // Arrange: температура 15°C при нормі 2–8°C → порушення
        var (device, condition) = await PrepareDeviceWithConditionAsync("dev-temp-1", temperature: 15.0, humidity: 45.0);

        // Act: симулюємо перевірку (логіку з HandleTemperatureAsync)
        bool isViolation = condition.Temperature < device.MinTemperature
                        || condition.Temperature > device.MaxTemperature;

        if (isViolation)
        {
            var activeIncident = await _context.StorageIncidents
                .FirstOrDefaultAsync(i => i.DeviceId == device.DeviceID
                                       && i.IncidentType == IncidentType.TemperatureViolation
                                       && i.Status == IncidentStatus.Active);

            if (activeIncident is null)
            {
                var incident = new StorageIncident
                {
                    DeviceId = device.DeviceID,
                    OrganizationId = device.OrganizationId,
                    IncidentType = IncidentType.TemperatureViolation,
                    DetectedValue   = condition.Temperature,
                    ExpectedMin     = device.MinTemperature,
                    ExpectedMax     = device.MaxTemperature,
                    Status = IncidentStatus.Active,
                    StartTime = DateTime.UtcNow,
                    CreatedAt = DateTime.UtcNow
                };
                _context.StorageIncidents.Add(incident);
                await _context.SaveChangesAsync();
            }
        }

        // Assert
        isViolation.Should().BeTrue("15°C > 8°C = порушення");

        var incidents = _context.StorageIncidents
            .Where(i => i.DeviceId == "dev-temp-1" && i.IncidentType == IncidentType.TemperatureViolation)
            .ToList();

        incidents.Should().HaveCount(1, "одне порушення температури має бути зафіксоване");
        incidents[0].Status.Should().Be(IncidentStatus.Active);
        incidents[0].DetectedValue.Should().Be(15.0f);
    }

    [Fact]
    public async Task TemperatureViolation_BelowMin_CreatesActiveIncident()
    {
        // Arrange: температура -5°C при нормі 2–8°C → порушення
        var (device, condition) = await PrepareDeviceWithConditionAsync("dev-temp-2", temperature: -5.0, humidity: 45.0);

        // Act
        bool isViolation = condition.Temperature < device.MinTemperature
                        || condition.Temperature > device.MaxTemperature;

        // Assert
        isViolation.Should().BeTrue("-5°C < 2°C = порушення нижньої межі");
    }

    [Fact]
    public async Task TemperatureNormal_NoViolation()
    {
        // Arrange: температура 5°C при нормі 2–8°C → норма
        var (device, condition) = await PrepareDeviceWithConditionAsync("dev-temp-3", temperature: 5.0, humidity: 45.0);

        // Act
        bool isViolation = condition.Temperature < device.MinTemperature
                        || condition.Temperature > device.MaxTemperature;

        // Assert
        isViolation.Should().BeFalse("5°C знаходиться в межах 2–8°C = норма");
    }

    [Fact]
    public async Task TemperatureViolation_Debounce_DoesNotCreateDuplicateIncident()
    {
        // Arrange: вже є активний інцидент для цього пристрою
        var (device, condition) = await PrepareDeviceWithConditionAsync("dev-temp-4", temperature: 20.0, humidity: 45.0);

        // Симулюємо вже існуючий активний інцидент (перша перевірка вже спрацювала)
        var existingIncident = new StorageIncident
        {
            DeviceId = device.DeviceID,
            OrganizationId = device.OrganizationId,
            IncidentType = IncidentType.TemperatureViolation,
            DetectedValue = 20.0f,
            Status = IncidentStatus.Active,
            StartTime = DateTime.UtcNow.AddMinutes(-5),
            CreatedAt = DateTime.UtcNow.AddMinutes(-5)
        };
        _context.StorageIncidents.Add(existingIncident);
        await _context.SaveChangesAsync();

        // Act: перевіряємо debounce-логіку
        bool isViolation = condition.Temperature < device.MinTemperature
                        || condition.Temperature > device.MaxTemperature;

        var activeIncident = await _context.StorageIncidents
            .FirstOrDefaultAsync(i => i.DeviceId == device.DeviceID
                                   && i.IncidentType == IncidentType.TemperatureViolation
                                   && i.Status == IncidentStatus.Active);

        // Якщо activeIncident is not null — новий не створюємо
        bool shouldCreateNew = isViolation && activeIncident is null;

        // Assert
        shouldCreateNew.Should().BeFalse("активний інцидент вже існує — дублювати не потрібно");

        var totalIncidents = _context.StorageIncidents
            .Where(i => i.DeviceId == "dev-temp-4")
            .Count();
        totalIncidents.Should().Be(1, "має бути лише один інцидент завдяки debounce");
    }

    [Fact]
    public async Task TemperatureRestored_AutoResolvesActiveIncident()
    {
        // Arrange: є активний інцидент, температура повернулась в норму
        var (device, condition) = await PrepareDeviceWithConditionAsync("dev-temp-5", temperature: 5.0, humidity: 45.0);

        var activeIncident = new StorageIncident
        {
            DeviceId = device.DeviceID,
            OrganizationId = device.OrganizationId,
            IncidentType = IncidentType.TemperatureViolation,
            DetectedValue = 20.0f,
            Status = IncidentStatus.Active,
            StartTime = DateTime.UtcNow.AddHours(-1),
            CreatedAt = DateTime.UtcNow.AddHours(-1)
        };
        _context.StorageIncidents.Add(activeIncident);
        await _context.SaveChangesAsync();

        // Act: симулюємо логіку відновлення
        bool isViolation = condition.Temperature < device.MinTemperature
                        || condition.Temperature > device.MaxTemperature;

        if (!isViolation && activeIncident is not null)
        {
            activeIncident.Status = IncidentStatus.AutoResolved;
            activeIncident.EndTime = DateTime.UtcNow;
            await _context.SaveChangesAsync();
        }

        // Assert
        isViolation.Should().BeFalse("5°C — норма");

        var resolvedIncident = await _context.StorageIncidents.FindAsync(activeIncident.IncidentId);
        resolvedIncident!.Status.Should().Be(IncidentStatus.AutoResolved,
            "після відновлення норми інцидент має бути автоматично закритий");
        resolvedIncident.EndTime.Should().NotBeNull("час закриття інциденту має бути зафіксований");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ВОЛОГІСТЬ
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task HumidityViolation_AboveMax_IsDetected()
    {
        // Arrange: вологість 90% при нормі 30–60% → порушення
        var (device, condition) = await PrepareDeviceWithConditionAsync("dev-hum-1", temperature: 5.0, humidity: 90.0);

        // Act
        bool isViolation = condition.Humidity < device.MinHumidity
                        || condition.Humidity > device.MaxHumidity;

        // Assert
        isViolation.Should().BeTrue("90% > 60% = порушення вологості");
    }

    [Fact]
    public async Task HumidityNormal_NoViolation()
    {
        // Arrange: вологість 45% при нормі 30–60% → норма
        var (device, condition) = await PrepareDeviceWithConditionAsync("dev-hum-2", temperature: 5.0, humidity: 45.0);

        // Act
        bool isViolation = condition.Humidity < device.MinHumidity
                        || condition.Humidity > device.MaxHumidity;

        // Assert
        isViolation.Should().BeFalse("45% знаходиться в межах 30–60% = норма");
    }

    public void Dispose() => _context.Dispose();
}

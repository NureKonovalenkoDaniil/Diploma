using FluentAssertions;
using MedicationManagement.Enums;
using MedicationManagement.Models;

namespace MedicationManagement.UnitTests;

/// <summary>
/// Unit-тести для ServiceMedicine:
/// - Створення препарату з підстановкою OrganizationId (Multi-tenancy)
/// - Автоматична lifecycle-подія при створенні
/// - Операція Issue: списання, insufficient stock
/// - Операція Dispose: утилізація
/// </summary>
public class ServiceMedicineTests : ServiceMedicineTestBase
{
    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task Create_SetsOrganizationId_FromCurrentUser()
    {
        // Arrange
        var service = CreateService();
        var medicine = new Medicine { Name = "Аспірин", Type = "Таблетки", Quantity = 10, ExpiryDate = DateTime.UtcNow.AddYears(1) };

        // Act
        var result = await service.Create(medicine, performedBy: "manager@test.com");

        // Assert
        result.Should().NotBeNull();
        result!.OrganizationId.Should().Be("org-1",
            "бо поточний HTTP-контекст містить OrganizationId = org-1");
    }

    [Fact]
    public async Task Create_WithAutoReceivedEvent_CreatesLifecycleEvent()
    {
        // Arrange
        var service = CreateService();
        var medicine = new Medicine { Name = "Ібупрофен", Type = "Таблетки", Quantity = 20, ExpiryDate = DateTime.UtcNow.AddYears(1) };

        // Act
        var result = await service.Create(medicine, performedBy: "manager@test.com", autoReceivedEvent: true);

        // Assert
        result.Should().NotBeNull();
        var events = Context.MedicineLifecycleEvents.Where(e => e.MedicineId == result!.MedicineID).ToList();
        events.Should().HaveCount(1, "автоматична подія Received має бути створена");
        events[0].EventType.Should().Be(LifecycleEventType.Received);
        events[0].Quantity.Should().Be(20);
    }

    [Fact]
    public async Task Create_WithAutoReceivedEventFalse_DoesNotCreateLifecycleEvent()
    {
        // Arrange
        var service = CreateService();
        var medicine = new Medicine { Name = "Парацетамол", Type = "Таблетки", Quantity = 5, ExpiryDate = DateTime.UtcNow.AddYears(1) };

        // Act
        var result = await service.Create(medicine, performedBy: "manager@test.com", autoReceivedEvent: false);

        // Assert
        result.Should().NotBeNull();
        var events = Context.MedicineLifecycleEvents.Where(e => e.MedicineId == result!.MedicineID).ToList();
        events.Should().BeEmpty("autoReceivedEvent=false, тому події не має бути");
    }

    [Fact]
    public async Task Create_NullMedicine_ReturnsNull()
    {
        // Arrange
        var service = CreateService();

        // Act
        var result = await service.Create(null!, performedBy: "manager@test.com");

        // Assert
        result.Should().BeNull("null-об'єкт не повинен зберігатися в БД");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MULTI-TENANCY: READ
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task Read_ReturnsOnlyMedicinesOfCurrentOrganization()
    {
        // Arrange: два різні записи у різних організаціях
        Context.Medicines.AddRange(
            new Medicine { Name = "Ліки Org-1", Type = "Таблетки", OrganizationId = "org-1", Quantity = 5, ExpiryDate = DateTime.UtcNow.AddYears(1) },
            new Medicine { Name = "Ліки Org-2", Type = "Таблетки", OrganizationId = "org-2", Quantity = 5, ExpiryDate = DateTime.UtcNow.AddYears(1) }
        );
        await Context.SaveChangesAsync();

        SetCurrentUser("org-1", "Manager");
        var service = CreateService();

        // Act
        var result = (await service.Read()).ToList();

        // Assert
        result.Should().HaveCount(1, "менеджер org-1 повинен бачити лише свої ліки");
        result[0].Name.Should().Be("Ліки Org-1");
    }

    [Fact]
    public async Task Read_AsAdministrator_ReturnsAllMedicines()
    {
        // Arrange
        Context.Medicines.AddRange(
            new Medicine { Name = "Ліки A", Type = "Таблетки", OrganizationId = "org-1", Quantity = 5, ExpiryDate = DateTime.UtcNow.AddYears(1) },
            new Medicine { Name = "Ліки B", Type = "Таблетки", OrganizationId = "org-2", Quantity = 5, ExpiryDate = DateTime.UtcNow.AddYears(1) }
        );
        await Context.SaveChangesAsync();

        // Адмін бачить усе
        SetCurrentUser("org-admin", "Administrator");
        var service = CreateService();

        // Act
        var result = (await service.Read()).ToList();

        // Assert
        result.Should().HaveCount(2, "адміністратор повинен бачити всі ліки незалежно від організації");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ISSUE
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task Issue_DecreasesQuantity_AndCreatesIssuedEvent()
    {
        // Arrange
        var medicine = new Medicine { Name = "Вітамін C", Type = "Таблетки", OrganizationId = "org-1", Quantity = 50, ExpiryDate = DateTime.UtcNow.AddYears(1) };
        Context.Medicines.Add(medicine);
        await Context.SaveChangesAsync();

        var service = CreateService();

        // Act
        var (result, error) = await service.Issue(medicine.MedicineID, 10, performedBy: "manager@test.com");

        // Assert
        error.Should().BeNull("видача 10 од. при наявних 50 — коректна операція");
        result.Should().NotBeNull();
        result!.Quantity.Should().Be(40, "50 - 10 = 40");

        var events = Context.MedicineLifecycleEvents.Where(e => e.MedicineId == medicine.MedicineID).ToList();
        events.Should().ContainSingle(e => e.EventType == LifecycleEventType.Issued);
    }

    [Fact]
    public async Task Issue_InsufficientStock_ReturnsError()
    {
        // Arrange
        var medicine = new Medicine { Name = "Рідкий препарат", Type = "Розчин", OrganizationId = "org-1", Quantity = 5, ExpiryDate = DateTime.UtcNow.AddYears(1) };
        Context.Medicines.Add(medicine);
        await Context.SaveChangesAsync();

        var service = CreateService();

        // Act
        var (result, error) = await service.Issue(medicine.MedicineID, 100, performedBy: "manager@test.com");

        // Assert
        result.Should().BeNull("кількість для видачі перевищує наявний залишок");
        error.Should().Contain("Insufficient stock");
    }

    [Fact]
    public async Task Issue_NotFound_ReturnsError()
    {
        // Arrange
        var service = CreateService();

        // Act
        var (result, error) = await service.Issue(999_999, 1, performedBy: "manager@test.com");

        // Assert
        result.Should().BeNull();
        error.Should().Be("Medicine not found");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DISPOSE
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task Dispose_DecreasesQuantity_AndCreatesDisposedEvent()
    {
        // Arrange
        var medicine = new Medicine { Name = "Прострочений", Type = "Таблетки", OrganizationId = "org-1", Quantity = 30, ExpiryDate = DateTime.UtcNow.AddDays(-1) };
        Context.Medicines.Add(medicine);
        await Context.SaveChangesAsync();

        var service = CreateService();

        // Act
        var (result, error) = await service.Dispose(medicine.MedicineID, 30, performedBy: "manager@test.com");

        // Assert
        error.Should().BeNull();
        result.Should().NotBeNull();
        result!.Quantity.Should().Be(0);
        result!.Status.Should().Be(MedicineStatus.Disposed, "коли Quantity=0 після утилізації, статус має стати Disposed");

        var events = Context.MedicineLifecycleEvents.Where(e => e.MedicineId == medicine.MedicineID).ToList();
        events.Should().ContainSingle(e => e.EventType == LifecycleEventType.Disposed);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECEIVE
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task Receive_IncreasesQuantity_AndCreatesReceivedEvent()
    {
        // Arrange
        var medicine = new Medicine { Name = "Синупрет", Type = "Краплі", OrganizationId = "org-1", Quantity = 10, ExpiryDate = DateTime.UtcNow.AddYears(2) };
        Context.Medicines.Add(medicine);
        await Context.SaveChangesAsync();

        var service = CreateService();

        // Act
        var (result, error) = await service.Receive(medicine.MedicineID, 25, performedBy: "manager@test.com");

        // Assert
        error.Should().BeNull();
        result.Should().NotBeNull();
        result!.Quantity.Should().Be(35, "10 + 25 = 35");

        var events = Context.MedicineLifecycleEvents.Where(e => e.MedicineId == medicine.MedicineID).ToList();
        events.Should().ContainSingle(e => e.EventType == LifecycleEventType.Received);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MOVE
    // ─────────────────────────────────────────────────────────────────────────

    [Fact]
    public async Task Move_ChangesStorageLocation_AndCreatesMovedEvent()
    {
        // Arrange: спочатку зберігаємо локації (отримуємо їх ID)
        var locationA = new StorageLocation { Name = "Склад A", OrganizationId = "org-1", LocationType = StorageLocationType.Shelf };
        var locationB = new StorageLocation { Name = "Аптека B", OrganizationId = "org-1", LocationType = StorageLocationType.Refrigerator };
        Context.StorageLocations.AddRange(locationA, locationB);
        await Context.SaveChangesAsync(); // Зберігаємо, щоб LocationId заповнились

        // Тільки після цього додаємо препарат з коректним FK
        var medicine = new Medicine
        {
            Name = "Нурофен",
            Type = "Таблетки",
            OrganizationId = "org-1",
            Quantity = 20,
            ExpiryDate = DateTime.UtcNow.AddYears(1),
            StorageLocationId = locationA.LocationId
        };
        Context.Medicines.Add(medicine);
        await Context.SaveChangesAsync();

        var service = CreateService();

        // Act
        var result = await service.Move(medicine.MedicineID, locationB.LocationId, performedBy: "manager@test.com");

        // Assert
        result.Should().NotBeNull();
        result!.StorageLocationId.Should().Be(locationB.LocationId, "препарат має бути переміщений до Аптека B");

        var events = Context.MedicineLifecycleEvents.Where(e => e.MedicineId == medicine.MedicineID).ToList();
        events.Should().ContainSingle(e => e.EventType == LifecycleEventType.Moved);
    }

    [Fact]
    public async Task Move_TargetLocationNotFound_ReturnsNull()
    {
        // Arrange
        var medicine = new Medicine { Name = "Ліки X", Type = "Таблетки", OrganizationId = "org-1", Quantity = 5, ExpiryDate = DateTime.UtcNow.AddYears(1) };
        Context.Medicines.Add(medicine);
        await Context.SaveChangesAsync();

        var service = CreateService();

        // Act
        var result = await service.Move(medicine.MedicineID, 999_999, performedBy: "manager@test.com");

        // Assert
        result.Should().BeNull("локація з ID 999999 не існує, переміщення має провалитися");
    }
}

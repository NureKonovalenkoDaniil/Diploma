# Проміжний технічний аудит: Фази 1–3

> **Проєкт:** Розподілена інформаційна система управління життєвим циклом та безпекою зберігання медичних препаратів  
> **Дата аудиту:** 2026-04-20  
> **Охоплення:** Backend (ASP.NET Core 8), IoT emulator (ESP32/PlatformIO)  
> **Статус:** Фаза 1 ✅ · Фаза 2 ✅ · Фаза 3 ✅ · Фаза 4 🔲 (SPA Frontend — наступна)

---

## 1. Готовий функціонал

### 1.1 Автентифікація та управління користувачами

| Ендпоінт | Метод | Доступ | Опис |
|---|---|---|---|
| `/api/auth/register` | POST | Public | Реєстрація користувача. Перший зареєстрований автоматично отримує роль `Administrator`, решта — `User` |
| `/api/auth/login` | POST | Public | Автентифікація, видача JWT-токена |
| `/api/auth/me` | GET | JWT | Повертає `{ id, userName, email, roles[] }` поточного авторизованого користувача |
| `/api/auth/create-role` | POST | Administrator | Створення нової ролі |
| `/api/auth/assign-role` | POST | Administrator | Призначення ролі користувачу |

**Ролі системи:** `Administrator`, `User`, `Sensor` — додаються автоматично при старті застосунку через `EnsureRolesCreated`.

---

### 1.2 Управління препаратами (`/api/medicine`)

| Ендпоінт | Метод | Доступ | Опис |
|---|---|---|---|
| `GET /api/medicine` | GET | JWT | Список усіх препаратів |
| `GET /api/medicine/{id}` | GET | JWT | Препарат за ID |
| `POST /api/medicine` | POST | Administrator | Створення препарату |
| `PATCH /api/medicine/{id}` | PATCH | Administrator | Оновлення через JSON Patch |
| `DELETE /api/medicine/{id}` | DELETE | Administrator | Видалення препарату |
| `GET /api/medicine/low-stock` | GET | Administrator | Препарати з кількістю нижче порогу (`?threshold=10`) |
| `GET /api/medicine/expiring` | GET | Administrator | Препарати, що закінчуються (`?daysThreshold=7`) |
| `GET /api/medicine/replenishment-recommendations` | GET | Administrator | Рекомендації до поповнення (фіксована логіка: поріг 10, рекомендована к-сть = 100 - поточна) |

**Поля `Medicine`:** `MedicineID`, `Name`, `Type`, `ExpiryDate`, `Quantity`, `Category`, `Manufacturer?`, `BatchNumber?`, `Description?`, `MinStorageTemp?`, `MaxStorageTemp?`, `StorageLocationId?` (FK).

---

### 1.3 Управління IoT-пристроями (`/api/iotdevice`)

| Ендпоінт | Метод | Доступ | Опис |
|---|---|---|---|
| `GET /api/iotdevice` | GET | JWT | Список усіх пристроїв |
| `GET /api/iotdevice/{id}` | GET | JWT | Пристрій за ID (також використовує IoT-емулятор для отримання порогів) |
| `POST /api/iotdevice` | POST | Administrator | Реєстрація пристрою |
| `PATCH /api/iotdevice/{id}` | PATCH | Administrator | Оновлення через JSON Patch |
| `DELETE /api/iotdevice/{id}` | DELETE | Administrator | Видалення пристрою |
| `PATCH /api/iotdevice/setstatus/{deviceId}` | PATCH | Administrator | Активація / деактивація пристрою |
| `GET /api/iotdevice/conditions/{deviceId}` | GET | JWT | Всі показники умов зберігання для пристрою |

**Поля `IoTDevice`:** `DeviceID`, `Location`, `Type`, `Parameters`, `IsActive`, `MinTemperature`, `MaxTemperature`, `MinHumidity`, `MaxHumidity`.

---

### 1.4 Умови зберігання (`/api/storagecondition`)

| Ендпоінт | Метод | Доступ | Опис |
|---|---|---|---|
| `GET /api/storagecondition` | GET | JWT | Всі записи умов |
| `GET /api/storagecondition/{id}` | GET | JWT | Запис за ID |
| `POST /api/storagecondition` | POST | JWT | Запис нових показників (основний ендпоінт IoT-емулятора) |
| `PATCH /api/storagecondition/{id}` | PATCH | JWT | Оновлення запису |
| `DELETE /api/storagecondition/{id}` | DELETE | JWT | Видалення запису |
| `GET /api/storagecondition/checkCondition` | GET | JWT | Прямий запит перевірки наявних порушень для всіх пристроїв |

---

### 1.5 Локації зберігання (`/api/storagelocation`)

| Ендпоінт | Метод | Доступ | Опис |
|---|---|---|---|
| `GET /api/storagelocation` | GET | JWT | Список усіх локацій (з `IoTDevice`) |
| `GET /api/storagelocation/{id}` | GET | JWT | Локація за ID (з `IoTDevice` + `Medicines`) |
| `POST /api/storagelocation` | POST | Administrator | Створення локації |
| `PUT /api/storagelocation/{id}` | PUT | Administrator | Повне оновлення |
| `DELETE /api/storagelocation/{id}` | DELETE | Administrator | Видалення |

**Типи локацій (`StorageLocationType`):** `Refrigerator`, `Shelf`, `Warehouse`, `Cabinet`, `Other`.

---

### 1.6 Інциденти зберігання (`/api/storageincident`)

| Ендпоінт | Метод | Доступ | Опис |
|---|---|---|---|
| `GET /api/storageincident` | GET | JWT | Всі інциденти (з IoTDevice + StorageLocation) |
| `GET /api/storageincident/active` | GET | JWT | Лише активні (Status=Active) |
| `GET /api/storageincident/{id}` | GET | JWT | Інцидент за ID |
| `POST /api/storageincident` | POST | Administrator | Ручне створення інциденту |
| `PATCH /api/storageincident/{id}/resolve` | PATCH | Administrator | Ручне закриття інциденту |

**Логіка:** Інциденти автоматично створюються `StorageConditionMonitoringService` при виявленні порушення (debounce — один інцидент на device+type). При поверненні в норму — auto-resolve.  
**Типи (`IncidentType`):** `TemperatureViolation`, `HumidityViolation`.  
**Статуси (`IncidentStatus`):** `Active`, `Resolved`, `Acknowledged`.

---

### 1.7 LifeCycle-події препаратів (`/api/medicinelifecycle`)

| Ендпоінт | Метод | Доступ | Опис |
|---|---|---|---|
| `GET /api/medicinelifecycle` | GET | Administrator | Всі події для всіх препаратів |
| `GET /api/medicinelifecycle/{id}` | GET | JWT | Подія за ID |
| `GET /api/medicinelifecycle/medicine/{medicineId}` | GET | JWT | Всі події для конкретного препарату |
| `POST /api/medicinelifecycle` | POST | JWT | Додавання нової події (PerformedBy встановлюється з JWT-контексту) |

**Типи подій (`LifecycleEventType`):** `Received`, `Dispensed`, `Relocated`, `Disposed`, `Inspected`, `Quarantined`, `Returned`.

---

### 1.8 Сповіщення (`/api/notification`)

| Ендпоінт | Метод | Доступ | Опис |
|---|---|---|---|
| `GET /api/notification` | GET | JWT | Всі сповіщення (опціональний фільтр `?role=Administrator`) |
| `GET /api/notification/unread` | GET | JWT | Непрочитані сповіщення |
| `POST /api/notification` | POST | Administrator | Ручне створення |
| `PATCH /api/notification/{id}/read` | PATCH | JWT | Позначити одне як прочитане |
| `PATCH /api/notification/read-all` | PATCH | JWT | Позначити всі як прочитані (опціональний фільтр `?role=...`) |

**Типи (`NotificationType`):** `StorageViolation`, `Expiry`, `LowStock`, `System`.  
**Генеруються автоматично** `StorageConditionMonitoringService` (порушення/відновлення) та `ExpiryNotificationService` (закінчення терміну).

---

### 1.9 Журнал аудиту (`/api/auditlog`)

| Ендпоінт | Метод | Доступ | Опис |
|---|---|---|---|
| `GET /api/auditlog` | GET | Administrator | Журнал аудиту з фільтрацією: `?from=`, `?to=`, `?user=`, `?action=` |

**Поля:** `Action`, `User`, `Timestamp`, `Details`, `EntityType?`, `EntityId?`, `Severity` (`Info`/`Warning`/`Error`).  
**Записи формуються** при кожній CRUD-операції, авторизаційних подіях, а також автоматично фоновими сервісами.

---

### 1.10 IoT Emulator

- **Платформа:** ESP32 DevKit C v4 + DHT22 + Buzzer (Wokwi + PlatformIO + C++)
- **Цикл роботи:**
  1. Щосекунди опитує DHT22 (`dht.readTemperature()`, `dht.readHumidity()`)
  2. Кожні 10 секунд (`sendInterval`) надсилає `POST /api/storagecondition` з температурою та вологістю
  3. Кожні 5 секунд (`checkInterval`) виконує `GET /api/iotdevice/{id}` для отримання актуальних порогів
  4. Активує buzzer при локальному виявленні порушення

---

## 2. Основні архітектурні рішення

### 2.1 Розширення бази даних

**Схема зв'язків (Entity Relationship)**

```
IoTDevice (1) ──< StorageCondition (N)
IoTDevice (1) ──< StorageIncident (N)
IoTDevice (1) ──o StorageLocation  (0..1) — nullable FK

StorageLocation (1) ──< Medicine (N)        — nullable FK у Medicine
StorageLocation (0..1) ──< MedicineLifecycleEvent (N) — RelatedLocationId nullable

Medicine (1) ──< MedicineLifecycleEvent (N)

AuditLog — незалежна таблиця, містить EntityType + EntityId як soft-reference
Notification — незалежна таблиця, містить RelatedEntityType + RelatedEntityId як soft-reference
```

Прийняте рішення: `Notification` та `AuditLog` навмисно не мають строгих FK-зв'язків до пов'язаних сутностей — лише soft-reference через `string RelatedEntityType` та `int? RelatedEntityId`. Це дозволяє логувати події для будь-якої сутності без зміни схеми при розширенні.

**Enum persistence strategy:** Усі enum-поля зберігаються як рядки (`HasConversion<string>` або через `[JsonConverter]`). Перевага — читабельний SQL без числових констант; недолік — більший розмір рядків у БД порівняно з `int`.

---

### 2.2 JWT-безпека (Фаза 1)

До Фази 1 існували три критичні вразливості:

| Проблема | Рішення |
|---|---|
| `GenerateJwtToken` використовував `.Result` (deadlock-ризик) | Замінено на `await` |
| Ключ підписання кодувався через `Encoding.ASCII` (втрата байт при Unicode) | Замінено на `Encoding.UTF8` |
| JWT-секрет зберігався у `appsettings.json` | Перенесено у `dotnet user-secrets` |
| Строк токена — хардкод | Читається з `Jwt:ExpireDays` у конфігурації |

Поточний рядок підписання:
```csharp
var key = Encoding.UTF8.GetBytes(_configuration["Jwt:Key"]);
// Ключ отримується з User Secrets — відсутній в source control
```

---

### 2.3 Взаємодія фонових сервісів з БД

Обидва `BackgroundService` реалізують **scoped DI in singleton context** через `IServiceProvider`:

```csharp
using var scope = _serviceProvider.CreateScope();
var db = scope.ServiceProvider.GetRequiredService<MedicineStorageContext>();
```

Це коректний паттерн: `BackgroundService` є singleton, а `DbContext` — scoped. Кожен тік сервісу відкриває власний scope і власний `DbContext`, що унеможливлює race conditions при конкурентному доступі.

**`StorageConditionMonitoringService` — логіка debounce:**
```
foreach device in active_devices:
    condition = last StorageCondition for device

    // Температура
    if violation AND no active TemperatureViolation incident for device:
        → create StorageIncident(Active) + Notification + AuditLog(Warning)
    elif no violation AND active TemperatureViolation incident exists:
        → resolve incident (EndTime, Status=Resolved) + Notification + AuditLog(Info)

    // Вологість — аналогічно
```

Debounce реалізований на рівні бази даних: новий інцидент створюється лише якщо `StorageIncidents` не містить активного запису для `DeviceId + IncidentType`. Це гарантує **не більше одного активного інциденту** на пристрій+тип одночасно.

**`ExpiryNotificationService` — дедуплікація:**
```
foreach expiring_medicine:
    if NOT exists Notification(Type=Expiry, RelatedEntityId=medicineId, CreatedAt.Date=today):
        → create Notification + AuditLog(Warning)
```

Дедуплікація perday: одне сповіщення на препарат на добу.

---

### 2.4 Конфігурація моніторингу

```json
"Monitoring": {
  "IntervalSeconds": 60,
  "ExpiryWarningDays": 7
}
```

Значення читаються через `IConfiguration.GetValue<int>()` з fallback на default. Зміна параметрів без перезбірки — лише рестарт сервісу.

---

## 3. Технологічний стек

| Компонент | Технологія | Версія |
|---|---|---|
| Runtime | .NET | 8.0 |
| Web framework | ASP.NET Core | 8.0 |
| ORM | Entity Framework Core + SQL Server | 8.x |
| Автентифікація | ASP.NET Core Identity + JWT Bearer | 8.x / MSAL |
| JSON Patch | `Microsoft.AspNetCore.JsonPatch` + Newtonsoft | — |
| Background tasks | `BackgroundService` (IHostedService) | вбудований |
| DI | Built-in ASP.NET Core DI (Native) | — |
| Документація API | Swagger / Swashbuckle | — |
| Конфігурація секретів | `dotnet user-secrets` | — |
| IoT Hardware (emulated) | ESP32 DevKit C v4 + DHT22 + Buzzer | — |
| IoT Platform | PlatformIO + Wokwi (симулятор) | — |
| IoT Networking | HTTPClient (C++ `HTTPClient.h`) | — |
| IoT Serialization | ArduinoJson | — |
| СУБД | Microsoft SQL Server | локальний dev |

---

## 4. Технічний борг та результати Code Review

> Нижче наведено суворий аудит усіх виявлених проблем. Кожна позначена пріоритетом: 🔴 Критично · 🟡 Середній · 🟢 Низький.

---

### 4.1 🔴 Критичні проблеми

#### TD-01: Хардкод `jwtToken`, `deviceID` та URL у `IoTEmulate/src/main.cpp`

```cpp
// Рядок 22 — IoTEmulate/src/main.cpp
const String deviceConfigUrl = "http://192.168.100.2:5000/api/iotdevice/4";
const String dataSendUrl     = "http://192.168.100.2:5000/api/storagecondition";
const String jwtToken        = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."; // живий токен у репо!
int deviceID = 4;
```

**Проблема:** Реальний JWT у вихідному коді — пряме порушення принципу безпеки "secrets out of source control". Токен скомпрометований. За 30-денним терміном він може бути ще активним.  
**Рішення для диплому:** Перенести у `config.h` (не у VCS) або використовувати NVS (Non-Volatile Storage) ESP32 для зберігання конфігурації.

---

#### TD-02: Логіка "перший користувач = Administrator" — security misconfiguration

```csharp
// AuthController.cs, рядок 64-65
var usersCount = _userManager.Users.Count();
var role = usersCount == 1 ? "Administrator" : "User";
```

**Проблема:** Якщо база даних скинута або за умов race condition (кілька одночасних реєстрацій), будь-який користувач може отримати адміністративні права.  
**Додаткова проблема:** `_userManager.Users.Count()` — синхронний виклик у async-контексті. Слід використовувати `await _userManager.Users.CountAsync()`.  
**Рішення для диплому:** Захарокодити перший адміністраторський акаунт через seed-дані у `EnsureRolesCreated`, або додати перевірку на наявність конкретного email з конфігурації.

---

### 4.2 🟡 Середні проблеми

#### TD-03: Відсутність `AsNoTracking()` у read-only запитах

У `ServiceStorageCondition`, `ServiceIoTDevice`, `ServiceMedicine` та інших сервісах усі `GET`-запити завантажують entities з tracking:

```csharp
// Приклад: ServiceMedicine.cs — Read()
return await _context.Medicines.ToListAsync(); // ← без AsNoTracking
```

EF Core відстежує стан кожного об'єкта у памʼяті через `ChangeTracker`. Для read-only ендпоінтів це зайві витрати памʼяті та ресурси CPU.  
**Рішення:** Додати `.AsNoTracking()` до всіх запитів, де результат не модифікується:

```csharp
return await _context.Medicines.AsNoTracking().ToListAsync();
```

**Підвищений пріоритет** для `GetAll()` у `ServiceStorageIncident` та `ServiceMedicineLifecycle`, що виконують `.Include()` — вони завантажують граф об'єктів:
```csharp
return await _context.StorageIncidents
    .AsNoTracking()           // ← додати
    .Include(i => i.IoTDevice)
    .Include(i => i.StorageLocation)
    .OrderByDescending(i => i.CreatedAt)
    .ToListAsync();
```

---

#### TD-04: Controllers повертають entity-моделі напряму (без DTO)

Усі контролери повертають `Medicine`, `StorageIncident`, `Notification` тощо напряму:

```csharp
return Ok(incidents); // IEnumerable<StorageIncident> — повна entity
```

**Наслідки:**
- Нав'язка циклічних JSON serialization помилок при .Include() глибоких графів (особливо Medicine → StorageLocation → Medicines → ...)
- Витік внутрішньої структури БД у відповідях API (over-fetching)
- Неможливість гнучко контролювати what is serialized

**Рішення для Фази 4:** Введено шар Response DTO для всіх ресурсів: `MedicineDto`, `StorageIncidentDto`, `IoTDeviceDto` тощо. Усі 8 контролерів оновлено на використання `.ToDto()` маппінгу для уникнення повернення прямих сутностей з БД.
```csharp
record MedicineDto(int Id, string Name, string Type, DateTime ExpiryDate, int Quantity, string? StorageLocationName);
```

---

#### TD-05: `NotificationController.Create` — приймає `Notification` entity напряму

```csharp
[HttpPost]
public async Task<IActionResult> Create([FromBody] Notification notification)
```

Frontend може передати довільний `NotificationId`, `CreatedAt`, `IsRead` тощо. В `ServiceNotification.Create(Notification)` вони явно перезаписуються, але це не задокументована поведінка і порушує принцип мінімальних привілеїв на рівні API-контракту.

---

#### TD-06: `AuditLogController` інжектує `MedicineStorageContext` напряму минаючи Service-шар

```csharp
// AuditLogController.cs
private readonly MedicineStorageContext _context; // ← прямий доступ до DbContext

public async Task<IActionResult> GetLogs(...)
{
    var query = _context.AuditLogs.AsQueryable();
    // ...
}
```

Це порушення Single Responsibility Principle і відсутність єдиного Service-шару. Якщо логіка запиту ускладниться — вона буде дублюватись. Слід ввести `IServiceAuditLogReader` або розширити `IServiceAuditLog`.

---

#### TD-07: `StorageConditionMonitoringService` — debounce без захисту від конкурентних тіків

```csharp
var activeIncident = await db.StorageIncidents
    .FirstOrDefaultAsync(i => i.DeviceId == device.DeviceID
                           && i.IncidentType == IncidentType.TemperatureViolation
                           && i.Status == IncidentStatus.Active);

if (isViolation && activeIncident is null)
{
    var incident = new StorageIncident { ... };
    db.StorageIncidents.Add(incident);
    await db.SaveChangesAsync(); // ← між перевіркою і вставкою немає блокування
```

**Теоретична проблема:** За умов затримки у БД (або якщо інтервал буде зменшено) — два тіки можуть одночасно пройти перевірку і обидва вставити інцидент. Для рівня дипломної роботи це прийнятно, але в production варто додати унікальний індекс `(DeviceId, IncidentType) WHERE Status = 'Active'`.

---

#### TD-08: `ServiceMedicine.GetExpiringMedicines` не виключає вже протерміновані препарати

```csharp
return await _context.Medicines
    .Where(m => m.ExpiryDate <= thresholdDate) // ← включає вже прострочені
    .ToListAsync();
```

Якщо `ExpiryDate` менший за `DateTime.Now`, препарат вже прострочений, але все одно потрапляє у список "expiring". Для `ExpiryNotificationService` це призводить до нескінченних сповіщень про вже протерміновані ліки.  
**Рішення:**
```csharp
.Where(m => m.ExpiryDate > DateTime.Now && m.ExpiryDate <= thresholdDate)
```

---

#### TD-09: `ReplenishmentRecommendations` — хардкод бізнес-логіки

```csharp
// ServiceMedicine.cs, рядок 70-76
var lowStockMedicines = await GetLowStockMedicines(10);   // ← поріг 10 = хардкод
return lowStockMedicines.Select(m => new ReplenishmentRecommendation
{
    RecommendedQuantity = 100 - m.Quantity                // ← 100 = хардкод
}).ToList();
```

Поріг запасу і рекомендована кількість мають читатись з конфігурації або з поля самого `Medicine` (наприклад, `MinimumStock`).

---

#### TD-10: `IoTDevice.Location` та `IoTDevice.Parameters` — строки без валідації

```csharp
public string Location { get; set; }   // без Required
public string Parameters { get; set; } // без Required, вільний текст
```

`Parameters` є неструктурованим полем — не зрозуміло, що зберігається: JSON, YAML, CSV? Це ускладнює роботу Frontend. Для Фази 4 варто або структурувати (окремі поля або JSON-схема), або задокументувати очікуваний формат.

---

### 4.3 🟢 Незначні проблеми

#### TD-11: Непослідовний стиль обробки помилок

Частина сервісів (`ServiceMedicine`, `ServiceIoTDevice`, `ServiceStorageCondition`) перехоплює виключення і повертає `null`/порожній список:

```csharp
catch (Exception ex)
{
    _logger.LogError(ex, "...");
    return null; // ← контролер отримує null і може повернути 500 замість 404
}
```

Нові сервіси (`ServiceStorageLocation`, `ServiceStorageIncident`, `ServiceNotification`) не мають try/catch взагалі — виключення "спливає" до контролера або middleware.

Підхід без try/catch у сервісах є правильнішим (помилки обробляються у middleware), але у рамках одного проєкту є непослідовність.

---

#### TD-12: `RoleDto` визначено як nested class у `AuthController`

```csharp
// AuthController.cs, рядок 213-217
public class RoleDto
{
    public string Email { get; set; }
    public string RoleName { get; set; }
}
```

`RoleDto` логічніше розмістити у `Models/` або `DTOs/` папці для дотримання структури проєкту.

---

#### TD-13: Swagger відображається лише у Development

```csharp
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI(...);
}
```

Для демонстрації дипломного проєкту — зручніше вмикати Swagger завжди (або умовно через конфігурацію), щоб не перемикати середовища для демо.

---

#### TD-14: Cookie-автентифікація налаштована у `Program.cs`, але нікуди не використовується

```csharp
.AddCookie(options => {
    options.LoginPath = "/Account/Login";
    // ...
})
```

Проєкт використовує виключно JWT Bearer. Cookie-автентифікація — залишок від попередньої web-MVC архітектури. Цей код вводить в оману і може спричинити небажану поведінку у middleware. Слід видалити.

---

#### TD-15: `app.Run()` дублюється у `ConfigureMiddleware`

```csharp
private static void ConfigureMiddleware(WebApplication app)
{
    // ...
    app.MapControllers();
    app.Run(); // ← дублювання виклику
}

// У Program.Main():
// ...
ConfigureMiddleware(app);
app.Run(); // ← цей ніколи не виконається, але і не шкодить
```

Фактично `app.Run()` у `ConfigureMiddleware` завершує виконання, тому зовнішній виклик у `Main` мертвий код. Незначне, але заплутане.

---

## 5. Підготовка до Фази 4: SPA Frontend

### 5.1 Оцінка готовності API

| Аспект | Стаціус | Примітка |
|---|---|---|
| CORS | ✅ Налаштовано | FrontendPolicy дозволяє запити з localhost:5173 / 3000 |
| Автентифікація (JWT) | ✅ Готово | `POST /api/auth/login` → токен → `Authorization: Bearer` |
| Профіль поточного юзера | ✅ `GET /api/auth/me` | Повертає `{ id, userName, email, roles[] }` — достатньо |
| CRUD для Medicine | ✅ Готово | Є Create/Read/Update(Patch)/Delete |
| CRUD для IoTDevice | ✅ Готово | — |
| CRUD для StorageLocation | ✅ Готово | — |
| Incidents API | ✅ Готово | GetAll, GetActive, Resolve |
| Notifications API | ✅ Готово | GetUnread, MarkAsRead — ідеально для notification bell |
| LifeCycle Events | ✅ Готово | — |
| AuditLog з фільтрами | ✅ Готово | `?from=`, `?to=`, `?user=`, `?action=` |
| Swagger / OpenAPI | ✅ Dev | Потрібно налаштувати для Production |
| Pagination | ❌ Відсутній | Усі GET повертають всю колекцію без пагінації |
| Response DTO | ✅ Готово | Усі контролери повертають DTO замість сирих entity |
| Sorting / Filtering | ❌ Мінімальний | Лише AuditLog має фільтри, решта — ні |

---

### 5.2 Першочергові зміни перед початком Фази 4

#### 1. Налаштування CORS (обов'язково)

```csharp
// Program.cs → RegisterServices
builder.Services.AddCors(options =>
{
    options.AddPolicy("FrontendPolicy", policy =>
    {
        policy.WithOrigins("http://localhost:5173") // Vite dev server
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

// ConfigureMiddleware — до UseAuthentication
app.UseCors("FrontendPolicy");
```

#### 2. Базові Response DTO (рекомендовано)

Мінімальний набір для Frontend:

```csharp
// Для Medicine list view
record MedicineListItemDto(int Id, string Name, string Type,
    DateTime ExpiryDate, int Quantity, string? StorageLocationName);

// Для Incident list view
record StorageIncidentDto(int Id, string DeviceLocation, string IncidentType,
    float DetectedValue, float ExpectedMin, float ExpectedMax,
    string Status, DateTime StartTime, DateTime? EndTime);

// Для Notification
record NotificationDto(int Id, string Type, string Title,
    string Message, bool IsRead, DateTime CreatedAt);
```

#### 3. Виправити `AsNoTracking` у read-запитах (рекомендовано перед load тестами)

До збільшення навантаження від SPA — особливо `GET /api/storageincident` з `Include`.

---

### 5.3 JSON-формати відповідей (Reference для Frontend)

#### `POST /api/auth/login` → response:
```json
{ "token": "eyJhbGc..." }
```

#### `GET /api/auth/me` → response:
```json
{
  "id": "68a67b52-cf77-40cb-b94c-9102b9f8fe96",
  "userName": "admin@gmail.com",
  "email": "admin@gmail.com",
  "roles": ["Administrator"]
}
```

#### `GET /api/medicine` → response (приклад одного об'єкта):
```json
{
  "medicineID": 1,
  "name": "Amoxicillin",
  "type": "Antibiotic",
  "expiryDate": "2026-12-31T00:00:00",
  "quantity": 150,
  "category": "Prescription",
  "manufacturer": "Pfizer",
  "batchNumber": "B2024-001",
  "description": "Широкоспектральний антибіотик",
  "minStorageTemp": 2.0,
  "maxStorageTemp": 8.0,
  "storageLocationId": 1,
  "storageLocation": { "locationId": 1, "name": "Холодильник A", ... }
}
```

#### `GET /api/storageincident/active` → response:
```json
[{
  "incidentId": 3,
  "deviceId": 4,
  "ioTDevice": { "deviceID": 4, "location": "Warehouse A", ... },
  "incidentType": "TemperatureViolation",
  "detectedValue": 32.5,
  "expectedMin": 2.0,
  "expectedMax": 8.0,
  "status": "Active",
  "startTime": "2026-04-20T17:00:00Z",
  "endTime": null,
  "createdAt": "2026-04-20T17:00:00Z"
}]
```

#### `GET /api/notification/unread` → response:
```json
[{
  "notificationId": 12,
  "type": "StorageViolation",
  "title": "⚠️ Порушення температури",
  "message": "Температурне порушення на пристрої 4: 32.5°C (норма: 2–8°C)",
  "targetRole": "Administrator",
  "isRead": false,
  "createdAt": "2026-04-20T17:00:00Z",
  "relatedEntityType": "StorageIncident",
  "relatedEntityId": 3
}]
```

---

## Підсумок

| Категорія | Кількість позицій | Критичних |
|---|---|---|
| Технічний борг | 15 | 2 |
| Відсутні API-фіче (для Фази 4) | 1 (Pagination) | 0 |
| Готових ендпоінтів | 34 | — |
| Покрито тестами | 0 | — |

**Найважливіші дії перед Фазою 4:**
1. ✅ Додати CORS policy (виконано)
2. ✅ Виправити `GetExpiringMedicines` (виконано)
3. ✅ Додати `AsNoTracking()` до read-запитів (виконано)
4. ✅ Прибрати Cookie-автентифікацію (виконано)
5. ✅ Видалити або перенести константи з `IoTEmulate/main.cpp` (виконано)

---

*Документ підготовлено за результатами повного аудиту кодової бази станом на 2026-04-20.*  
*Наступне оновлення — після завершення Фази 4 (SPA Frontend).*

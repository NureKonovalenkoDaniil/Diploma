# Рішення щодо безпеки IoT-пристроїв у системі управління медичними препаратами

## 1. Огляд проблем безпеки

Під час аудиту системи виявлено чотири критичні проблеми безпеки:

### 1.1 Вразливість зареєстрованого пристрою (Device Claim Vulnerability)

**Проблема:**  
Поточна реалізація дозволяє будь-якому, хто знає `DeviceId`, викликати `POST /api/iotdevice/claim` і отримати `deviceSecret`. Це означає:
- Не потрібно зберігати жодних секретних ключів на девайсі.
- `DeviceId` часто видимий у логах, UI, документації.
- Зловмисник може "перехопити" будь-який пристрій, розробивши список можливих `DeviceId`.

**Рівень ризику:** ⚠️ **КРИТИЧНИЙ**

**Поточна реалізація (Дипломна):**  
На дипломній фазі система припускає:
- Середовище розробки (localhost).
- Контрольований доступ до реєстрації пристроїв.
- Довіра до мережі та учасників.

---

### 1.2 Слабка політика паролів користувачів (Weak Password Policy)

**Проблема:**  
У [WebApp/MedicationManagement/Program.cs](WebApp/MedicationManagement/Program.cs):
```csharp
options.Password.RequiredLength = 4; // Мінімум 4 символи
// БЕЗ вимог: цифри, великі букви, спеціальні символи
```

Це дозволяє паролі типу: `"1234"`, `"aaaa"`, `"qwer"` — легко підбирати.

**Рівень ризику:** ⚠️ **КРИТИЧНИЙ** (для бази з чутливими даними про лікарські засоби)

---

### 1.3 Збереження облікових даних у конфігураційних файлах (Credentials in appsettings)

**Проблема:**  
У [WebApp/MedicationManagement/appsettings.json](WebApp/MedicationManagement/appsettings.json):
```json
"SmtpSettings": {
  "SmtpPass": "SG.utIITsboTVqiJrfxHBgQqw..."
},
"AdminSeeding": {
  "Email": "admin@medstorage.com",
  "Password": "AdminPassword123!"
}
```

Якщо `appsettings.json` витече (у git-репозиторії, на серверу, в резервній копії):
- Хтось отримує доступ до SMTP-сервера.
- Знає облік адміна за замовчуванням.

**Рівень ризику:** ⚠️ **КРИТИЧНИЙ** (особливо якщо репо публічне)

---

### 1.4 Передбачувані облікові дані адміна (Predictable Default Admin Credentials)

**Проблема:**  
Облік адміна, зареєстрований при першому запуску, має фіксовані облікові дані:
- Email: `admin@medstorage.com`
- Password: `AdminPassword123!`

Якщо не змінити одразу — це входи "за замовчуванням", які легко сканувати в мережі.

**Рівень ризику:** ⚠️ **КРИТИЧНИЙ**

---

## 2. Рішення для дипломної реалізації

### 2.1 Device Provisioning Security (Factory Bootstrap)

**Рекомендоване рішення:**  
Впровадити **Factory Provisioning** (варіант 1 з [IOT_PROVISIONING_SECURITY.md](IOT_PROVISIONING_SECURITY.md)).

**Реалізація на дипломній фазі:**

#### Сервер: Клас IoTDeviceService

```csharp
/// <summary>
/// Claim device secret using DeviceId and BootstrapToken.
/// BootstrapToken is generated during manufacturing/provisioning.
/// </summary>
public async Task<(IoTDevice, string deviceSecret)> ClaimDeviceSecretWithBootstrapAsync(
    string deviceId, 
    string bootstrapToken)
{
    var device = await _context.IoTDevices
        .FirstOrDefaultAsync(d => d.DeviceId == deviceId);
    
    if (device == null)
        throw new InvalidOperationException($"Device {deviceId} not found");
    
    if (!string.IsNullOrEmpty(device.DeviceSecretHash))
        throw new InvalidOperationException($"Device {deviceId} already claimed");
    
    // Verify bootstrap token matches expected value
    if (device.BootstrapToken != bootstrapToken)
        throw new UnauthorizedAccessException("Invalid bootstrap token");
    
    // Generate new device secret
    var secret = GenerateSecureRandomString(32);
    device.DeviceSecretHash = _passwordHasher.HashPassword(device, secret);
    device.BootstrapToken = null; // Clear bootstrap token after use
    
    await _context.SaveChangesAsync();
    
    return (device, secret);
}
```

#### Контролер: IoTDeviceController

```csharp
[HttpPost("claim-with-bootstrap")]
[AllowAnonymous]
public async Task<ActionResult<object>> ClaimWithBootstrap(
    [FromBody] DeviceClaimBootstrapDto dto)
{
    try
    {
        var (device, secret) = await _serviceIoTDevice
            .ClaimDeviceSecretWithBootstrapAsync(dto.DeviceId, dto.BootstrapToken);
        
        _serviceAuditLog.LogAction(
            userId: null,
            action: "Device provisioned via factory bootstrap",
            resourceType: "IoTDevice",
            resourceId: device.Id.ToString(),
            details: $"DeviceId: {device.DeviceId}");
        
        return Ok(new { device, deviceSecret = secret });
    }
    catch (Exception ex)
    {
        _logger.LogError($"Claim failed: {ex.Message}");
        return BadRequest(new { error = ex.Message });
    }
}
```

#### DTO: DeviceClaimBootstrapDto

```csharp
public class DeviceClaimBootstrapDto
{
    [Required]
    public string DeviceId { get; set; }
    
    [Required]
    public string BootstrapToken { get; set; }
}
```

#### Емулятор: config.h

```cpp
// Device identity
#define DEVICE_ID "WOKWI-SENSOR-A1"

// Bootstrap token (pre-provisioned during "manufacturing")
#define BOOTSTRAP_TOKEN "ABCD-1234-EFGH-5678"

// Device secret (initially empty, claimed on first run)
#define DEVICE_SECRET ""
```

#### Емулятор: main.cpp (фрагмент)

```cpp
void claimDeviceSecretWithBootstrap() {
    if (!WiFi.isConnected()) {
        Serial.println("[CLAIM] WiFi not connected");
        return;
    }
    
    HTTPClient http;
    String url = String(SERVER_BASE_URL) + "/api/iotdevice/claim-with-bootstrap";
    
    http.begin(url);
    http.addHeader("Content-Type", "application/json");
    
    DynamicJsonDocument doc(256);
    doc["deviceId"] = DEVICE_ID;
    doc["bootstrapToken"] = BOOTSTRAP_TOKEN;
    
    String payload;
    serializeJson(doc, payload);
    
    int httpCode = http.POST(payload);
    
    if (httpCode == 200) {
        DynamicJsonDocument response(1024);
        deserializeJson(response, http.getString());
        
        String secret = response["deviceSecret"].as<String>();
        
        // Store secret in NVS
        preferences.putString("device_secret", secret);
        
        Serial.println("[CLAIM] Secret obtained and stored");
    } else {
        Serial.printf("[CLAIM] Failed: %d\n", httpCode);
    }
    
    http.end();
}

void setup() {
    // ... WiFi connection ...
    
    // Check if device already has secret
    String storedSecret = preferences.getString("device_secret", "");
    if (storedSecret.isEmpty()) {
        Serial.println("[SETUP] No stored secret, initiating claim");
        claimDeviceSecretWithBootstrap();
    }
}
```

**Переваги:**
- ✅ Немає ручного введення кодів при розгортанні.
- ✅ Bootstrap token "одноразовий" (очищується після claim).
- ✅ DeviceId + BootstrapToken складніше перехопити.
- ✅ Масштабується для тисяч пристроїв.

**На захисті:**
- Показати, що `BootstrapToken` — це "вшита" у прошивку константа, сгенерована при виробництві.
- Розповісти, як в реальному продакшені це було б частиною CI/CD для кожного пристрою.

---

### 2.2 Посилення політики паролів (Password Policy Hardening)

**Рішення:**

Оновити [WebApp/MedicationManagement/Program.cs](WebApp/MedicationManagement/Program.cs):

```csharp
var builder = WebApplicationBuilder.CreateBuilder(args);

// ... інші налаштування ...

// Identity Password Options
builder.Services.Configure<IdentityOptions>(options =>
{
    options.Password.RequiredLength = 8;  // ↑ Мінімум 8 символів
    options.Password.RequireDigit = true;  // Вимагати цифру (0-9)
    options.Password.RequireUppercase = true;  // Вимагати велику букву
    options.Password.RequireLowercase = true;  // Вимагати малу букву
    options.Password.RequireNonAlphanumeric = true;  // Вимагати спецсимвол (!@#$%^&*)
    
    // Lockout policy: брутфорс-захист
    options.Lockout.DefaultLockoutTimeSpan = TimeSpan.FromMinutes(15);
    options.Lockout.MaxFailedAccessAttempts = 5;
    options.Lockout.AllowedForNewUsers = true;
});
```

**Приклади коректних паролів:**
- ✅ `"MedSecure@2025"` (8+ символів, цифра, велика/мала букви, спецсимвол)
- ✅ `"LikelyTo$Pass123"`

**Приклади відхилених паролів:**
- ❌ `"1234"` (занадто короткий)
- ❌ `"password"` (мало цифр/спецсимволів)
- ❌ `"12345678"` (немає букв)

**На захисті:**
- "PasswordPolicy посилена до промислового стандарту (8+, цифра, великі букви, спецсимвол)".
- Показати `IdentityOptions.Lockout` для захисту від брутфорсу.

---

### 2.3 Управління обліковими даними (Secrets Management)

**Рішення для дипломної фази:**

#### Крок 1: Відділити конфігурацію від секретів

Оновити `appsettings.json` (зберігати в VCS):
```json
{
  "ConnectionStrings": {
    "MedicineStorageDB": "Server=.;Database=MedicationManagementDB;Trusted_Connection=true;"
  },
  "SmtpSettings": {
    "Server": "smtp.sendgrid.net",
    "Port": 587,
    "SmtpUser": "apikey",
    "SmtpPass": "${SMTP_PASSWORD}"  // Placeholder
  },
  "AdminSeeding": {
    "Email": "admin@medstorage.com",
    "Password": "${ADMIN_PASSWORD}"  // Placeholder
  }
}
```

Створити `appsettings.Development.json` (НЕ в VCS):
```json
{
  "SmtpSettings": {
    "SmtpPass": "SG.utIITsboTVqiJrfxHBgQqw..."
  },
  "AdminSeeding": {
    "Password": "SecureAdminPass123!"
  }
}
```

#### Крок 2: User Secrets (для локальної розробки)

```bash
# У папці WebApp/MedicationManagement/:
dotnet user-secrets init
dotnet user-secrets set "Jwt:SecretKey" "your-very-long-secret-key-here-min-32-chars"
dotnet user-secrets set "SmtpSettings:SmtpPass" "SG.utIITsboTVqiJrfxHBgQqw..."
dotnet user-secrets set "AdminSeeding:Password" "SecureAdminPass123!"
```

#### Крок 3: Оновити Program.cs для читання секретів

```csharp
var builder = WebApplicationBuilder.CreateBuilder(args);

// User Secrets для Development
if (builder.Environment.IsDevelopment())
{
    builder.Configuration.AddUserSecrets<Program>();
}

// Альтернатива: читати з environment variables
var smtpPass = builder.Configuration["SmtpSettings:SmtpPass"] 
    ?? Environment.GetEnvironmentVariable("SMTP_PASSWORD");

var adminPass = builder.Configuration["AdminSeeding:Password"]
    ?? Environment.GetEnvironmentVariable("ADMIN_PASSWORD");

// ... передати у сервісів ...
```

#### Крок 4: .gitignore (забезпечити, що секрети не витікають)

```
# User Secrets
[Uu]ser[Ss]ecrets/
*.db
*.log

# appsettings з локальними даними
appsettings.Development.json
appsettings.Production.json

# IDE
.vs/
.vscode/
obj/
bin/
```

**На захисті:**
- "Облікові дані зберігаються у User Secrets або environment variables, не у VCS".
- Показати `.gitignore` й пояснити CI/CD pipeline, як передаються секрети на сервер.

---

### 2.4 Обов'язкова зміна облікового облік адміна при першому запуску (Forced Admin Password Change)

**Рішення:**

Додати `InitialSetupController` або флаг у `AuthController`:

```csharp
[ApiController]
[Route("api/[controller]")]
public class InitialSetupController : ControllerBase
{
    private readonly IServiceUser _serviceUser;
    private readonly IConfiguration _configuration;
    private readonly ILogger<InitialSetupController> _logger;
    
    public InitialSetupController(
        IServiceUser serviceUser,
        IConfiguration configuration,
        ILogger<InitialSetupController> logger)
    {
        _serviceUser = serviceUser;
        _configuration = configuration;
        _logger = logger;
    }
    
    [HttpPost("change-default-admin-password")]
    [AllowAnonymous]
    public async Task<ActionResult> ChangeDefaultAdminPassword(
        [FromBody] ChangeAdminPasswordDto dto)
    {
        var adminEmail = _configuration["AdminSeeding:Email"];
        var user = await _serviceUser.FindUserByEmailAsync(adminEmail);
        
        if (user == null)
            return NotFound("Admin user not found");
        
        // Перевірити, що передається старий (дефолтний) пароль
        var result = await _serviceUser.ChangePasswordAsync(
            user.Id,
            _configuration["AdminSeeding:Password"],
            dto.NewPassword);
        
        if (!result.Succeeded)
            return BadRequest(new { errors = result.Errors });
        
        _logger.LogInformation($"Default admin password changed for {adminEmail}");
        
        return Ok(new { message = "Admin password changed successfully" });
    }
}

public class ChangeAdminPasswordDto
{
    [Required]
    [StringLength(100, MinimumLength = 8)]
    public string NewPassword { get; set; }
}
```

**На захисті:**
- "При першому запуску адміністратор змушений змінити дефолтний пароль перед першим входом".
- Можна показати сторінку "Initial Setup" у фронтенді, яка блокує доступ до панелі управління, поки пароль не змінено.

---

## 3. Архітектурні альтернативи для production

| **Варіант** | **Безпека** | **Складність** | **Масштабованість** | **Для диплома** |
|---|---|---|---|---|
| **Factory Bootstrap** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ✅ Рекомендовано |
| **Claim Token** | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ✅ Альтернатива |
| **Claim Window** | ⭐⭐⭐ | ⭐ | ⭐⭐ | ❌ Невідповідна |
| **Public Key Infrastructure (PKI)** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⏳ Для подальшого |

Детальніше див. [IOT_PROVISIONING_SECURITY.md](IOT_PROVISIONING_SECURITY.md).

---

## 4. Впровадження по етапах

### Фаза 1 (Поточна дипломна): Базова безпека
- ✅ Factory Bootstrap для provisioning.
- ✅ Посилена password policy (8+ символів, цифра, велика буква, спецсимвол).
- ✅ User Secrets для локального зберігання облікових даних.
- ✅ Обов'язкова зміна дефолтного паролю адміна.

### Фаза 2 (Вдосконалення дипломної): Моніторинг
- Device claim audit logging (хто коли заявив пристрій).
- Failed login attempt tracking.
- Anomaly detection (багато claim-спроб однієї IP).

### Фаза 3 (Production): Розширена безпека
- TLS certificate validation для устройств.
- Mutual TLS (mTLS) для device-server комунікації.
- Rate limiting на `/api/iotdevice/claim`.
- IP-whitelisting для device endpoints.
- HSM (Hardware Security Module) для зберігання master secrets.

---

## 5. Контрольний список для реалізації

- [ ] Реалізувати Factory Bootstrap у IoTDeviceService.
- [ ] Додати BootstrapToken у модель IoTDevice (EF Core migration).
- [ ] Оновити контролер IoTDeviceController.
- [ ] Посилити password policy у Program.cs.
- [ ] Налаштувати User Secrets (dotnet user-secrets).
- [ ] Обновити appsettings.json (placeholders).
- [ ] Додати InitialSetupController для зміни дефолтного паролю.
- [ ] Оновити емулятор (BOOTSTRAP_TOKEN у config.h).
- [ ] Написати документацію у README.
- [ ] Протестувати flow: register → claim → device-login → fetch data.
- [ ] На захисті розповісти про security decisions.

---

## 6. Посилання на код

- [WebApp/MedicationManagement/Services/ServiceIoTDevice.cs](../WebApp/MedicationManagement/Services/ServiceIoTDevice.cs) — Device provisioning logic.
- [WebApp/MedicationManagement/Controllers/IoTDeviceController.cs](../WebApp/MedicationManagement/Controllers/IoTDeviceController.cs) — API endpoints.
- [WebApp/MedicationManagement/Models/IoTDevice.cs](../WebApp/MedicationManagement/Models/IoTDevice.cs) — Device model.
- [WebApp/MedicationManagement/Program.cs](../WebApp/MedicationManagement/Program.cs) — DI & password options.
- [IoTEmulate/src/config.h](../IoTEmulate/src/config.h) — Device configuration.
- [IoTEmulate/src/main.cpp](../IoTEmulate/src/main.cpp) — Provisioning flow.
- [IOT_PROVISIONING_SECURITY.md](IOT_PROVISIONING_SECURITY.md) — Детальні варіанти рішень.

---

## 7. Висновок

Дипломна реалізація забезпечує:
- **Безпека:** Factory Bootstrap +强ila password policy + secrets management.
- **Простота:** Один endpoint для claim, автоматичне отримання secret.
- **Реалізм:** Архітектура готова до масштабування на production.
- **Захист роботи:** Демонстрація усвідомленого підходу до IoT security.

---

**Статус:** Документація створена. Готова до імплементації.  
**Дата:** 2025  
**Автор:** Дипломний проєкт

# AUDIT_AND_DIPLOMA_PLAN.md
> Технічний аудит і план розвитку дипломного проєкту  
> Дата аудиту: 2026-04-09  
> Проведено: Antigravity AI  

---

# 1. Короткий загальний висновок

## Що це за проєкт зараз

Це курсова робота, яка реалізує базову інформаційну систему управління медичними препаратами на стеку **ASP.NET Core 8 / SQL Server / EF Core / JWT + Identity**. До складу входять:

- backend API (5 контролерів, 4 сервіси, 2 фонові служби);
- web frontend у `wwwroot` (10 HTML-файлів + 11 JS-файлів, Bootstrap 5);
- мобільний застосунок на Kotlin/Android Compose;
- IoT-емуляція на ESP32 + DHT22 у середовищі Wokwi (PlatformIO / C++);
- навантажувальні тести на NBomber (GET і POST).

## Наскільки проєкт придатний як основа диплома

**Придатний з суттєвими доповненнями.** Технічний каркас правильний і реалізований на сучасному стеку. Але предметна модель занадто проста для дипломного рівня: відсутні ключові для теми сутності (StorageLocation, StorageIncident, MedicineLifecycleEvent, Notification), немає нормального зв'язку між Medicine і StorageCondition, жорстко зашиті токени і URL, відсутні unit / integration-тести, frontend морально застарів і вбудований у backend.

## Головні сильні сторони

1. Правильна базова архітектура: DI, інтерфейси сервісів, окремі контролери.
2. JWT-авторизація та ролева модель (Administrator / User) реально працюють.
3. Журнал аудиту (AuditLog) присутній і використовується у всіх операціях.
4. Два фонові сервіси (ExpiryNotificationService, StorageConditionMonitoringService) — патерн BackgroundService правильний.
5. IoT-емуляція на реальній платі ESP32 у Wokwi, з реальним HTTP POST до API та локальним buzzer-алармом.
6. Мобільний застосунок Android (Kotlin + Compose) є і покриває основні екрани.
7. Навантажувальні тести (NBomber GET + POST) вже написані.
8. Два DbContext (розподіл Identity і основних даних) — правильне рішення.

## Головні слабкі сторони

1. Предметна модель неповна: Medicine не пов'язана з StorageCondition, IoTDevice не прив'язаний до місця зберігання, відсутні StorageLocation і StorageIncident.
2. Жорстко зашиті секрети: JWT-ключ і JWT-токени прямо в коді (appsettings.json, main.cpp, тести).
3. Токен зберігається в localStorage — XSS-вразливість.
4. Frontend вбудований у backend (wwwroot): немає окремого SPA-проєкту.
5. Ніяких тестів (unit / integration).
6. StorageConditionController — не захищений JWT! [Authorize] відсутній на рівні класу.
7. Перший зареєстрований користувач = Administrator — небезпечна логіка.
8. Термін JWT = 1 рік (рядок 179 AuthController): DateTime.UtcNow.AddYears(1) — критично.
9. IoT-код: температура і вологість захардкоджені (float temperature = 3; float humidity = 30;) — датчик DHT22 не читається реально.
10. Відсутній docker-compose.

---

# 2. Загальна структура проєкту

Diploma/
  AGENTS.md
  PROJECT_CONTEXT.md
  AUDIT_AND_DIPLOMA_PLAN.md
  WebApp/
    MedicationManagement/       <- Backend ASP.NET Core 8
      Controllers/              <- 5 API-контролерів
      Services/                 <- 4 сервіси з інтерфейсами
      BackgroundServices/       <- 2 фонові служби
      Models/                   <- 7 моделей / DTO
      DBContext/                <- 2 DbContext (основний + Identity)
      Migrations/               <- EF Core міграції (2 набори)
      wwwroot/                  <- Вбудований HTML/JS frontend (10 сторінок)
      Program.cs
      appsettings.json
    LoadTest.GET/               <- NBomber GET тест
    LoadTest.POST/              <- NBomber POST тест
  Mobile/
    MedicationManagement/       <- Android Kotlin + Compose (26 Kotlin-файлів)
  IoTEmulate/                   <- ESP32/Wokwi IoT-емуляція (C++/PlatformIO)
    src/main.cpp
    diagram.json
    platformio.ini

---

# 3. Детальний аудит по модулях

## 3.1 Backend (ASP.NET Core 8)

Файли: WebApp/MedicationManagement/

Що вже реалізовано:
- DI для всіх сервісів
- JWT Bearer authentication
- ASP.NET Identity для управління користувачами
- Swagger/OpenAPI (Swashbuckle 7.1.0)
- JSON Patch для часткового оновлення (PATCH endpoints)
- Статична роздача файлів (UseStaticFiles)

Що реалізовано частково або слабо:
- StorageConditionController не має [Authorize] на рівні класу
- GenerateJwtToken використовує .Result замість await
- ReplenishmentRecommendation — логіка рекомендацій примітивна: 100 - quantity
- Немає DTO-рівня: контролери приймають і повертають entity-моделі напряму

Що відсутнє:
- StorageLocation, StorageIncident, MedicineLifecycleEvent, Notification
- Зв'язок Medicine із StorageLocation
- Unit / integration тести, docker-compose

Висновок: частково переробити.

## 3.2 Database / Models / Entities / Migrations

Поточні сутності:

| Сутність | Поля | Проблеми |
|---|---|---|
| Medicine | MedicineID, Name, Type, ExpiryDate, Quantity, Category | Немає FK до StorageLocation, Manufacturer, BatchNumber |
| StorageCondition | ConditionID, Temperature, Humidity, Timestamp, DeviceID | Немає FK до Medicine або StorageLocation |
| IoTDevice | DeviceID, Location(string), Type, Parameters, IsActive, Min/Max Temp/Humidity | Location — рядок, не FK |
| AuditLog | Id, Action, User, Timestamp, Details | Немає EntityType, EntityId, Severity |
| ReplenishmentRecommendation | MedicineId, MedicineName, RecommendedQuantity | Не сутність БД — тільки DTO |

Два DbContext: MedicineStorageContext і UserContext — правильне рішення, залишити.

Чого не вистачає: StorageLocations, StorageIncidents, MedicineLifecycleEvents, Notifications.

Висновок: переробити схему БД, додати 4 нові таблиці.

## 3.3 Controllers / API

| Контролер | Endpoints | Авторизація | Стан |
|---|---|---|---|
| AuthController | POST register, login, create-role, assign-role | Частково | Баги: термін токена 1 рік |
| MedicineController | GET, GET{id}, POST, PATCH{id}, DELETE{id}, low-stock, expiring, replenishment | JWT + Role | Добре |
| StorageConditionController | GET, GET{id}, POST, PATCH{id}, DELETE{id}, checkCondition | НЕ ЗАХИЩЕНИЙ | КРИТИЧНО |
| IoTDeviceController | GET, GET{id}, POST, PATCH{id}, DELETE{id}, setstatus, conditions/{id} | JWT + Role | Добре |
| AuditLogController | GET (filters: from, to, user, action) | Administrator only | Добре |

Відсутні: StorageLocationController, StorageIncidentController, MedicineLifecycleController, NotificationController, GET /api/auth/me

Висновок: доробити.

## 3.4 Services / Business Logic

| Сервіс | Інтерфейс | Проблеми |
|---|---|---|
| ServiceMedicine | IServiceMedicine | RecommendedQuantity = 100 - quantity — hardcoded |
| ServiceStorageCondition | IServiceStorageCondition | CheckConditionsForAllDevices повертає List<string> |
| ServiceIoTDevice | IServiceIoTDevice | — |
| ServiceAuditLog | IServiceAuditLog | — |

Відсутні: ServiceStorageLocation, ServiceStorageIncident, ServiceMedicineLifecycle, ServiceNotification.

Висновок: частково переробити.

## 3.5 Authentication / Authorization / Roles

Критичні проблеми:
1. Термін JWT-токена — 1 рік (DateTime.UtcNow.AddYears(1), рядок 179), незважаючи на "ExpireDays": 30 у appsettings.json
2. JWT-ключ у appsettings.json відкритим текстом: "Key": "A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6"
3. Перший зареєстрований = Administrator (usersCount == 1) — race condition і неявна семантика
4. Немає Refresh Token
5. Токен у localStorage (XSS)
6. GenerateJwtToken використовує .Result замість await
7. Немає /api/auth/me

Висновок: частково переробити.

## 3.6 Audit / Logging

Що є: таблиця AuditLogs, сервіс LogAction(action, user, details, isSensor), контролер із фільтрацією.

Проблеми:
- Немає EntityType і EntityId — нема прив'язки до конкретних об'єктів
- Немає Severity
- Update-операції не покриті аудитом
- Немає пагінації

Висновок: доробити.

## 3.7 Background Services

| Служба | Інтервал | Проблема |
|---|---|---|
| ExpiryNotificationService | 1 день | Тільки логує, не зберігає Notification entity |
| StorageConditionMonitoringService | 5 секунд | Тільки логує, не створює StorageIncident; 5 сек = 17000 записів/добу при порушенні |

Немає debounce/dedup-логіки.

Висновок: переробити.

## 3.8 IoT / Sensor Integration / Emulation

Платформа: ESP32 DevKit C v4 + DHT22 + buzzer (Wokwi + PlatformIO + C++)

Що є:
- HTTP GET до /api/iotdevice/{id} для отримання порогових значень
- HTTP POST до /api/storagecondition для передачі вимірювань
- JWT-автентифікація у запитах
- Buzzer-сигнал при порушенні

КРИТИЧНІ ПРОБЛЕМИ:
1. Рядки 150-151 і 172-173: float temperature = 3; float humidity = 30; — DHT22 НЕ ЧИТАЄТЬСЯ!
2. JWT-токен захардкоджений у main.cpp рядок 22
3. URL захардкоджений: http://192.168.100.2:5000
4. deviceID = 4 захардкоджений

Висновок: доробити (виправити DHT22, параметризувати URL і deviceID).

## 3.9 Web Frontend

10 HTML + 11 JS-файлів, Bootstrap 5.3.6 (CDN), Vanilla JS.

Покриває: CRUD Medicine, IoT, Storage, Audit, Notifications.

Проблеми:
- Вбудований у backend (wwwroot) — немає окремого SPA
- Токен у localStorage (XSS)
- Немає CSS-дизайн-системи
- Немає сторінок для StorageLocation, StorageIncident, MedicineLifecycleEvent
- Немає графіків (charts)
- Немає адмін-панелі

Висновок: повністю перенести у окремий SPA.

## 3.10 Mobile App

26 Kotlin-файлів: 9 Activity, 5 Fragment, 4 Adapter, 4 Model, 3 Theme.

Покриває: CRUD Medicine, IoT-пристрої, умови зберігання, аудит-лог.

Невідомо (не читав код): чи є Retrofit, обробка помилок, hardcoded URL.

Відсутні екрани: StorageIncidents, StorageLocation, MedicineLifecycle.

Висновок: частково переробити.

## 3.11 Load Tests

NBomber: GET (50 паралельних, 15 сек), POST (10 паралельних, 20 сек).

КРИТИЧНО: JWT-токен і URL захардкоджені у файлах коду.
Покриття: тільки Medicine.

Висновок: доробити.

## 3.12 Конфігурація

JWT-ключ відкритим текстом в appsettings.json.
ExpireDays: 30 є, але не використовується (AddYears(1)).
Немає docker-compose.

Пакети (до речі, всі актуальні для .NET 8).

Висновок: доробити.

## 3.13 Тести

LoadTest (NBomber): є.
Unit тести backend: ВІДСУТНІ.
Integration тести: ВІДСУТНІ.
Android тести: папки є, вміст невідомий.

Висновок: критична прогалина.

## 3.14 Документація

Є: AGENTS.md, PROJECT_CONTEXT.md, Swagger у Development.
Відсутнє: README.md, IMPLEMENTATION_ROADMAP.md, ER-діаграма, C4-діаграма.

Висновок: додати.

---

# 4. Що вже є в курсовому проєкті і можна використати в дипломі

1. ASP.NET Core 8 backend — технологія і загальна архітектура
2. JWT + ASP.NET Identity — реально працює, ролі є
3. Entity Framework Core + SQL Server + 2 DbContext
4. AuditLog сутність і ServiceAuditLog
5. ExpiryNotificationService і StorageConditionMonitoringService — патерн правильний
6. ServiceMedicine, ServiceStorageCondition, ServiceIoTDevice — CRUD-логіка
7. MedicineController, IoTDeviceController, AuditLogController — структура
8. IoT-схема Wokwi + PlatformIO + fetchDeviceConfig()
9. NBomber навантажувальні тести
10. Android-застосунок: структура Activity/Fragment/Adapter/Model

---

# 5. Що вже є, але потребує переробки

| Компонент | Що конкретно змінити |
|---|---|
| AuthController.GenerateJwtToken | Замінити .Result на await, читати ExpireDays з config |
| StorageConditionController | Додати [Authorize] або API-ключ для IoT |
| AuditLog entity | Додати EntityType, EntityId, Severity + міграція |
| ExpiryNotificationService | Зберігати Notification entity |
| StorageConditionMonitoringService | Зберігати StorageIncident + Notification, збільшити інтервал, debounce |
| IoTEmulate/src/main.cpp (рядки 150-151, 172-173) | dht.readTemperature() / dht.readHumidity() замість hardcode |
| IoTEmulate/src/main.cpp (рядки 20-22) | Параметризувати URL і JWT-токен |
| LoadTest.GET/POST | Перенести токен і URL у конфіг |
| appsettings.json | Перенести JWT-ключ у User Secrets або env |

---

# 6. Чого не вистачає для дипломної роботи

## 6.1 Нові сутності (обов'язково)

- StorageLocation — місце зберігання (назва, адреса, тип, FK до IoTDevice)
- StorageIncident — зафіксований факт порушення умов (DeviceID, время, тип, значення, статус)
- MedicineLifecycleEvent — подія в житті препарату (Received / Issued / Moved / Expired / Disposed)
- Notification — сповіщення системи (тип, текст, роль-отримувач, статус прочитання)
- Оновлений Medicine: Manufacturer, BatchNumber, Description, MinStorageTemp, MaxStorageTemp, StorageLocationId (FK)
- Оновлений AuditLog: EntityType, EntityId, Severity

## 6.2 Нові API-ендпоінти

- GET/POST/PUT/DELETE /api/storagelocation
- GET/POST /api/storageincident, PATCH /api/storageincident/{id}/resolve
- GET /api/medicine/{id}/lifecycle, POST /api/medicine/{id}/lifecycle
- GET /api/notification, PATCH /api/notification/{id}/read
- GET /api/auth/me, GET /api/auth/users (для адміна)

## 6.3 Новий frontend

- Окремий SPA (Vue.js або React) у Frontend/
- Нові сторінки: StorageLocations, StorageIncidents, MedicineLifecycle, Notifications
- Dashboard із графіками (Chart.js) температури / вологості у часі
- Адмін-панель управління користувачами

## 6.4 Тести

- Unit-тести: ServiceMedicine, ServiceStorageCondition, ServiceStorageIncident (xUnit)
- Integration-тести: MedicineController, StorageIncidentController (WebApplicationFactory)
- Розширення LoadTest

## 6.5 DevOps / Документація

- docker-compose.yml (backend + SQL Server)
- README.md
- ER-діаграма бази даних
- C4-діаграма системи
- IMPLEMENTATION_ROADMAP.md

---

# 7. Пропозиція цільової архітектури дипломного проєкту

Diploma/
  WebApp/
    MedicationManagement/         <- ASP.NET Core 8 API (тільки backend)
      Controllers/                <- + StorageLocationController, StorageIncidentController
                                  <- + MedicineLifecycleController, NotificationController
      Services/                   <- + ServiceStorageLocation, ServiceStorageIncident
                                  <- + ServiceMedicineLifecycle, ServiceNotification
      Models/                     <- + StorageLocation, StorageIncident
                                  <- + MedicineLifecycleEvent, Notification
                                  <- + оновлений Medicine, оновлений AuditLog
      BackgroundServices/         <- Оновлені: StorageIncident + Notification
      DBContext/                  <- Оновлений MedicineStorageContext
      Migrations/                 <- Нові EF Core міграції
  Frontend/                       <- [НОВИЙ] Окремий SPA (Vue.js або React)
    src/
      pages/
      components/
      api/
    package.json
  Mobile/
    MedicationManagement/         <- Kotlin + Compose (нові екрани)
  IoTEmulate/                     <- ESP32 + Wokwi (виправлений)
  Tests/                          <- [НОВИЙ] xUnit + integration tests
    MedicationManagement.Tests/
  docker-compose.yml              <- [НОВИЙ]
  README.md                       <- [НОВИЙ]
  IMPLEMENTATION_ROADMAP.md       <- [НОВИЙ]

Розподіл процесів:
- ASP.NET Core API: порт 5000/7069
- Frontend SPA: порт 3000/5173, спілкується з API через HTTP
- Mobile: Android APK, спілкується з тим же API
- IoT: ESP32 Wokwi через HTTP POST до API
- SQL Server: окремий контейнер у docker-compose

---

# 8. Пропозиція нових сутностей і зв'язків

Medicine (ДОПОВНИТИ):
  + Manufacturer, BatchNumber, Description
  + MinStorageTemp, MaxStorageTemp (float)
  + StorageLocationId (FK -> StorageLocation, nullable)

StorageLocation (NEW):
  LocationID (PK), Name, Address
  LocationType (Refrigerator / Shelf / Vault)
  IoTDeviceId (FK -> IoTDevice, nullable)

StorageIncident (NEW):
  IncidentID (PK)
  DeviceID (FK -> IoTDevice)
  LocationID (FK -> StorageLocation, nullable)
  IncidentType (TemperatureViolation / HumidityViolation)
  DetectedValue (float), ExpectedMin, ExpectedMax (float)
  StartTime, EndTime (DateTime nullable)
  Status (Active / Resolved)
  CreatedAt

MedicineLifecycleEvent (NEW):
  EventID (PK)
  MedicineID (FK -> Medicine)
  EventType (Received / Issued / Moved / Expired / Disposed / Recalled)
  Description, Quantity (nullable)
  PerformedBy (email), PerformedAt
  RelatedLocationId (FK -> StorageLocation, nullable)

Notification (NEW):
  NotificationID (PK)
  Type (Expiry / LowStock / StorageViolation / IncidentCreated)
  Title, Message
  TargetRole (Administrator / User / All)
  IsRead (bool), CreatedAt
  RelatedEntityType, RelatedEntityId (nullable)

AuditLog (ДОПОВНИТИ):
  + EntityType (Medicine / IoTDevice / StorageCondition / StorageIncident)
  + EntityId (int, nullable)
  + Severity (Info / Warning / Critical)

Нові зв'язки:
  Medicine -> StorageLocation (N:1, nullable FK)
  StorageLocation -> IoTDevice (1:1, nullable FK)
  StorageCondition -> IoTDevice (N:1) — вже є
  StorageIncident -> IoTDevice (N:1)
  StorageIncident -> StorageLocation (N:1, nullable)
  MedicineLifecycleEvent -> Medicine (N:1)
  MedicineLifecycleEvent -> StorageLocation (N:1, nullable)

---

# 9. Рекомендований порядок розробки

## Фаза 1 — Виправлення критичних проблем (1-2 дні)
1. Виправити GenerateJwtToken: .Result → await, читати ExpireDays з config
2. Перенести JWT-ключ у User Secrets / env
3. Вирішити авторизацію StorageConditionController ([Authorize] або API-ключ)
4. Виправити main.cpp рядки 150-151, 172-173: dht.readTemperature() / dht.readHumidity()

## Фаза 2 — Розширення предметної моделі (3-5 днів)
5. StorageLocation entity + міграція + Controller + Service
6. Medicine: нові поля + FK до StorageLocation + міграція
7. AuditLog: EntityType, EntityId, Severity + міграція
8. StorageIncident entity + міграція + Controller + Service
9. MedicineLifecycleEvent entity + міграція + Controller
10. Notification entity + міграція + Controller

## Фаза 3 — Рефакторинг Background Services (1-2 дні)
11. StorageConditionMonitoringService: StorageIncident + Notification, debounce, інтервал 30-60 сек
12. ExpiryNotificationService: зберігати Notification entity

## Фаза 4 — Новий Frontend SPA (5-7 днів)
13. Ініціалізувати Vue.js або React проєкт у Frontend/
14. Перенести існуючі сторінки у компоненти
15. Нові сторінки: StorageLocation, StorageIncident, MedicineLifecycle, Notification
16. Графіки Temperature/Humidity (Chart.js)
17. Адмін-панель для управління користувачами

## Фаза 5 — Мобільний застосунок (2-3 дні)
18. Перевірити якість поточного коду (Retrofit, обробка помилок, URL)
19. Нові екрани: StorageIncidents, MedicineLifecycle

## Фаза 6 — Тести (2-3 дні)
20. Проєкт Tests/MedicationManagement.Tests/ (xUnit)
21. Unit-тести: ServiceMedicine, ServiceStorageCondition, ServiceStorageIncident
22. Integration-тести через WebApplicationFactory

## Фаза 7 — DevOps та документація (1-2 дні)
23. docker-compose.yml
24. README.md
25. ER-діаграма, C4-діаграма
26. IMPLEMENTATION_ROADMAP.md

---

# 10. Підсумкова таблиця

| Компонент | Поточний стан | Дія |
|---|---|---|
| ASP.NET Core 8 backend (каркас) | Є, працює | Залишити і розширити |
| JWT + ASP.NET Identity | Є, є критичні баги | Доробити / виправити |
| Medicine entity + CRUD | Є, неповна модель | Доробити |
| StorageCondition entity + CRUD | Є | Залишити |
| IoTDevice entity + CRUD | Є | Залишити |
| AuditLog entity + service | Є, спрощений | Доробити |
| ExpiryNotificationService | Є, тільки логує | Доробити |
| StorageConditionMonitoringService | Є, 5 сек, тільки логує | Переробити |
| StorageConditionController безпека | КРИТИЧНА ПРОБЛЕМА | Виправити |
| StorageLocation | Відсутній | Додати |
| StorageIncident | Відсутній | Додати |
| MedicineLifecycleEvent | Відсутній | Додати |
| Notification entity | Відсутній | Додати |
| StorageLocationController + service | Відсутні | Додати |
| StorageIncidentController + service | Відсутні | Додати |
| MedicineLifecycleController + service | Відсутні | Додати |
| NotificationController + service | Відсутні | Додати |
| GET /api/auth/me | Відсутній | Додати |
| Web frontend (wwwroot) | Є, Bootstrap + Vanilla JS | Перенести у SPA |
| Frontend SPA (Frontend/) | Відсутній | Додати (Vue.js або React) |
| Android Mobile (Kotlin+Compose) | Є, 26 файлів | Доробити (нові екрани) |
| IoT (ESP32 + Wokwi) | Є, DHT22 НЕ ЧИТАЄТЬСЯ | Доробити |
| LoadTest GET/POST (NBomber) | Є, hardcoded токен і URL | Доробити |
| Unit / Integration тести backend | Відсутні | Додати (обов'язково) |
| docker-compose.yml | Відсутній | Додати |
| README.md | Відсутній | Додати |
| ER-діаграма, C4-діаграма | Відсутні | Додати |
| IMPLEMENTATION_ROADMAP.md | Відсутній | Додати |

---

*Документ підготовлено за результатами повного технічного аудиту workspace станом на 2026-04-09.*  
*Наступне оновлення — після завершення Фази 1 (виправлення критичних проблем).*

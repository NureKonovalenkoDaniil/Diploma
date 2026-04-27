# PROJECT_CONTEXT.md

## 1. Короткий опис проєкту

Цей проєкт початково був створений як курсова робота, пов’язана з управлінням медичними препаратами, моніторингом умов їх зберігання та використанням IoT-пристроїв / датчиків.

Зараз проєкт розглядається як основа дипломної роботи на тему:

«Розподілена інформаційна система управління життєвим циклом та безпекою зберігання медичних препаратів»

## 2. Поточна підтверджена архітектура (після аудиту 2026-04-09)

**Backend:** ASP.NET Core 8 / .NET 8  
**Database:** SQL Server  
**ORM:** Entity Framework Core 8.0.11  
**Authentication:** JWT Bearer + ASP.NET Identity (2 DbContext: MedicineStorageContext + UserContext)  
**API:** REST API (5 контролерів: AuthController, MedicineController, StorageConditionController, IoTDeviceController, AuditLogController)  
**Services:** 4 сервіси з інтерфейсами: IServiceMedicine, IServiceStorageCondition, IServiceIoTDevice, IServiceAuditLog  
**Background Services:** ExpiryNotificationService (1 день), StorageConditionMonitoringService (5 сек)  
**Web frontend:** wwwroot: 10 HTML + 11 JS (Bootstrap 5.3.6 CDN, Vanilla JS) — вбудований у backend  
**Mobile:** Android Kotlin + Jetpack Compose, 26 Kotlin-файлів (9 Activity, 5 Fragment, 4 Adapter, 4 Model, 3 Theme)  
**IoT:** ESP32 DevKit C v4 + DHT22 + buzzer у Wokwi (PlatformIO / C++), 181 рядок main.cpp  
**Load Tests:** NBomber (GET 50 копій 15 сек, POST 10 копій 20 сек)  
**Swagger:** Swashbuckle.AspNetCore 7.1.0 (Development only)

## 3. Підтверджені поточні модулі

На даний момент підтверджено наявність або часткову наявність таких модулів:

- Medicines
- Storage conditions
- IoT devices
- Audit log
- Authentication and roles
- Notifications / background checks
- Web client
- Mobile client

## 4. Напрямок дипломної роботи

Ключова ціль дипломної роботи:
розвинути поточний курсовий проєкт до рівня розподіленої інформаційної системи, яка підтримує:

- повний життєвий цикл препарату;
- безпечне зберігання препаратів;
- місця зберігання;
- інциденти порушення умов зберігання;
- сучасний web frontend;
- доопрацьований mobile app;
- більш сильне тестування;
- кращу архітектурну та технічну оформленість.

## 5. Основні must-have для диплома

До обов’язкової частини диплома входять:

- аудит і реорганізація поточного рішення;
- оновлена предметна модель;
- нові або оновлені сутності БД;
- життєвий цикл препарату;
- інциденти зберігання;
- місця зберігання;
- сучасний frontend;
- покращення mobile app;
- unit / integration tests;
- архітектурні діаграми;
- Docker Compose або інший зрозумілий спосіб запуску системи.

## 6. Optional-ідеї

Опціональні ідеї, які можна реалізовувати лише якщо вистачить часу:

- перехід на PostgreSQL;
- Redis;
- push-сповіщення;
- реальний фізичний IoT-пристрій;
- QR / barcode / OCR;
- AI-підсумки ризиків або рекомендації.

## 7. Підтверджені технічні борги (після аудиту 2026-04-09)

**КРИТИЧНІ:**
1. StorageConditionController не має [Authorize] — будь-хто може POST дані умов зберігання
2. JWT термін = 1 рік (DateTime.UtcNow.AddYears(1)), незважаючи на ExpireDays: 30 у конфізі
3. JWT-ключ відкритим текстом у appsettings.json: "A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6"
4. IoT main.cpp: float temperature = 3; float humidity = 30; — DHT22 НЕ ЧИТАЄТЬСЯ
5. JWT-токени захардкоджені у LoadTest.GET, LoadTest.POST і main.cpp

**АРХІТЕКТУРНІ:**
6. Немає StorageLocation, StorageIncident, MedicineLifecycleEvent, Notification сутностей
7. Medicine не має FK до StorageLocation; IoTDevice.Location — рядок, не FK
8. BackgroundServices тільки пишуть у AuditLog, не створюють структуровані сутності
9. StorageConditionMonitoringService: інтервал 5 сек = ~17000 записів/добу при порушенні
10. ~~Frontend вбудований у backend (wwwroot), немає окремого SPA~~ **[ВИКОНАНО 2026-04-27]**

**ЯКІСТЬ КОДУ:**
11. GenerateJwtToken використовує .Result замість await
12. Перший зареєстрований користувач автоматично = Administrator (race condition)
13. Немає DTO-рівня — контролери приймають entity напряму
14. Немає unit / integration тестів для backend
15. Відсутній docker-compose, README.md, ER-діаграма, C4-діаграма (Roadmap створено)

## 8. Уже погоджені рішення

### Рішення 1

- Дата: [вказати дату]
- Рішення: За основу диплома береться існуючий курсовий проєкт.
- Причина: Уже є робоча технічна база, яку можна розвинути до дипломного рівня.
- Наслідок: Потрібно провести аудит і визначити, що залишити, а що переробити.

### Рішення 2

- Дата: [вказати дату]
- Рішення: Основна тема диплома — «Розподілена інформаційна система управління життєвим циклом та безпекою зберігання медичних препаратів».
- Причина: Тема узгоджена з керівником і логічно розвиває існуючий курсовий проєкт.
- Наслідок: Усі подальші технічні рішення мають підтримувати саме цю тему.

### Рішення 3

- Дата: [вказати дату]
- Рішення: Таблиця функціоналу та напрямів розвитку проєкту погоджена керівником.
- Причина: Є базове бачення складу дипломної роботи.
- Наслідок: Можна переходити до аудиту і технічного планування.

## 9. Журнал аналізу

### Запис 1

- Дата: 2026-04-09
- Завдання: Початковий повний технічний аудит усього workspace для дипломної роботи
- Переглянуті файли / модулі: AGENTS.md, PROJECT_CONTEXT.md, всі Controller, Service, Model, DBContext, Migration, BackgroundService, Program.cs, appsettings.json, весь wwwroot (HTML + JS), IoTEmulate/src/main.cpp, diagram.json, platformio.ini, LoadTest.GET/POST, мобільний (структура 26 файлів)
- Основні висновки: Каркас ASP.NET Core 8 правильний. 5 критичних проблем безпеки. Предметна модель неповна для диплома (відсутні 4 ключові сутності). DHT22 не читається (hardcode). Тестів немає. Frontend вбудований у backend. Аудит зафіксовано у AUDIT_AND_DIPLOMA_PLAN.md.
- Що потрібно робити далі: Фаза 1 — виправлення критичних проблем (JWT, авторизація StorageConditionController, IoT DHT22)

### Запис 2

- Дата: 2026-04-13
- Завдання: Фаза 1 — виправлення критичних проблем безпеки
- Переглянуті файли / модулі: AuthController.cs, appsettings.json, StorageConditionController.cs, IoTEmulate/src/main.cpp
- Основні висновки: Спостережено інконсистентність кодування ASCII vs UTF8 (виправлено на UTF8). dht.begin() взагалі не викликався. Збірка 0 помилок, 0 попереджень.
- Що потрібно робити далі: Фаза 2 — розширення предметної моделі

### Запис 3

- Дата: 2026-04-14
- Завдання: Фаза 2 — розширення предметної моделі
- Переглянуті файли / модулі: Models/, Enums/, Services/, Controllers/, DBContext/, Migrations/, Program.cs
- Основні висновки: Створено 6 enum-типів, 4 нові entity, розширено 2 існуючі, 4 нові сервіси, 4 нові контролери, міграція успішно застосована, 0 помилок збірки
- Що потрібно робити далі: Фаза 3 — рефакторинг Background Services

### Запис 4

- Дата: 2026-04-18
- Завдання: Фаза 3 — рефакторинг Background Services
- Переглянуті файли / модулі: BackgroundServices/, appsettings.json
- Основні висновки: `StorageConditionMonitoringService` повністю перероблено — інтервал з config, debounce через `StorageIncident.Status`, auto-resolve, `Notification` + `AuditLog`. `ExpiryNotificationService` оновлено — дедуплікація, `Notification` у БД, `ExpiryWarningDays` з config. 0 помилок.
- Що потрібно робити далі: Фаза 4 — новий SPA Frontend

### Запис 5
- Дата: 2026-04-27
- Завдання: Фаза 4 — Розробка сучасного SPA Frontend
- Переглянуті файли / модулі: Frontend/ (Vite, React, TypeScript, Tailwind, shadcn/ui), Axios client, React Router, AuthContext, ThemeContext.
- Основні висновки: Реалізовано повноцінний SPA на React. 9 основних сторінок (Dashboard, Medicines, IoT, Incidents, Audit, Notifications і т.д.). Налаштовано темну/світлу тему, polling сповіщень. Виправлено CORS (локальний доступ), проблеми з регістром (PascalCase vs camelCase) у JSON та фільтрацію JSON Patch для DTO-полів.
- Що потрібно робити далі: Фаза 5 — Мобільний застосунок

## 10. Журнал змін і рішень

### Запис шаблону

- Дата:
- Що змінено:
- Які файли змінено:
- Причина:
- Ризики / наслідки:
- Наступний крок:

### Запис 1 — Фаза 1 (виконано 2026-04-13)

- Дата: 2026-04-13
- Що змінено:
  - AuthController.GenerateJwtToken: .Result → await, термін з AddYears(1) → AddDays(ExpireDays з config)
  - Кодування ASCII → UTF8 для ключа JWT (консистентно з Program.cs)
  - JWT-ключ перенесено у dotnet User Secrets, appsettings.json очищено (Key = "")
  - StorageConditionController: додано [Authorize(JwtBearerDefaults.AuthenticationScheme)] на рівні класу
  - IoTEmulate/src/main.cpp: додано dht.begin(), hardcoded значення замінено на dht.readTemperature() / dht.readHumidity()
- Які файли змінено: AuthController.cs, appsettings.json, StorageConditionController.cs, IoTEmulate/src/main.cpp
- Причина: Виправлення критичних проблем безпеки перед початком дипломної розробки
- Ризики / наслідки: JWT-ключ зберігається локально у User Secrets; для Production потрібно env-змінну Jwt__Key. IoT-токен у main.cpp все одно hardcoded (це залишається на Фазу 4-5)
- Наступний крок: Фаза 2 — розширення предметної моделі (StorageLocation, StorageIncident, MedicineLifecycleEvent, Notification)

### Запис 2 — Фаза 2 (виконано 2026-04-13)

- Дата: 2026-04-13
- Що змінено:
  - Створено `Enums/` з 6 enum-типами (StorageLocationType, IncidentType, IncidentStatus, LifecycleEventType, NotificationType, AuditSeverity)
  - Створено `Models/StorageLocation.cs`, `StorageIncident.cs`, `MedicineLifecycleEvent.cs`, `Notification.cs`
  - Розширено `Medicine.cs`: +Manufacturer, +BatchNumber, +Description, +MinStorageTemp, +MaxStorageTemp, +StorageLocationId FK
  - Розширено `AuditLog.cs`: +EntityType, +EntityId, +Severity (enum as string)
  - Оновлено `MedicineStorageContext`: 4 нові DbSet + Fluent API (enum as string, FK cascade rules)
  - Міграція `DiplomaPhase2_DomainModel`: 4 нові таблиці, 6 нових колонок, 6 індексів
  - Створено `ServiceStorageLocation`, `ServiceStorageIncident`, `ServiceMedicineLifecycle`, `ServiceNotification`
  - Створено `StorageLocationController`, `StorageIncidentController`, `MedicineLifecycleController`, `NotificationController`
  - Оновлено `ServiceAuditLog`: +entityType, +entityId, +severity параметри (дефолти → зворотна сумісність)
  - Додано `GET /api/auth/me` до `AuthController`
  - Зареєстровано 4 нові сервіси у `Program.cs`
- Які файли змінено: Enums/ (6 нових), Models/ (4 нових, 2 оновлених), Services/ (4 нових, 1 оновлений), Controllers/ (4 нових, 1 оновлений), DBContext/MedicineStorageContext.cs, Program.cs, Migrations/
- Причина: Розширення предметної моделі до дипломного рівня
- Ризики / наслідки: Контролери повертають entity напряму (без DTO) — технічний борг для наступних фаз. Enum as string у БД — читабельніше, але без перекладу.
- Наступний крок: Фаза 3 — рефакторинг Background Services

### Запис 3 — Фаза 3 (виконано 2026-04-18)

- Дата: 2026-04-18
- Що змінено:
  - `appsettings.json`: додано секцію `Monitoring` (`IntervalSeconds=60`, `ExpiryWarningDays=7`)
  - `StorageConditionMonitoringService`: повний рефакторинг:
    - інтервал з `appsettings.json` (замість 5 сек — 60)
    - debounce через `StorageIncident.Status == Active`
    - при порушенні: створюємо `StorageIncident` + `Notification` + `AuditLog(Warning)`
    - при відновленні норми: auto-resolve `StorageIncident` + `Notification` + `AuditLog(Info)`
    - окремі методи для temperature і humidity
  - `ExpiryNotificationService`: оновлено:
    - дедуплікація: не надсилає повторне сповіщення за той самий день
    - зберігає `Notification` у БД (замість логування)
    - `ExpiryWarningDays` з `appsettings.json`
- Які файли змінено: BackgroundServices/StorageConditionMonitoringService.cs, BackgroundServices/ExpiryNotificationService.cs, appsettings.json
- Причина: Рефакторинг Background Services до дипломного рівня
- Ризики / наслідки: debounce працює на рівні БД (один інцидент на пристрій+тип). Якщо IoT-дані нехть до БД — останній `StorageCondition` може бути старим. Це прийнятно для диплому.
- Наступний крок: Фаза 4 — новий SPA Frontend

### Запис 4 — Проміжний аудит (2026-04-20)

- Дата: 2026-04-20
- Що змінено: Створено `INTERMEDIATE_AUDIT_PHASE_1_3.md` — повний проміжний технічний аудит по завершенні Фаз 1-3. Охоплені розділи: готовий функціонал, архітектурні рішення, стек, code review (15 техборгів), JSON-формати для Fronтend, вимоги до Фази 4.
- Ключові висновки: CORS відсутній (критично), 34 готових ендпоінти, 15 позицій техборгу, 2 критичні (хардкод IoT-токен, логіка першого admin)
- Наступний крок: Фаза 4 — SPA Frontend (перша дія: CORS + DTO + React/Vue)

### Запис 5 — Фаза 3.5: виправлення техборгу (2026-04-21)

- Дата: 2026-04-21
- Що змінено: Виправлено 13 позицій техборгу (з 15 з INTERMEDIATE_AUDIT_PHASE_1_3.md), екскл.
  двох що відносяться виключно до Фази 4 (Уніфікований CORS, DTO для SPA)
- Ключові зміни:
  - **TD-01**: IoTEmulate/src/config.h (новий файл) + main.cpp очищено від живого JWT + .gitignore
  - **TD-02**: AuthController: `Count()` → `AnyAsync()` + існує ДО створення користувача
  - **TD-03**: `AsNoTracking()` в 7 сервісах (Read, ReadById, GetAll, GetActive, GetUnread)
  - **TD-06**: `IServiceAuditLog.GetLogs()` метод + рефакторинг AuditLogController
  - **TD-07**: Коментар debounce-обмеження в StorageConditionMonitoringService
  - **TD-08**: GetExpiringMedicines: `m.ExpiryDate > DateTime.Now && ≤ thresholdDate`
  - **TD-09**: Business:LowStockThreshold/ReplenishToQuantity з appsettings.json
  - **TD-10**: IoTDevice: [Required] + [StringLength] + XML-документація
  - **TD-11**: ILogger + try/catch в 4 нових сервісах
  - **TD-12**: Models/DTOs/RoleDto.cs (вилучено з AuthController)
  - **TD-13**: Swagger:Enabled в appsettings (Program.cs оновлено)
  - **TD-14**: Видалено Cookie auth з Program.cs
  - **TD-15**: Прибрано дублювання app.Run() в ConfigureMiddleware
  - **+bonus**: Nullable warnings (CS8603/CS8604) знижено з 31 до 0
- Результат збірки: `dotnet build` — 0 помилок, **0 попереджень**
- Наступний крок: Фаза 4 — SPA Frontend (всі виправлено, CORS + DTO — перші дії)

### Запис 6 — Фаза 4: SPA Frontend (виконано 2026-04-27)

- Дата: 2026-04-27
- Що змінено:
  - Створено окремий проєкт у `Frontend/` на базі Vite 6 + React 18 + TS.
  - Реалізовано дизайн-систему на Tailwind CSS + shadcn/ui (Dashboard, DataTable, Dialogs, Cards).
  - Налаштовано **CORS** у `Program.cs` (дозволено будь-який порт localhost).
  - Створено `AuthContext` для JWT (login/logout, захист роутів, відображення за роллю).
  - Реалізовано 9 сторінок: Dashboard (графіки Recharts), Medicines (CRUD + Patch), IoT Devices, Storage Locations, Incidents, Notifications (polling), Audit Log (фільтрація).
  - Виправлено баг з регістром токена (`Token` vs `token`) в `authApi`.
  - Виправлено баг JSON Patch: додано фільтрацію read-only DTO полів перед відправкою на бекенд.
  - Додано сторінку реєстрації (`/register`).
- Які файли змінено: Frontend/ (весь проєкт), WebApp/MedicationManagement/Program.cs, src/api/index.ts.
- Причина: Створення сучасного інтерфейсу користувача та відокремлення frontend від backend.
- Ризики / наслідки: Токен зберігається в localStorage. Потрібна Node.js 22.12+ для Vite (використано Vite 6 для сумісності з 22.11).
- Наступний крок: Фаза 5 — Мобільний застосунок

## 11. Поточний план найближчих дій (оновлено 2026-04-21)

**Фази 1-3 виконано. Проміжний аудит створено. Фаза 3.5 (виправлення 13 з 15 техборгів) виконано.** Dotnet build: **0 помилок, 0 попереджень**.

**Наступні кроки (пріоритетний порядок):**

1. **[ВИКОНАНО 2026-04-13]** ФАЗА 1 — Виправлення критичних проблем:
   - ✅ GenerateJwtToken: .Result → await, ASCII → UTF8, термін з config
   - ✅ JWT-ключ у User Secrets
   - ✅ [Authorize] до StorageConditionController
   - ✅ dht.begin() + dht.readTemperature()/readHumidity() у IoT main.cpp

2. **[ВИКОНАНО 2026-04-13]** ФАЗА 2 — Розширення предметної моделі:
   - ✅ 6 enum-типів у `Enums/`
   - ✅ `StorageLocation` entity + міграція + CRUD API
   - ✅ `Medicine`: +6 полів + FK до StorageLocation
   - ✅ `AuditLog`: +EntityType, +EntityId, +Severity
   - ✅ `StorageIncident` entity + міграція + API (вкл. resolve)
   - ✅ `MedicineLifecycleEvent` entity + міграція + API
   - ✅ `Notification` entity + міграція + API
   - ✅ `GET /api/auth/me`
   - ✅ `ServiceAuditLog` оновлено (EntityType/EntityId/Severity)

## 11. Поточний план найближчих дій (оновлено 2026-04-18)

**Аудит завершено. Фаза 1 виправлено. Фаза 2 виконано. Фаза 3 виконано.** Результати у AUDIT_AND_DIPLOMA_PLAN.md.

**Наступні кроки (пріоритетний порядок):**

1. **[ВИКОНАНО 2026-04-13]** ФАЗА 1 — Виправлення критичних проблем:
   - ✅ GenerateJwtToken: .Result → await, ASCII → UTF8, термін з config
   - ✅ JWT-ключ у User Secrets
   - ✅ [Authorize] до StorageConditionController
   - ✅ dht.begin() + dht.readTemperature()/readHumidity() у IoT main.cpp

2. **[ВИКОНАНО 2026-04-13]** ФАЗА 2 — Розширення предметної моделі:
   - ✅ 6 enum-типів у `Enums/`
   - ✅ `StorageLocation` entity + міграція + CRUD API
   - ✅ `Medicine`: +6 полів + FK до StorageLocation
   - ✅ `AuditLog`: +EntityType, +EntityId, +Severity
   - ✅ `StorageIncident` entity + міграція + API (вкл. resolve)
   - ✅ `MedicineLifecycleEvent` entity + міграція + API
   - ✅ `Notification` entity + міграція + API
   - ✅ `GET /api/auth/me`
   - ✅ `ServiceAuditLog` оновлено (EntityType/EntityId/Severity)

3. **[ВИКОНАНО 2026-04-18]** ФАЗА 3 — Рефакторинг Background Services:
   - ✅ `appsettings.json`: секція `Monitoring` (IntervalSeconds=60, ExpiryWarningDays=7)
   - ✅ `StorageConditionMonitoringService`: debounce, `StorageIncident`, auto-resolve, `Notification`, `AuditLog`
   - ✅ `ExpiryNotificationService`: дедуплікація, `Notification` у БД, конфіг з appsettings

4. **[ВИКОНАНО 2026-04-27]** ФАЗА 4 — Новий SPA Frontend:
   - ✅ Ініціалізація Vite 6 + React + TS
   - ✅ Налаштування CORS у Program.cs
   - ✅ Реалізація Auth (Login/Register/Me)
   - ✅ 9 основних сторінок та Layout (Sidebar/Topbar)
   - ✅ Polling для сповіщень та інцидентів
   - ✅ Фільтрація JSON Patch для DTO

5. **[ПОТОЧНА]** ФАЗА 5 — Мобільний застосунок

## 12. Підтверджені нові сутності для диплома (реалізовано 2026-04-13)

Стан на 2026-04-13 — всі заплановані сутності реалізовано:

**РЕАЛІЗОВАНО (Enum as string у БД):**
- ✅ `StorageLocation` — місце зберігання (Name, Address, LocationType, FK→IoTDevice)
- ✅ `StorageIncident` — інцидент порушення умов (DeviceId, IncidentType, DetectedValue, Status)
- ✅ `MedicineLifecycleEvent` — подія препарату (MedicineId, EventType, PerformedBy, PerformedAt)
- ✅ `Notification` — сповіщення (Type, Title, Message, TargetRole, IsRead)

**РОЗШИРЕНО у Пазі 2:**
- ✅ `Medicine`: +Manufacturer, +BatchNumber, +Description, +MinStorageTemp, +MaxStorageTemp, +StorageLocationId (FK)
- ✅ `AuditLog`: +EntityType, +EntityId, +Severity

**ІСНУЮТЬ з курсової (залишити):**
- `Medicine`, `StorageCondition`, `IoTDevice`, `AuditLog`

**НЕ ПОТРІБНО (не входить у план):**
- `StorageMeasurement` — замінюється існуючим `StorageCondition`

## 13. Межі диплома

У дипломі не потрібно безконтрольно розширювати систему.
Потрібно зосередитися на:

- реалістичному обсязі робіт;
- коректному розширенні предметної моделі;
- покращенні архітектури;
- покращенні інтерфейсів;
- тестуванні;
- документованому технічному результаті.

Optional-функції не повинні шкодити реалізації основної частини диплома.

# PROJECT_CONTEXT.md

## 1. Короткий опис проєкту

Цей проєкт початково був створений як курсова робота, пов’язана з управлінням медичними препаратами, моніторингом умов їх зберігання та використанням IoT-пристроїв / датчиків.

Зараз проєкт розглядається як основа дипломної роботи на тему:

«Розподілена інформаційна система управління життєвим циклом та безпекою зберігання медичних препаратів»

## 2. Поточна підтверджена архітектура (актуалізовано після комітів 2026-04-28/29 та 2026-05-01)

**Backend:** ASP.NET Core 8 / .NET 8 (`WebApp/MedicationManagement`)  
**Database:** SQL Server  
**ORM:** Entity Framework Core 8.0.11 (2 DbContext: `MedicineStorageContext` + `UserContext`)  
**Authentication:** JWT Bearer + ASP.NET Identity + підтвердження email + відновлення пароля, ролі: `Administrator`, `Manager`, `User`, `Device`  
**API:** REST API (9 контролерів): `AuthController`, `MedicineController`, `StorageConditionController`, `IoTDeviceController`, `AuditLogController`, `StorageLocationController`, `StorageIncidentController`, `MedicineLifecycleController`, `NotificationController`  
**Services:** 8 сервісів з інтерфейсами: `IServiceMedicine`, `IServiceStorageCondition`, `IServiceIoTDevice`, `IServiceAuditLog`, `IServiceStorageLocation`, `IServiceStorageIncident`, `IServiceMedicineLifecycle`, `IServiceNotification`  
**Background Services:** `ExpiryNotificationService` (1 день), `StorageConditionMonitoringService` (інтервал з `Monitoring:IntervalSeconds`, default 60 сек)  
**Web frontend (legacy):** `wwwroot` (Bootstrap + Vanilla JS) — досі присутній у backend  
**Web frontend (SPA):** окремий проєкт `Frontend/` (Vite + React + TypeScript + Tailwind + shadcn/ui)  
**Mobile:** Android Kotlin (Activities/Fragments + частково Compose) у `Mobile/MedicationManagement`  
**IoT:** ESP32 DevKit C v4 + DHT22 + buzzer у Wokwi (PlatformIO / C++), конфіг винесено у `IoTEmulate/src/config.h` (файл ігнорується Git)  
**Load Tests:** NBomber (`WebApp/LoadTest.GET`, `WebApp/LoadTest.POST`)  
**Swagger:** Swashbuckle.AspNetCore 7.1.0 (вмикання через `Swagger:Enabled` у `appsettings.json`)

## 2.1. Останні зміни (після 2026-04-29)

### [ВИКОНАНО 2026-05-01] Атомарне переміщення препарату між локаціями

Проблема: переміщення препарату вимагало двох дій (змінити `Medicine.StorageLocationId` окремо та вручну створити `MedicineLifecycleEvent`), що призводило до розсинхронізації та зайвих переходів у UI.

Рішення:

- Додано backend endpoint `POST /api/medicine/{id}/move`, який в одній транзакції:
  - оновлює `Medicine.StorageLocationId`;
  - створює `MedicineLifecycleEvent` з `EventType = Moved`.
- У Frontend (сторінка `MedicineDetailPage`) додано кнопку/діалог **"Перемістити"**, що викликає цей endpoint і після успіху оновлює дані препарату та список lifecycle-подій.

### [ВИКОНАНО 2026-05-01] Командні операції для подій, що змінюють стан (Quantity)

Щоб lifecycle-події не розходились із фактичним станом препарату, введено окремі атомарні команди:

- `POST /api/medicine/{id}/receive` — збільшує `Medicine.Quantity` і створює `MedicineLifecycleEvent(EventType=Received)`
- `POST /api/medicine/{id}/issue` — зменшує `Medicine.Quantity` і створює `MedicineLifecycleEvent(EventType=Issued)` (з валідацією залишку)
- `POST /api/medicine/{id}/dispose` — зменшує `Medicine.Quantity` і створює `MedicineLifecycleEvent(EventType=Disposed)` (0 = утилізувати весь залишок у UI)

Frontend: у `MedicineDetailPage` додані кнопки **"Надходження" / "Видача" / "Утилізація"** з діалогом введення кількості і коментаря.

### [ВИКОНАНО 2026-05-01] Автоматичні lifecycle-події та статус препарату

Щоб зменшити ручні дії та уникнути ситуацій, коли факт у системі не відображений у журналі:

- Додано `Medicine.Status` (enum як string у БД): `Active`, `Expired`, `Disposed`, `Recalled`.
  - Міграція: `AddMedicineStatus` (для `MedicineStorageContext`).
- При створенні препарату через `POST /api/medicine` автоматично створюється lifecycle-подія `Received` (опис: авто-надходження при створенні).
- `ExpiryNotificationService` доповнено: якщо `Medicine.ExpiryDate <= now` і ще немає lifecycle-події `Expired`, сервіс:
  - створює `MedicineLifecycleEvent(EventType=Expired)` (dedupe);
  - переводить `Medicine.Status` у `Expired` (якщо був `Active`).

Важливо: ручне додавання подій через `POST /api/medicinelifecycle` залишено (як “аудит/коментар”), але ключові стани фіксуються автоматично/атомарно.

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

**АРХІТЕКТУРНІ:** 6. Немає StorageLocation, StorageIncident, MedicineLifecycleEvent, Notification сутностей 7. Medicine не має FK до StorageLocation; IoTDevice.Location — рядок, не FK 8. BackgroundServices тільки пишуть у AuditLog, не створюють структуровані сутності 9. StorageConditionMonitoringService: інтервал 5 сек = ~17000 записів/добу при порушенні 10. ~~Frontend вбудований у backend (wwwroot), немає окремого SPA~~ **[ВИКОНАНО 2026-04-27]**

**ЯКІСТЬ КОДУ:** 11. GenerateJwtToken використовує .Result замість await 12. Перший зареєстрований користувач автоматично = Administrator (race condition) 13. Немає DTO-рівня — контролери приймають entity напряму 14. Немає unit / integration тестів для backend 15. Відсутній docker-compose, README.md, ER-діаграма, C4-діаграма (Roadmap створено)

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

### Запис 6 — Сесія 2026-04-29 (Multi-Tenant Bug-Fix + UX)

- Дата: 2026-04-29
- Завдання: Повний аудит та виправлення проблем рольової моделі, multi-tenancy, UX
- Переглянуті файли / модулі:
  - Backend: `IoTDeviceController.cs`, `StorageIncidentController.cs`, `ServiceStorageIncident.cs`, `ServiceNotification.cs`, `StorageConditionMonitoringService.cs`
  - Frontend: `IoTDevicesPage.tsx`, `StorageLocationsPage.tsx`, `MedicinesPage.tsx`, `IncidentsPage.tsx`, `DashboardPage.tsx`, `AuthContext.tsx`, `App.tsx`
- Основні висновки:
  1. **Критичний баг multi-tenancy (сповіщення/інциденти)**: фоновий сервіс писав `OrganizationId = null` у всі інциденти/сповіщення, бо `CurrentOrgId` порожній у BackgroundService-контексті. Менеджер отримував порожні списки після перезавантаження.
  2. **Системний баг у всіх `Where`-фільтрах**: операції читання (`GetAll`, `GetActive`, `GetById`) та запису (`Resolve`, `MarkAsRead`, `MarkAllAsRead`) фільтрували лише по `OrganizationId == currentOrgId`, відкидаючи legacy-записи з `null`.
  3. **Баг кешу React Query**: при переключенні між акаунтами кеш від адміна зберігався — менеджер бачив чужі дані до першого перезавантаження.
  4. **Доступ менеджера**: кнопки "Додати/Редагувати/Видалити" на сторінках Medicines, StorageLocations, IncidentsPage були доступні лише `isAdmin`, а не `isAdmin || isManager`.
  5. **403 Forbidden для менеджера**: `setstatus`, `UPDATE`, `DELETE` у `IoTDeviceController` мали тільки `Administrator` у `[Authorize(Roles)]`.
- Що потрібно робити далі: Фаза 5 — Мобільний застосунок або Фаза 6 — Тести

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
- Ризики / наслідки: JWT-ключ зберігається локально у User Secrets; для Production потрібно env-змінну Jwt\_\_Key. IoT-токен у main.cpp все одно hardcoded (це залишається на Фазу 4-5)
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

### Запис 4 — Device secret для IoT (виконано 2026-05-02)

- Дата: 2026-05-02
- Що змінено:
  - Додано `DeviceSecretHash` у `IoTDevice` та генерацію device secret під час створення пристрою.
  - `POST /api/iotdevice` тепер повертає DTO з `deviceSecret` (одноразове відображення).
  - `POST /api/auth/device-login` вимагає `deviceSecret` і перевіряє хеш.
  - Додано claim-flow через `POST /api/iotdevice/claim` (без ключів, тільки `DeviceId`).
  - UI реєструє пристрій без видачі secret; IoT-емулятор отримує secret автоматично через claim і далі JWT через device-login.
  - Оновлено `config.example.h` (тільки `DeviceId` + опційний `deviceSecret`), прибрано секцію `Provisioning` з конфігів.
- Які файли змінено: Models/IoTDevice.cs, Models/DTOs/DeviceLoginDto.cs, Models/DTOs/DeviceClaimDto.cs, Models/DTOs/ResponseDTOs.cs, Models/DTOs/MappingExtensions.cs, Services/ServiceIoTDevice.cs, Controllers/IoTDeviceController.cs, Controllers/AuthController.cs, Frontend/src/types/api.ts, Frontend/src/api/index.ts, Frontend/src/pages/IoTDevicesPage.tsx, IoTEmulate/src/main.cpp, IoTEmulate/src/config.example.h, appsettings.json, appsettings.example.json
- Причина: Відмова від hardcoded JWT у IoT, безпечніша аутентифікація пристрою.
- Ризики / наслідки: Потрібна EF міграція для `DeviceSecretHash`; claim можливий лише для вже зареєстрованого у UI `DeviceId`.
- Наступний крок: Створити міграцію БД та (опційно) endpoint для ротації device secret.
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

### Запис 6 — Фаза 4.9 (виконано 2026-04-28)

- Дата: 2026-04-28
- Що змінено:
  - Встановлено `jwt-decode`, оновлено `AuthContext` для зберігання ролі та `isManager`
  - Оновлено `Sidebar`: "Журнал аудиту" та "IoT-пристрої" доступні лише Admin/Manager
  - Реалізовано `IoTDevicesPage.tsx` з таблицею та модальним вікном реєстрації
  - Прив'язка IoT-пристрою при редагуванні локацій (`StorageLocationsPage`)
- Які файли змінено: Frontend/src/contexts/AuthContext.tsx, Frontend/src/components/layout/Sidebar.tsx, Frontend/src/pages/IoTDevicesPage.tsx, Frontend/src/pages/StorageLocationsPage.tsx
- Причина: Рольовий доступ у фронтенді після впровадження multi-tenancy
- Ризики / наслідки: Доступ лише за JWT-роллю (без перевірки на сервері при рендері)
- Наступний крок: Виявлено і виправлено критичні баги рольової моделі та multi-tenancy

### Запис 7 — Фаза 4.10: Bug-Fix Session (виконано 2026-04-29)

- Дата: 2026-04-29
- Що змінено:

  **Бекенд — Рольова модель (403 → 200):**
  - `IoTDeviceController`: додано `Manager` до `[Authorize(Roles)]` для `SetSensorStatus`, `Update`, `Delete`

  **Бекенд — Multi-Tenancy (критичний fix):**
  - `StorageConditionMonitoringService`: встановлено `incident.OrganizationId = device.OrganizationId` та `notification.OrganizationId = device.OrganizationId` — фоновий сервіс більше не пише записи з `null`
  - `IServiceNotification.Create(overload)`: додано параметр `organizationId?` — дозволяє явно передавати org при виклику поза HTTP-контекстом
  - `StorageConditionMonitoringService`: `targetRole: "Administrator"` → `targetRole: "All"` — менеджери отримують сповіщення
  - Всі `Where`-фільтри у `ServiceStorageIncident` та `ServiceNotification` (6 методів): додано умову `|| string.IsNullOrEmpty(i.OrganizationId)` для backward compatibility з legacy-записами

  **Бекенд — StorageIncident.Resolve (404 → 200):**
  - `ServiceStorageIncident.Resolve()`: той самий фільтр з backward compatibility
  - `ServiceStorageIncident.GetById()`: аналогічно
  - `ServiceNotification.MarkAsRead()` та `MarkAllAsRead()`: аналогічно

  **Фронтенд — Рольова модель (Manager бачить і може керувати):**
  - `MedicinesPage.tsx`: `isAdmin` → `canManage = isAdmin || isManager` у 4 місцях
  - `StorageLocationsPage.tsx`: аналогічно, + додано `DialogDescription` (усунено aria-warning)
  - `IncidentsPage.tsx`: `isAdmin` → `canManage` у 4 місцях (заголовок, colSpan, кнопка "Закрити")
  - `IoTDevicesPage.tsx`: `isAdmin` → `canManage`, key prop `<>` → `<Fragment key={...}>`, кнопка видалення з `AlertDialog`-підтвердженням

  **Фронтенд — Кеш React Query:**
  - `AuthContext.tsx`: `queryClient.clear()` викликається при `login()` та `logout()` — усунено витік даних між акаунтами
  - `App.tsx`: `queryClient` переданий як пропс у `AuthProvider`

  **Фронтенд — Dashboard:**
  - `DashboardPage.tsx`: новий компонент `StorageChart` з перемикачем між активними пристроями (кнопки по локаціях)
  - `lowStock` тепер доступний і менеджерам (`enabled: canManage`)

  **Фронтенд — AlertDialog компонент:**
  - Встановлено пакет `@radix-ui/react-alert-dialog`
  - Створено `src/components/ui/alert-dialog.tsx` (стандартний shadcn/radix компонент)

- Які файли змінено:
  - Backend: `IoTDeviceController.cs`, `StorageConditionMonitoringService.cs`, `ServiceNotification.cs`, `ServiceStorageIncident.cs`
  - Frontend: `MedicinesPage.tsx`, `StorageLocationsPage.tsx`, `IncidentsPage.tsx`, `IoTDevicesPage.tsx`, `DashboardPage.tsx`, `AuthContext.tsx`, `App.tsx`, `alert-dialog.tsx` (новий)
- Причина: Усунення системних помилок multi-tenancy та рольової моделі, виявлених під час тестування
- Ризики / наслідки: Backward compatibility фільтр (`|| string.IsNullOrEmpty(OrganizationId)`) технічно дозволяє менеджерам бачити записи без orgs. Це прийнятно для однієї організації, але потребує SQL-міграції для backfill у production.
- Наступний крок: Фаза 5 — Мобільний застосунок або Фаза 6 — Тести

### Запис 8 — Email підтвердження (виконано 2026-05-01)

- Дата: 2026-05-01
- Що змінено:
  - Додано підтвердження пошти для реєстрації користувача та створення менеджера.
  - Додано ендпоінти `GET /api/auth/confirm-email` та `POST /api/auth/resend-confirmation`.
  - Вхід блокується для непідтверджених користувачів.
  - Додано email-сервіс SMTP, секції конфігурації `Email` і `Frontend:BaseUrl`.
  - Додано SPA-сторінку підтвердження пошти `/confirm-email`, кнопку повторної відправки на реєстрації та пояснення у створенні менеджера.
- Які файли змінено: Program.cs, appsettings.json, Controllers/AuthController.cs, Models/EmailSettings.cs, Services/IEmailSender.cs, Services/SmtpEmailSender.cs, Models/DTOs/ResendConfirmationDto.cs, Frontend/src/App.tsx, Frontend/src/api/index.ts, Frontend/src/pages/RegisterPage.tsx, Frontend/src/pages/LoginPage.tsx, Frontend/src/pages/ConfirmEmailPage.tsx
- Причина: Вимога підтвердження email для користувачів і менеджерів.
- Ризики / наслідки: Потрібна валідна SMTP-конфігурація; без неї листи не будуть надсилатися.
- Наступний крок: Налаштувати SMTP в середовищі або User Secrets і протестувати потік.

### Запис 9 — Конфіги та gitignore (виконано 2026-05-01)

- Дата: 2026-05-01
- Що змінено:
  - Оновлено `.gitignore` для виключення збірок, локальних конфігів і секретів у backend/frontend/mobile/IoT.
  - Додано приклади конфігів: `appsettings.example.json` і `IoTEmulate/src/config.example.h`.
- Які файли змінено: .gitignore, WebApp/MedicationManagement/appsettings.example.json, IoTEmulate/src/config.example.h
- Причина: уникнути випадкового коміту секретів і спростити локальне налаштування.
- Ризики / наслідки: Потрібно створювати локальні `appsettings.json` та `config.h` вручну.
- Наступний крок: перенести секрети у User Secrets / env для production.

### Запис 10 — Сповіщення про термін придатності (org fix) (виконано 2026-05-01)

- Дата: 2026-05-01
- Що змінено:
  - Для expiry-сповіщень додано запис `OrganizationId` та дедуплікацію по org.
- Які файли змінено: WebApp/MedicationManagement/BackgroundServices/ExpiryNotificationService.cs
- Причина: усунути видимість сповіщень між організаціями.
- Ризики / наслідки: немає.
- Наступний крок: перевірити історичні сповіщення без org (legacy).

### Запис 11 — Відновлення пароля (виконано 2026-05-01)

- Дата: 2026-05-01
- Що змінено:
  - Додано ендпоінти `POST /api/auth/forgot-password` та `POST /api/auth/reset-password`.
  - Реалізовано email-розсилку лінка для скидання пароля.
  - Додано сторінки SPA для запиту та скидання пароля.
- Які файли змінено: Controllers/AuthController.cs, Models/DTOs/ForgotPasswordDto.cs, Models/DTOs/ResetPasswordDto.cs, Frontend/src/App.tsx, Frontend/src/api/index.ts, Frontend/src/pages/ForgotPasswordPage.tsx, Frontend/src/pages/ResetPasswordPage.tsx, Frontend/src/pages/LoginPage.tsx
- Причина: забезпечити відновлення доступу користувачів.
- Ризики / наслідки: потрібна валідна SMTP-конфігурація; посилання працює лише для підтверджених email.
- Наступний крок: протестувати end-to-end потік скидання пароля.

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

4.5. **[ВИКОНАНО 2026-04-28]** ФАЗА 4.5 — Multi-Tenancy Архітектура:

- ✅ Створення `ApplicationUser` з полем `OrganizationId`
- ✅ Додавання `OrganizationId` до всіх 8 моделей БД
- ✅ Авто-генерація унікального ID при реєстрації нових користувачів
- ✅ Оновлення міграцій та контекстів БД

  4.6. **[ВИКОНАНО 2026-04-28]** ФАЗА 4.6 — Рефакторинг авторизації:

- ✅ Оновлення матриці ролей: `Administrator`, `Manager`, `User`, `Device`
- ✅ Data Seeding для Admin-акаунту в `Program.cs`
- ✅ Заборона зміни ролі при відкритій реєстрації (завжди `User`)
- ✅ Метод `CreateManager` для адміністраторів (прив'язка до `OrganizationId`)
- ✅ Метод `DeviceLogin` для M2M автентифікації IoT-сенсорів за `DeviceId`

  4.7. **[ВИКОНАНО 2026-04-28]** ФАЗА 4.7 — Ізоляція даних у Сервісах (Data Filtering):

- ✅ Додано `AddHttpContextAccessor()` до конвеєра `Program.cs`
- ✅ Створено розширення `ClaimsPrincipalExtensions` для отримання `OrganizationId`
- ✅ Оновлено 8 сервісів предметної області (`IServiceMedicine`, `IServiceStorageLocation` тощо)
- ✅ Впроваджено логіку фільтрації: адміністратори і фонові задачі бачать усе, менеджери/користувачі — лише записи з їхнім `OrganizationId`
- ✅ Автоматичне підставлення `OrganizationId` під час `CreateAsync`

  4.8. **[ВИКОНАНО 2026-04-28]** ФАЗА 4.8 — Міграція `DeviceId` (int -> string):

- ✅ Зміна типу первинного ключа `DeviceID` у моделі `IoTDevice` з `int` на `string`
- ✅ Оновлення пов'язаних таблиць (`StorageCondition`, `StorageLocation`, `StorageIncident`)
- ✅ Оновлення всіх DTO, Controller'ів та Сервісів
- ✅ Генерація нової EF Core міграції `ChangeDeviceIdToString`
- ✅ (База даних перестворюється вручну через неможливість зміни PK у вже створеній БД SQL Server без втрати даних/складних скриптів)

  4.9. **[ВИКОНАНО 2026-04-28]** ФАЗА 4.9 — Адаптація Frontend SPA (Roles & IoT Devices):

- ✅ Встановлено `jwt-decode` для безпечного парсингу ролей з JWT токена
- ✅ Оновлено `AuthContext` для зберігання `Role` та стану `isManager`
- ✅ Оновлено `Sidebar`: "Журнал аудиту" та "Інвентар пристроїв" доступні лише для Admin/Manager
- ✅ Створено сторінку `IoTDevicesPage.tsx` з таблицею та модальним вікном реєстрації
- ✅ Впроваджено `<select>` для прив'язки IoT-пристроїв при створенні/редагуванні локацій (`StorageLocationsPage`)

  4.10. **[ВИКОНАНО 2026-04-29]** ФАЗА 4.10 — Bug-Fix: Multi-Tenancy та Рольова Модель:

- ✅ `StorageConditionMonitoringService`: встановлено `OrganizationId = device.OrganizationId`, `targetRole = "All"`
- ✅ `IServiceNotification.Create`: доданий параметр `organizationId?` для позаhttp-контексту
- ✅ Всі `Where`-фільтри в `ServiceStorageIncident` та `ServiceNotification` (8 методів): backward compatibility для legacy-записів
- ✅ `IoTDeviceController`: доданий `Manager` до `[Authorize(Roles)]` для SetStatus/Update/Delete
- ✅ `MedicinesPage`, `StorageLocationsPage`, `IncidentsPage`, `IoTDevicesPage`: `isAdmin` → `canManage`
- ✅ `AuthContext`: `queryClient.clear()` при login/logout
- ✅ `DashboardPage`: перемикач між пристроями у графіку умов зберігання
- ✅ `IoTDevicesPage`: кнопка видалення пристрою з `AlertDialog`-підтвердженням
- ✅ `IoTDevicesPage`: виправлено React warning (key prop: `<>` → `<Fragment key={...}>`)
- ✅ `StorageLocationsPage`: додано `DialogDescription` (усунено aria-warning)

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

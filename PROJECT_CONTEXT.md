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
10. Frontend вбудований у backend (wwwroot), немає окремого SPA

**ЯКІСТЬ КОДУ:**
11. GenerateJwtToken використовує .Result замість await
12. Перший зареєстрований користувач автоматично = Administrator (race condition)
13. Немає DTO-рівня — контролери приймають entity напряму
14. Немає unit / integration тестів для backend
15. Відсутній docker-compose, README.md, ER-діаграма, C4-діаграма

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

## 11. Поточний план найближчих дій (оновлено 2026-04-09)

**Аудит завершено.** Результати у AUDIT_AND_DIPLOMA_PLAN.md.

**Наступні кроки (пріоритетний порядок):**

1. **[ВИКОНАНО 2026-04-13]** ФАЗА 1 — Виправлення критичних проблем:
   - ✅ GenerateJwtToken: .Result → await, ASCII → UTF8, термін з config
   - ✅ JWT-ключ у User Secrets
   - ✅ [Authorize] до StorageConditionController
   - ✅ dht.begin() + dht.readTemperature()/readHumidity() у IoT main.cpp

2. **[ПОТОЧНА]** ФАЗА 2 — Розширення предметної моделі:
   - StorageLocation entity + міграція + CRUD API
   - Medicine: нові поля + FK до StorageLocation
   - AuditLog: EntityType, EntityId, Severity
   - StorageIncident entity + міграція + API
   - MedicineLifecycleEvent entity + міграція + API
   - Notification entity + міграція + API

3. ФАЗА 3-7 — (відповідно до AUDIT_AND_DIPLOMA_PLAN.md)

## 12. Підтверджені нові сутності для диплома (після аудиту 2026-04-09)

Після аудиту реального коду підтверджено необхідність таких нових сутностей:

**НОВІ (відсутні в БД):**
- `StorageLocation` — місце зберігання (Name, Address, LocationType, FK→IoTDevice)
- `StorageIncident` — інцидент порушення умов (DeviceID, IncidentType, DetectedValue, Status)
- `MedicineLifecycleEvent` — подія препарату (MedicineID, EventType, PerformedBy, PerformedAt)
- `Notification` — сповіщення (Type, Title, Message, TargetRole, IsRead)

**РОЗШИРЕННЯ ІСНУЮЧИХ:**
- `Medicine`: + Manufacturer, BatchNumber, Description, MinStorageTemp, MaxStorageTemp, StorageLocationId (FK)
- `AuditLog`: + EntityType, EntityId, Severity

**ВЖЕ ІСНУЮТЬ (залишити):**
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

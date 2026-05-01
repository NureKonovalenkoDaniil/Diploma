# AUDIT_AND_DIPLOMA_PLAN.md

> Технічний аудит і план розвитку дипломного проєкту  
> Дата аудиту: 2026-04-09  
> Останнє оновлення: 2026-05-01 (підтвердження email, відновлення пароля, org-fix для expiry, приклади конфігів, оновлений .gitignore)  
> Проведено: Antigravity AI

---

# 1. Короткий загальний висновок

## Що це за проєкт зараз

Це курсова робота, яка реалізує базову інформаційну систему управління медичними препаратами на стеку **ASP.NET Core 8 / SQL Server / EF Core / JWT + Identity**. До складу входять:

- backend API (розширено: доменні сутності, multi-tenancy, окремі контролери/сервіси, 2 фонові служби);
- legacy web frontend у `wwwroot` (Bootstrap 5 + Vanilla JS);
- окремий SPA frontend у `Frontend/` (React/TS);
- мобільний застосунок на Kotlin/Android Compose;
- IoT-емуляція на ESP32 + DHT22 у середовищі Wokwi (PlatformIO / C++);
- навантажувальні тести на NBomber (GET і POST).

## Наскільки проєкт придатний як основа диплома

**Придатний.** Технічний каркас правильний і реалізований на сучасному стеку. Після робіт 2026-04-28/29 предметну модель розширено до дипломного рівня (StorageLocation, StorageIncident, MedicineLifecycleEvent, Notification), додано multi-tenancy та сучасний SPA frontend. Ключові прогалини для дипломного “фінішу” зараз: тестування (unit/integration), відтворюваний запуск (docker-compose), узгоджена демонстраційна історія (life cycle + storage safety + incidents + audit), синхронізація Mobile/LoadTests з актуальним API.

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

1. Предметна модель суттєво розширена (StorageLocation/StorageIncident/MedicineLifecycleEvent/Notification, multi-tenancy), але зв'язок “препарат → фактичні умови зберігання” залишається непрямим: Medicine → StorageLocation → IoTDevice → StorageCondition (може бути достатньо для диплома, але UI/звіти потребуватимуть явних сценаріїв).
2. ~~Жорстко зашиті секрети: JWT-ключ і JWT-токени прямо в коді (appsettings.json, main.cpp, тести).~~ **[ВИПРАВЛЕНО 2026-04-13]** JWT-ключ перенесено у User Secrets, appsettings.json очищено. _Залишок: IoT-токен у main.cpp та токени у LoadTests — буде вирішено у Фазі 4-5._
3. Токен зберігається в localStorage — XSS-вразливість.
4. Frontend як SPA вже існує (`Frontend/`), але у репозиторії досі присутній legacy `wwwroot`, а backend робить редірект `/` → `/login.html` (ризик плутанини під час демонстрації диплома).
5. Ніяких тестів (unit / integration).
6. ~~StorageConditionController — не захищений JWT! [Authorize] відсутній на рівні класу.~~ **[ВИПРАВЛЕНО 2026-04-13]** Додано `[Authorize(JwtBearerDefaults.AuthenticationScheme)]`.
7. Перший зареєстрований користувач = Administrator — небезпечна логіка.
8. ~~Термін JWT = 1 рік (рядок 179 AuthController): DateTime.UtcNow.AddYears(1) — критично.~~ **[ВИПРАВЛЕНО 2026-04-13]** Термін читається з `Jwt:ExpireDays` (default 30 днів). Також виправлено `.Result` → `await` і `ASCII` → `UTF8`.
9. ~~IoT-код: температура і вологість захардкоджені (float temperature = 3; float humidity = 30;) — датчик DHT22 не читається реально.~~ **[ВИПРАВЛЕНО 2026-04-13]** Замінено на `dht.readTemperature()` / `dht.readHumidity()`, додано `dht.begin()` у `setup()`.
10. Відсутній docker-compose.

---

# 1.1 Оновлення після 2026-04-29 (коміти 2026-05-01)

## Що додано / змінено

1. `MedicineController`: додані атомарні команди, які одночасно змінюють стан і пишуть `MedicineLifecycleEvent`:
   - `POST /api/medicine/{id}/move`
   - `POST /api/medicine/{id}/receive`
   - `POST /api/medicine/{id}/issue`
   - `POST /api/medicine/{id}/dispose`
2. Додано `Medicine.Status` (`MedicineStatus`: `Active`, `Expired`, `Disposed`, `Recalled`) з EF міграцією `AddMedicineStatus`.
3. Автоматизації lifecycle:
   - при `POST /api/medicine` створюється `MedicineLifecycleEvent(Received)` (авто-надходження);
   - у `ExpiryNotificationService` додано авто-фіксацію `MedicineLifecycleEvent(Expired)` (dedupe) + зміна `Medicine.Status` на `Expired`.
4. `StorageConditionMonitoringService`: покращено обробку вологості/температури та інцидентів; додано поля вологості в `Medicine` (міграція `AddMedicineHumidity`).
5. Frontend (SPA): додані кнопки/діалоги для переміщення та операцій із запасом прямо на `MedicineDetailPage`; оновлено типи `MedicineDto` (додано `status`).
6. Auth: додано підтвердження email (link-based), ендпоінти confirm/resend, блокування входу без підтвердження.
7. Frontend (SPA): додано сторінку `/confirm-email`, кнопку повторної відправки після реєстрації та підказку при створенні менеджера.
8. Конфіги: додано `appsettings.example.json` і `IoTEmulate/src/config.example.h`, оновлено `.gitignore` для секретів/білдів.
9. Expiry notifications: додано `OrganizationId` при створенні та дедуплікацію по org.
10. Auth: додано відновлення пароля (forgot/reset) з email-лінком і нові сторінки SPA.

## Чого не вистачає / ризики (актуально на 2026-05-01)

1. Потрібні unit / integration тести для нових командних endpoint-ів (особливо: валідація залишку, multi-tenancy фільтри, транзакційність).
2. `LoadTest.GET`/`LoadTest.POST` все ще мають hardcoded URL і JWT-токен — ризик витоку і неактуальність після змін (`DeviceID` string, multi-tenancy, статуси).
3. Mobile застосунок відстає від backend:
   - login очікує `token`, а API повертає `Token`;
   - `IoTDevice.DeviceID` у backend string, а в mobile місцями парситься як int;
   - URL бекенду захардкожений `http://10.0.2.2:5000`.
4. Відсутній `docker-compose.yml` і документований “single command run” для демонстрації диплому (backend + db + frontend).
5. Відсутні ER/C4 діаграми та узгоджений “happy path” сценарій демонстрації (life cycle + storage safety + incidents + audit).
6. Підтвердження пошти залежить від SMTP-конфігурації; без валідних налаштувань листи не надсилаються.
7. Відновлення пароля також залежить від SMTP; reset-лінк валідний лише для підтверджених email.

---

# 2. Загальна структура проєкту

Diploma/
AGENTS.md
PROJECT_CONTEXT.md
AUDIT_AND_DIPLOMA_PLAN.md
WebApp/
MedicationManagement/ <- Backend ASP.NET Core 8
Controllers/ <- 5 API-контролерів
Services/ <- 4 сервіси з інтерфейсами
BackgroundServices/ <- 2 фонові служби
Models/ <- 7 моделей / DTO
DBContext/ <- 2 DbContext (основний + Identity)
Migrations/ <- EF Core міграції (2 набори)
wwwroot/ <- Вбудований HTML/JS frontend (10 сторінок)
Program.cs
appsettings.json
LoadTest.GET/ <- NBomber GET тест
LoadTest.POST/ <- NBomber POST тест
Mobile/
MedicationManagement/ <- Android Kotlin + Compose (26 Kotlin-файлів)
IoTEmulate/ <- ESP32/Wokwi IoT-емуляція (C++/PlatformIO)
src/main.cpp
diagram.json
platformio.ini

---

# 3. Детальний аудит по модулях

## 3.1 Backend (ASP.NET Core 8)

Файли: WebApp/MedicationManagement/

Що реалізовано:

- DI для всіх сервісів
- JWT Bearer authentication
- ASP.NET Identity для управління користувачами
- Swagger/OpenAPI (Swashbuckle 7.1.0)
- JSON Patch для часткового оновлення (PATCH endpoints)
- Статична роздача файлів (UseStaticFiles)
- **[ДОДАНО Фаза 2]** 4 нові контролери: StorageLocationController, StorageIncidentController, MedicineLifecycleController, NotificationController
- **[ДОДАНО Фаза 2]** 4 нові сервіси: ServiceStorageLocation, ServiceStorageIncident, ServiceMedicineLifecycle, ServiceNotification
- **[ДОДАНО Фаза 2]** `GET /api/auth/me` у AuthController
- **[ДОДАНО Фаза 2]** 6 enum-типів у `Enums/`

Що реалізовано частково або слабо:

- ~~StorageConditionController не має [Authorize] на рівні класу~~ **[ВИПРАВЛЕНО 2026-04-13]**
- ~~GenerateJwtToken використовує .Result замість await~~ **[ВИПРАВЛЕНО 2026-04-13]**
- ReplenishmentRecommendation — логіка рекомендацій примітивна: 100 - quantity
- Немає DTO-рівня: контролери приймають і повертають entity-моделі напряму

Що відсутнє:

- ~~StorageLocation, StorageIncident, MedicineLifecycleEvent, Notification~~ **[ДОДАНО Фаза 2]**
- ~~Зв'язок Medicine із StorageLocation~~ **[ДОДАНО Фаза 2]**
- Unit / integration тести, docker-compose

Висновок: основний рефакторинг виконано. Залишилось: DTO (частково впроваджено для SPA), тести, docker-compose.

## 3.2 Database / Models / Entities / Migrations

Стан схеми БД станом на 2026-04-14:

| Сутність                    | Поля                                                                                                                                                        | Стан                |
| --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------- |
| Medicine                    | MedicineID, Name, Type, ExpiryDate, Quantity, Category, **+Manufacturer, +BatchNumber, +Description, +MinStorageTemp, +MaxStorageTemp, +StorageLocationId** | ✅ Розширено Фаза 2 |
| StorageCondition            | ConditionID, Temperature, Humidity, Timestamp, DeviceID                                                                                                     | Залишити            |
| IoTDevice                   | DeviceID, Location(string), Type, Parameters, IsActive, Min/Max Temp/Humidity                                                                               | Залишити            |
| AuditLog                    | Id, Action, User, Timestamp, Details, **+EntityType, +EntityId, +Severity**                                                                                 | ✅ Розширено Фаза 2 |
| ReplenishmentRecommendation | MedicineId, MedicineName, RecommendedQuantity                                                                                                               | Тільки DTO          |
| **StorageLocation**         | LocationId, Name, Address, LocationType (enum), IoTDeviceId FK                                                                                              | ✅ ДОДАНО Фаза 2    |
| **StorageIncident**         | IncidentId, DeviceId FK, LocationId FK, IncidentType (enum), DetectedValue, ExpectedMin/Max, StartTime, EndTime, Status (enum), CreatedAt                   | ✅ ДОДАНО Фаза 2    |
| **MedicineLifecycleEvent**  | EventId, MedicineId FK, EventType (enum), Description, Quantity, PerformedBy, PerformedAt, RelatedLocationId FK                                             | ✅ ДОДАНО Фаза 2    |
| **Notification**            | NotificationId, Type (enum), Title, Message, TargetRole, IsRead, CreatedAt, RelatedEntityType, RelatedEntityId                                              | ✅ ДОДАНО Фаза 2    |

Два DbContext: MedicineStorageContext і UserContext — правильне рішення, залишити.

Висновок: схема БД розширена до дипломного рівня.

## 3.3 Controllers / API

| Контролер                       | Endpoints                                                                     | Авторизація                                      | Стан               |
| ------------------------------- | ----------------------------------------------------------------------------- | ------------------------------------------------ | ------------------ |
| AuthController                  | POST register, login, create-role, assign-role, **GET me**                    | Частково                                         | ✅ оновлено Фаза 2 |
| MedicineController              | GET, GET{id}, POST, PATCH{id}, DELETE{id}, low-stock, expiring, replenishment | JWT + Role                                       | Добре              |
| StorageConditionController      | GET, GET{id}, POST, PATCH{id}, DELETE{id}, checkCondition                     | ~~НЕ ЗАХИЩЕНИЙ~~ **[ВИПРАВЛЕНО 2026-04-13]** JWT | Виправлено         |
| IoTDeviceController             | GET, GET{id}, POST, PATCH{id}, DELETE{id}, setstatus, conditions/{id}         | JWT + Role                                       | Добре              |
| AuditLogController              | GET (filters: from, to, user, action)                                         | Administrator only                               | Добре              |
| **StorageLocationController**   | GET, GET{id}, POST, PUT{id}, DELETE{id}                                       | JWT (запис/видалення — Admin)                    | ✅ ДОДАНО Фаза 2   |
| **StorageIncidentController**   | GET, GET active, GET{id}, POST, PATCH resolve                                 | JWT (POST/resolve — Admin)                       | ✅ ДОДАНО Фаза 2   |
| **MedicineLifecycleController** | GET all, GET medicine/{id}, GET{id}, POST                                     | JWT                                              | ✅ ДОДАНО Фаза 2   |
| **NotificationController**      | GET, GET unread, POST, PATCH read, PATCH read-all                             | JWT                                              | ✅ ДОДАНО Фаза 2   |

Висновок: усі заплановані контролери реалізовано.

## 3.4 Services / Business Logic

| Сервіс                       | Інтерфейс                 | Стан                                                  |
| ---------------------------- | ------------------------- | ----------------------------------------------------- |
| ServiceMedicine              | IServiceMedicine          | RecommendedQuantity = 100 - quantity — hardcoded      |
| ServiceStorageCondition      | IServiceStorageCondition  | CheckConditionsForAllDevices повертає List\<string\>  |
| ServiceIoTDevice             | IServiceIoTDevice         | Добре                                                 |
| ServiceAuditLog              | IServiceAuditLog          | ✅ оновлено Фаза 2: +EntityType, +EntityId, +Severity |
| **ServiceStorageLocation**   | IServiceStorageLocation   | ✅ ДОДАНО Фаза 2                                      |
| **ServiceStorageIncident**   | IServiceStorageIncident   | ✅ ДОДАНО Фаза 2                                      |
| **ServiceMedicineLifecycle** | IServiceMedicineLifecycle | ✅ ДОДАНО Фаза 2                                      |
| **ServiceNotification**      | IServiceNotification      | ✅ ДОДАНО Фаза 2                                      |

Висновок: всі заплановані сервіси реалізовано.

## 3.5 Authentication / Authorization / Roles

~~Критичні проблеми:~~

1. ~~Термін JWT-токена — 1 рік~~ **[ВИПРАВЛЕНО 2026-04-13]** — тепер `AddDays(ExpireDays)`, default 30 днів
2. ~~JWT-ключ у appsettings.json відкритим текстом~~ **[ВИПРАВЛЕНО 2026-04-13]** — перенесено у User Secrets
3. Перший зареєстрований = Administrator (usersCount == 1) — race condition і неявна семантика
4. Немає Refresh Token
5. Токен у localStorage (XSS)
6. ~~GenerateJwtToken використовує .Result замість await~~ **[ВИПРАВЛЕНО 2026-04-13]** — тепер async/await + UTF8
7. Немає /api/auth/me

Висновок: частково виправлено. Залишились пп. 3, 4, 5, 7.

## 3.6 Audit / Logging

Що є: таблиця AuditLogs, сервіс LogAction, контролер із фільтрацією.

~~Проблеми:~~

- ~~Немає EntityType і EntityId~~ **[ДОДАНО Фаза 2]**
- ~~Немає Severity~~ **[ДОДАНО Фаза 2]**
- Update-операції не покриті аудитом (нові контролери логують)
- Немає пагінації

Висновок: частково покращено. Залишилось: пагінація, покриття update-операцій.

## 3.7 Background Services

| Служба                            | Інтервал          | Стан                                                                                      |
| --------------------------------- | ----------------- | ----------------------------------------------------------------------------------------- |
| ExpiryNotificationService         | 1 доба            | ✅ Оновлено Фаза 3: дедуплікація, `Notification` у БД, `ExpiryWarningDays` з config       |
| StorageConditionMonitoringService | 60 сек (з config) | ✅ Оновлено Фаза 3: debounce, `StorageIncident`, auto-resolve, `Notification`, `AuditLog` |

~~Немає debounce/dedup-логіки.~~ **[ДОДАНО Фаза 3]**

Висновок: рефакторинг виконано. Залишилось: інтеграція з реальним IoT та push-сповіщення (optional).

## 3.8 IoT / Sensor Integration / Emulation

Платформа: ESP32 DevKit C v4 + DHT22 + buzzer (Wokwi + PlatformIO + C++)

Що є:

- HTTP GET до /api/iotdevice/{id} для отримання порогових значень
- HTTP POST до /api/storagecondition для передачі вимірювань
- JWT-автентифікація у запитах
- Buzzer-сигнал при порушенні

~~КРИТИЧНІ ПРОБЛЕМИ:~~

1. ~~Рядки 150-151 і 172-173: float temperature = 3; float humidity = 30; — DHT22 НЕ ЧИТАЄТЬСЯ!~~ **[ВИПРАВЛЕНО 2026-04-13]** — замінено на `dht.readTemperature()` / `dht.readHumidity()`, додано `dht.begin()` у `setup()`
2. JWT-токен захардкоджений у main.cpp рядок 22 _(залишається, буде вирішено у Фазі 4-5)_
3. URL захардкоджений: http://192.168.100.2:5000 _(залишається, буде вирішено у Фазі 4-5)_
4. deviceID = 4 захардкоджений _(залишається, буде вирішено у Фазі 4-5)_

Висновок: DHT22 виправлено. JWT-токен/URL/deviceID — залишаються на пізніші фази.

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

Висновок: повністю перенесено у окремий SPA (React + Tailwind). ✅ ВИКОНАНО 2026-04-27.

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

| Компонент                                            | Що конкретно змінити                                                                      |
| ---------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| ~~AuthController.GenerateJwtToken~~                  | ~~Замінити .Result на await, читати ExpireDays з config~~ **[ВИКОНАНО 2026-04-13]**       |
| ~~appsettings.json~~                                 | ~~Перенести JWT-ключ у User Secrets або env~~ **[ВИКОНАНО 2026-04-13]**                   |
| ~~StorageConditionController~~                       | ~~Додати [Authorize] або API-ключ для IoT~~ **[ВИКОНАНО 2026-04-13]**                     |
| ~~IoTEmulate/src/main.cpp (рядки 150-151, 172-173)~~ | ~~dht.readTemperature() / dht.readHumidity() замість hardcode~~ **[ВИКОНАНО 2026-04-13]** |
| AuditLog entity                                      | Додати EntityType, EntityId, Severity + міграція                                          |
| ExpiryNotificationService                            | Зберігати Notification entity                                                             |
| StorageConditionMonitoringService                    | Зберігати StorageIncident + Notification, збільшити інтервал, debounce                    |
| IoTEmulate/src/main.cpp (рядки 20-22)                | Параметризувати URL і JWT-токен _(Фаза 4-5)_                                              |
| LoadTest.GET/POST                                    | Перенести токен і URL у конфіг _(Фаза 6)_                                                 |

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
MedicationManagement/ <- ASP.NET Core 8 API (тільки backend)
Controllers/ <- + StorageLocationController, StorageIncidentController
<- + MedicineLifecycleController, NotificationController
Services/ <- + ServiceStorageLocation, ServiceStorageIncident
<- + ServiceMedicineLifecycle, ServiceNotification
Models/ <- + StorageLocation, StorageIncident
<- + MedicineLifecycleEvent, Notification
<- + оновлений Medicine, оновлений AuditLog
BackgroundServices/ <- Оновлені: StorageIncident + Notification
DBContext/ <- Оновлений MedicineStorageContext
Migrations/ <- Нові EF Core міграції
Frontend/ <- [НОВИЙ] Окремий SPA (Vue.js або React)
src/
pages/
components/
api/
package.json
Mobile/
MedicationManagement/ <- Kotlin + Compose (нові екрани)
IoTEmulate/ <- ESP32 + Wokwi (виправлений)
Tests/ <- [НОВИЙ] xUnit + integration tests
MedicationManagement.Tests/
docker-compose.yml <- [НОВИЙ]
README.md <- [НОВИЙ]
IMPLEMENTATION_ROADMAP.md <- [НОВИЙ]

Розподіл процесів:

- ASP.NET Core API: порт 5000/7069
- Frontend SPA: порт 3000/5173, спілкується з API через HTTP
- Mobile: Android APK, спілкується з тим же API
- IoT: ESP32 Wokwi через HTTP POST до API
- SQL Server: окремий контейнер у docker-compose

---

# 8. Пропозиція нових сутностей і зв'язків

Medicine (ДОПОВНИТИ):

- Manufacturer, BatchNumber, Description
- MinStorageTemp, MaxStorageTemp (float)
- StorageLocationId (FK -> StorageLocation, nullable)

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

- EntityType (Medicine / IoTDevice / StorageCondition / StorageIncident)
- EntityId (int, nullable)
- Severity (Info / Warning / Critical)

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

## Фаза 1 — ~~Виправлення критичних проблем (1-2 дні)~~ ✅ ВИКОНАНО (2026-04-13)

1. ~~Виправити GenerateJwtToken: .Result → await, читати ExpireDays з config~~ ✅
2. ~~Перенести JWT-ключ у User Secrets / env~~ ✅
3. ~~Вирішити авторизацію StorageConditionController ([Authorize] або API-ключ)~~ ✅
4. ~~Виправити main.cpp рядки 150-151, 172-173: dht.readTemperature() / dht.readHumidity()~~ ✅

## Фаза 2 — ~~Розширення предметної моделі (3-5 днів)~~ ✅ ВИКОНАНО (2026-04-13)

5. ~~StorageLocation entity + міграція + Controller + Service~~ ✅
6. ~~Medicine: нові поля + FK до StorageLocation + міграція~~ ✅
7. ~~AuditLog: EntityType, EntityId, Severity + міграція~~ ✅
8. ~~StorageIncident entity + міграція + Controller + Service~~ ✅
9. ~~MedicineLifecycleEvent entity + міграція + Controller~~ ✅
10. ~~Notification entity + міграція + Controller~~ ✅
11. ~~GET /api/auth/me~~ ✅
12. ~~6 enum-типів у Enums/~~ ✅

## Фаза 3 — ~~Рефакторинг Background Services (1-2 дні)~~ ✅ ВИКОНАНО (2026-04-18)

11. ~~`StorageConditionMonitoringService`: `StorageIncident` + `Notification`, debounce, інтервал 60 сек~~ ✅
12. ~~`ExpiryNotificationService`: зберігати `Notification` entity, дедуплікація~~ ✅
13. ~~`appsettings.json`: секція `Monitoring` (`IntervalSeconds`, `ExpiryWarningDays`)~~ ✅

## Фаза 4 — ~~Новий Frontend SPA (5-7 днів)~~ ✅ ВИКОНАНО 2026-04-27

13. ~~Ініціалізувати Vue.js або React проєкт у Frontend/~~ ✅
14. ~~Перенести існуючі сторінки у компоненти~~ ✅
15. ~~Нові сторінки: StorageLocation, StorageIncident, MedicineLifecycle, Notification~~ ✅
16. ~~Графіки Temperature/Humidity (Chart.js)~~ ✅ (Recharts)
17. ~~Адмін-панель для управління користувачами~~ ✅ (Audit Log + Roles)

## Фаза 4.5 — ~~Архітектура Multi-Tenancy (1-2 дні)~~ ✅ ВИКОНАНО 2026-04-28

17.1. ~~Створення ApplicationUser з OrganizationId~~ ✅
17.2. ~~Додавання OrganizationId до всіх доменних моделей~~ ✅
17.3. ~~Оновлення генерації користувачів у AuthController~~ ✅
17.4. ~~Оновлення EF Core міграцій~~ ✅

## Фаза 4.6 — ~~Рефакторинг авторизації (1 день)~~ ✅ ВИКОНАНО 2026-04-28

17.5. ~~Оновлення ролей: Admin, Manager, User, Device~~ ✅
17.6. ~~Впровадження Data Seeding для Admin-акаунту~~ ✅
17.7. ~~Створення ендпоінтів `CreateManager` та `DeviceLogin`~~ ✅

## Фаза 4.7 — ~~Ізоляція даних у Сервісах (Data Filtering)~~ ✅ ВИКОНАНО 2026-04-28

17.8. ~~Створення розширення ClaimsPrincipalExtensions~~ ✅
17.9. ~~Ін'єкція IHttpContextAccessor у 8 сервісів~~ ✅
17.10. ~~Фільтрація GET запитів за OrganizationId у сервісах~~ ✅
17.11. ~~Автоматичне додавання OrganizationId при CreateAsync~~ ✅

## Фаза 4.9 — ~~Адаптація Frontend SPA (Roles & IoT Devices)~~ ✅ ВИКОНАНО 2026-04-28

17.12. ~~Встановлено `jwt-decode`, оновлено `AuthContext` для ролі/isManager~~ ✅
17.13. ~~Оновлено `Sidebar` (Admin/Manager доступ)~~ ✅
17.14. ~~Реалізовано `IoTDevicesPage.tsx`~~ ✅
17.15. ~~Прив'язка IoT-пристрою при редагуванні локацій~~ ✅

## Фаза 4.10 — ~~Multi-Tenancy Bug-Fix (1 день)~~ ✅ ВИКОНАНО 2026-04-29

17.16. ~~`StorageConditionMonitoringService`: `OrganizationId = device.OrganizationId`, `targetRole = "All"`~~ ✅
17.17. ~~`IServiceNotification.Create`: параметр `organizationId?` для BackgroundService~~ ✅
17.18. ~~Всі `Where`-фільтри (8 методів у 2 сервісах): backward compatibility `|| IsNullOrEmpty`~~ ✅
17.19. ~~`IoTDeviceController`: `Manager` у `[Authorize(Roles)]` для SetStatus/Update/Delete~~ ✅
17.20. ~~`MedicinesPage`, `StorageLocationsPage`, `IncidentsPage`, `IoTDevicesPage`: `isAdmin` → `canManage`~~ ✅
17.21. ~~`AuthContext.queryClient.clear()` при login/logout~~ ✅
17.22. ~~`DashboardPage`: `StorageChart` з перемикачем між пристроями~~ ✅
17.23. ~~`IoTDevicesPage`: `AlertDialog` перед видаленням + `Fragment key`~~ ✅
17.24. ~~`StorageLocationsPage`: `DialogDescription` (aria-warning)~~ ✅ 18. Перевірити якість поточного коду (Retrofit, обробка помилок, URL) 19. Нові екрани: StorageIncidents, MedicineLifecycle

## Фаза 5 — Мобільний застосунок (2-3 дні)

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

| Компонент                                 | Поточний стан                                                         | Дія                                      |
| ----------------------------------------- | --------------------------------------------------------------------- | ---------------------------------------- |
| ASP.NET Core 8 backend (каркас)           | Є, працює                                                             | Залишити і розширити                     |
| JWT + ASP.NET Identity                    | ~~Є, є критичні баги~~ Виправлено (термін, ключ, .Result)             | Залишилось: Refresh Token                |
| Medicine entity + CRUD                    | ~~Є, неповна модель~~ Розширено Фаза 2 (+6 полів, FK)                 | Залишилось: DTO-рівень                   |
| StorageCondition entity + CRUD            | Є                                                                     | Залишити                                 |
| IoTDevice entity + CRUD                   | Є                                                                     | Залишити                                 |
| AuditLog entity + service                 | ~~Є, спрощений~~ Розширено Фаза 2 (+EntityType, +EntityId, +Severity) | Залишилось: пагінація                    |
| ExpiryNotificationService                 | Є, тільки логує                                                       | Доробити (Фаза 3)                        |
| StorageConditionMonitoringService         | Є, 5 сек, тільки логує                                                | Переробити (Фаза 3)                      |
| ~~StorageConditionController безпека~~    | ~~КРИТИЧНА ПРОБЛЕМА~~                                                 | ✅ Виправлено 2026-04-13                 |
| ~~StorageLocation~~                       | ~~Відсутній~~                                                         | ✅ Додано Фаза 2                         |
| ~~StorageIncident~~                       | ~~Відсутній~~                                                         | ✅ Додано Фаза 2                         |
| ~~MedicineLifecycleEvent~~                | ~~Відсутній~~                                                         | ✅ Додано Фаза 2                         |
| ~~Notification entity~~                   | ~~Відсутній~~                                                         | ✅ Додано Фаза 2                         |
| ~~StorageLocationController + service~~   | ~~Відсутні~~                                                          | ✅ Додано Фаза 2                         |
| ~~StorageIncidentController + service~~   | ~~Відсутні~~                                                          | ✅ Додано Фаза 2                         |
| ~~MedicineLifecycleController + service~~ | ~~Відсутні~~                                                          | ✅ Додано Фаза 2                         |
| ~~NotificationController + service~~      | ~~Відсутні~~                                                          | ✅ Додано Фаза 2                         |
| ~~GET /api/auth/me~~                      | ~~Відсутній~~                                                         | ✅ Додано Фаза 2                         |
| Web frontend (wwwroot)                    | Є, Bootstrap + Vanilla JS                                             | Перенести у SPA (Фаза 4)                 |
| Frontend SPA (Frontend/)                  | Відсутній                                                             | Додати (Vue.js або React) (Фаза 4)       |
| Android Mobile (Kotlin+Compose)           | Є, 26 файлів                                                          | Доробити (нові екрани) (Фаза 5)          |
| IoT (ESP32 + Wokwi)                       | ~~Є, DHT22 НЕ ЧИТАЄТЬСЯ~~ DHT22 виправлено                            | Залишилось: URL/токен/deviceID hardcoded |
| LoadTest GET/POST (NBomber)               | Є, hardcoded токен і URL                                              | Доробити (Фаза 6)                        |
| Unit / Integration тести backend          | Відсутні                                                              | Додати (Фаза 6)                          |
| docker-compose.yml                        | Відсутній                                                             | Додати (Фаза 7)                          |
| README.md                                 | Відсутній                                                             | Додати (Фаза 7)                          |
| ER-діаграма, C4-діаграма                  | Відсутні                                                              | Додати (Фаза 7)                          |
| IMPLEMENTATION_ROADMAP.md                 | Відсутній                                                             | Додати (Фаза 7)                          |

---

_Документ підготовлено за результатами повного технічного аудиту workspace станом на 2026-04-09._  
_Оновлено 2026-04-13: Фаза 1 завершена — виправлено JWT (термін, ключ, async), [Authorize] у StorageConditionController, DHT22 у IoT main.cpp._  
_Оновлено 2026-04-14: Фаза 2 завершена — предметна модель розширена (4 нові entity, 6 enum, 4 сервіси, 4 контролери, міграція)._  
_Оновлено 2026-04-18: Фаза 3 завершена — рефакторинг Background Services (дебаунс, auto-resolve, Notification в БД, дедуплікація, конфіг з appsettings)._  
_Оновлено 2026-04-20: Проміжний аудит (INTERMEDIATE_AUDIT_PHASE_1_3.md) — 34 ендпоінти, 15 позицій техборгу, вимоги до Фази 4._  
_Оновлено 2026-04-21: Фаза 3.5 завершена — виправлено 13 з 15 техборгів (TD-01..TD-15, excl. TD-04/CORS). dotnet build: 0 помилок, 0 попереджень._  
_Оновлено 2026-04-27: Фаза 4 завершена — розроблено сучасний SPA на React/TS, виправлено CORS, баги з регістром JSON та JSON Patch filtering._  
_Оновлено 2026-04-28: Фаза 4.5 завершена — впроваджено базову архітектуру Multi-Tenancy (додано OrganizationId до всіх моделей та ApplicationUser, оновлено AuthController)._  
_Оновлено 2026-04-28: Фаза 4.6 завершена — проведено рефакторинг ролей, додано CreateManager, DeviceLogin та Data Seeding._  
_Оновлено 2026-04-28: Фаза 4.7 завершена — впроваджено ізоляцію даних на рівні сервісів (Data Filtering)._  
_Оновлено 2026-04-28: Фаза 4.9 завершена — Frontend: ролі/isManager, IoTDevicesPage, прив'язка пристроїв._  
_Оновлено 2026-04-29: Фаза 4.10 завершена — Bug-Fix: критичні проблеми multi-tenancy (OrganizationId=null у BackgroundService, backward compatibility фільтри, кеш React Query), рольова модель Manager, Dashboard з перемикачем пристроїв, AlertDialog для видалення. Backend: 0 помилок. Frontend: tsc 0 помилок, build успішний._  
_Наступне оновлення — після завершення Фази 5 (Мобільний застосунок) або Фази 6 (Тести)._

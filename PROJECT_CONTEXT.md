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

### [ВИКОНАНО 2026-05-13] Вирівнювання mobile app під backend DTO та routes

- Переведено mobile API на реальні backend routes:
  - `api/auth/users` для списку та видалення користувачів;
  - `api/auth/assign-role` для зміни ролі;
  - `api/iotdevice/conditions/{deviceId}` для історії умов на деталях пристрою.
- Узгоджено mobile DTO з backend:
  - `Medicine` тепер містить status, manufacturer, batchNumber, description, storage thresholds і storage location;
  - `IoTDevice` та `StorageCondition` приведені до серверних полів;
  - `UserDto` і `AuditLogDto` синхронізовані з відповідями backend.
- Окрему вкладку `StorageLocationsFragment` переведено з повторного використання `DeviceAdapter` на власний adapter/viewmodel для локацій зберігання.
- `AuditLogFragment` більше не покладається на неіснуючу серверну фільтрацію по entityType: фільтр працює локально по вже завантажених записах, а timestamp читається коректно.
- `DeviceDetailsActivity` почав будувати графік за реальними storage conditions, а не на згенерованих точках.
- Розширено medicine screens: список, details, create/edit форми тепер приймають і відображають повніші backend-поля.
- Android збірка перевірена: `assembleDebug` успішно проходить.

### [ВИКОНАНО 2026-05-13] Фікс вкладки Storage Locations у mobile

- Виправлено мепінг DTO для `StorageLocationDto`, щоб коректно читати `IoTDeviceId` з backend (`iotDeviceId` / `ioTDeviceId` / `IoTDeviceId`).
- На вкладці локацій повернуто керування:
  - FAB `+` знову доступний для ролей з повним доступом;
  - додано створення нової локації;
  - додано редагування локації по натисканню на елемент;
  - додано видалення локації.
- Додано форму `dialog_storage_location_form.xml` і CRUD-операції у `StorageLocationsViewModel`.
- Android збірка після правок: `assembleDebug` — успішно.

### [ВИКОНАНО 2026-05-13] Оновлення Users екрана у mobile

- Прибрано зайву дію зміни ролі на екрані користувачів (кнопка Role і відповідний діалог).
- Додано адмінський сценарій створення менеджера на Users екрані:
  - нова кнопка (FAB) тільки для `Administrator`;
  - форма введення email і пароля;
  - інтеграція з backend endpoint `POST /api/auth/create-manager`.
- Додано читання `OrganizationId` з JWT claim для коректного виклику `create-manager`.
- Додано відсутні українські локалізації для Users-розділу (щоб не було fallback на англійське `Users`).
- Android збірка після правок: `assembleDebug` — успішно.

### [ВИКОНАНО 2026-05-13] Локалізація mobile app українською та англійською

- Завершено перший системний прохід по hardcoded текстах у mobile UI та основних flows.
- Переведено на ресурси:
  - `ConfirmEmailActivity`, `RegisterActivity`, `AddDeviceActivity`;
  - `AddMedicineActivity`, `EditMedicineActivity`;
  - `NotificationsFragment`;
  - `activity_confirm_email.xml`, `activity_login.xml`, `activity_add_device.xml`;
  - `fragment_medicines.xml`, `fragment_sensors.xml`, `item_medicine.xml`.
- Додано нові ключі у `values/strings.xml` та `values-uk/strings.xml` для email confirmation, device binding, medicine CRUD, notifications і reusable labels.
- Підтверджено, що runtime locale switching уже працював через `AppPreferences` / `SettingsFragment`, тому зміни зосереджені саме на покритті UI-рядків ресурсами.
- Android збірка після локалізації: `assembleDebug` — успішно.

### [ВИКОНАНО 2026-06-15] Локалізація фронтенд-частини українською та англійською

- Додано легкий `LocaleContext` для web SPA без сторонніх i18n-залежностей.
- Додано перемикач мови `UA / EN` у верхній панелі.
- Переведено на локальну словникову модель ключові користувацькі екрани:
  - `Topbar`;
  - `Sidebar`;
  - `LoginPage`;
  - `ConfirmEmailPage`;
  - `ForgotPasswordPage`;
  - `ResetPasswordPage`;
  - `DashboardPage`;
  - `StorageLocationsPage`;
  - `NotificationsPage`;
  - `IncidentsPage`;
  - `UsersPage`.
- Frontend збірка після локалізації: `npm run build` — успішно.

### [ВИКОНАНО 2026-06-28] Покращення UI та цілісності зв'язків при видаленні локацій та реєстрації пристроїв

- **Підтвердження видалення локацій:** Додано спливаючий діалог підтвердження `AlertDialog` на сторінку локацій (`StorageLocationsPage.tsx`) перед видаленням, аналогічно до сторінки пристроїв. Локалізовано відповідні повідомлення обома мовами.
- **Випадаючі списки для вибору локації пристроїв:** Замінено текстові поля ручного введення назви локації на випадаючі списки (`select`) зі списком реально існуючих у системі локацій у формах створення та редагування пристроїв на сторінці `IoTDevicesPage.tsx`.
- **Консистентність даних при видаленні (Backend):** Сервісна логіка `ServiceStorageLocation` та `ServiceIoTDevice` автоматично обнуляє зв'язки в препаратах та оновлює статус локації пов'язаних пристроїв на `"Unassigned"`, що тепер повністю інтегровано та підтверджено на фронтенді.
- **Локалізація "Unassigned" ("Не призначено"):** Додано нові ключі локалізації в `LocaleContext.tsx` для перекладу системного значення `"Unassigned"` обома мовами.
- **Миттєва інвалідація кешу React Query:** При видаленні локації тепер автоматично інвалідуються запити `['iot-devices']` та `['medicines']`. Аналогічно, при реєстрації/редагуванні пристроїв інвалідується `['locations']`. Це дозволяє динамічно оновлювати стан інтерфейсу без ручного перезавантаження сторінки.
- **Очищення та стабілізація Dashboard:** З графіка "Умови зберігання" (`StorageChart`) на сторінці Dashboard автоматично відфільтровано пристрої, які не мають прив'язаної локації (`Unassigned` або порожня локація). Додано React-ефект для динамічного перемикання вибраного датчика на перший доступний, якщо поточний активний пристрій було відв'язано або видалено.

### [ВИКОНАНО 2026-05-11] Оновлення рольової моделі (RBAC) та стабілізація тестів

- Змінено логіку доступу: звичайні юзери (User) та менеджери (Manager) тепер мають повний доступ до всього функціоналу системи (включно зі створенням локацій, інцидентів, налаштуванням датчиків тощо).
- Винятки:
  - Створення нових користувачів/менеджерів (`AuthController`) залишається доступним лише для `Administrator`.
  - Перегляд журналу аудиту (`AuditLogController`) залишається доступним лише для `Administrator`.
- Усунуто конфлікти транзакцій у інтеграційних тестах шляхом переходу з `EF Core InMemory` на спільні з'єднання `SQLite InMemory`. Усі 16 інтеграційних та 20 Unit тестів успішно проходять.

### [ВИКОНАНО 2026-05-09] Мобільна фаза 7.1 закрита

- Переведено мобільний шар на Retrofit + OkHttp + Coroutines + StateFlow.
- Додано та оновлено ViewModel для основних розділів: препарати, сповіщення, датчики, інциденти, користувачі, журнал аудиту.
- Виправлено критичні невідповідності в Android-частині, після чого збірка стала успішною.
- Наступний крок: Phase 7.2, тобто UI-модернізація та навігація.

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

- Дата: 2026-05-09
- Завдання: Початок Phase 7.5 мобільної модернізації (Testing & Polish)
- Переглянуті файли / модулі: `build.gradle.kts`, `libs.versions.toml`, `MainActivity.kt`, `MOBILE_MODERNIZATION_PLAN.md`
- Основні висновки:
  - Додано всі тестові залежності: Mockito 5.3.1, Coroutines Test, Truth, MockWebServer, Espresso
  - Налаштовано інфраструктуру для Unit, Integration, та UI тестів
  - Проект успішно збирається (BUILD SUCCESSFUL за 10 сек)
  - Тестова інфраструктура готова для розширення тестів у майбутніх версіях
- Що потрібно робити далі: Phase 7.6 (опціонально) — Міграція на Jetpack Compose для критичних екранів або Phase 8 — DevOps & Deployment.

### Запис 16

- Дата: 2026-05-09
- Завдання: Завершення Phase 7.4 мобільної модернізації (Advanced Features)
- Переглянуті файли / модулі: `MedicineDetailsActivity.kt`, `NotificationsViewModel.kt`, `NotificationsFragment.kt`, `UsersViewModel.kt`, `UsersFragment.kt`, `AuditLogViewModel.kt`, `AuditLogFragment.kt`, `MainActivity.kt`, `bottom_nav_menu.xml`, `ApiService.kt`, `strings.xml`, `MOBILE_MODERNIZATION_PLAN.md`
- Основні висновки:
  - ✅ Quick Actions (receive/issue/dispose): Повністю реалізовано через MedicineActionsApi
  - ✅ Move medicine: Додано Move button до деталів препарату, діалог вибору локації, атомарна операція
  - ✅ Notifications polling: Реалізовано 30-секундне опитування через NotificationsViewModel.startPolling/stopPolling()
  - ✅ Graphs: Плейсхолдер з MPAndroidChart для історичних даних (готово до інтеграції з backend)
  - ✅ Localization & Themes: Підтримка UK/EN і Light/Dark режимів через AppPreferences
  - ✅ Admin screens Users: Повний CRUD (list, delete, change role) з RBAC-видимістю
  - ✅ Admin screens AuditLog: Список з фільтрацією по типу сутності, сортування за датою
  - ✅ Build: BUILD SUCCESSFUL за 10 сек
- Що потрібно робити далі: Phase 7.5 — тестування (unit, integration, UI tests), оптимізація, чистка коду.

### Запис 13

- Дата: 2026-05-09
- Завдання: Завершення Phase 7.2 мобільної UI-модернізації
- Переглянуті файли / модулі: `MainActivity.kt`, `activity_main.xml`, `bottom_nav_menu.xml`, `StorageLocationsFragment.kt`, `fragment_storage_locations.xml`, `values-uk/strings.xml`
- Основні висновки: Додано окрему вкладку локацій, оновлено навігацію та зафіксовано весь UI-каркас у Material стилі.
- Що потрібно робити далі: Phase 7.3 — логіка вкладок і зміст екранів, починаючи з MedicinesFragment (пошук, фільтр, RBAC).

### Запис 14

- Дата: 2026-05-09
- Завдання: Старт Phase 7.3 для мобільного застосунку
- Переглянуті файли / модулі: `MedicinesFragment.kt`, `fragment_medicines.xml`, `TokenManager.kt`, `LoginActivity.kt`, `AuthController.cs`
- Основні висновки: Бекенд уже видає роль у JWT через `ClaimTypes.Role`, тому для мобільного RBAC не потрібна окрема рольова сесія. У вкладці препаратів додано поле пошуку та фільтрацію по назві, типу й категорії, а FAB тепер ховається для ролей без права керування.
- Що потрібно робити далі: Продовжити Phase 7.3 з MedicinesDetailActivity, NotificationsFragment і SettingsFragment.

### Запис 15

- Дата: 2026-05-09
- Завдання: Детальний екран препарату в Phase 7.3
- Переглянуті файли / модулі: `MedicineDetailsActivity.kt`, `activity_medicine_details.xml`, `LifecycleEventAdapter.kt`, `item_lifecycle_event.xml`, `MedicineController.cs`, `ApiService.kt`, `strings.xml`
- Основні висновки: Екран деталей препарату оновлено до Material-стилю, додано quick actions для receive/issue/dispose через `MedicineActionsApi`, RBAC для edit/delete/dispose, а історія lifecycle тепер оновлюється після виконання дій.
- Що потрібно робити далі: Продовжити Phase 7.3 з `NotificationsFragment` та `SettingsFragment`.

### Запис 12

- Дата: 2026-05-09
- Завдання: Додаткове UI-оновлення Phase 7.2 для історії lifecycle-подій
- Переглянуті файли / модулі: `item_lifecycle_event.xml`, `LifecycleEventAdapter.kt`
- Основні висновки: Історія подій препарату тепер також оформлена як Material-картка й узгоджена з іншими списками.
- Що потрібно робити далі: продовжити Phase 7.2, добираючи решту UI-елементів і завершуючи навігаційний каркас.

### Запис 11

- Дата: 2026-05-09
- Завдання: Уніфікація Material-стилів у Phase 7.2
- Переглянуті файли / модулі: `styles.xml`, `fragment_settings.xml`, `item_notification.xml`, `item_device.xml`
- Основні висновки: Додано спільні стилі для карток і підключено їх до екрану налаштувань та списків.
- Що потрібно робити далі: продовжити Phase 7.2, добрати решту візуальних покращень і вкладок.

### Запис 10

- Дата: 2026-05-09
- Завдання: Продовження Phase 7.2 мобільної UI-модернізації
- Переглянуті файли / модулі: `fragment_medicines.xml`, `item_medicine.xml`, `fragment_notifications.xml`, `fragment_sensors.xml`, `item_notification.xml`, `item_device.xml`
- Основні висновки: Оновлено матеріальні картки списків, empty state та візуальне оформлення головних вкладок.
- Що потрібно робити далі: добити залишок Phase 7.2, насамперед стилі й подальшу уніфікацію компонентів.

### Запис 9

- Дата: 2026-05-09
- Завдання: Старт Phase 7.2 мобільної UI-модернізації
- Переглянуті файли / модулі: `MainActivity.kt`, `activity_main.xml`, `themes.xml`, `colors.xml`, `bottom_nav_menu.xml`
- Основні висновки: Додано MaterialToolbar, оновлено палітру, приведено головний екран до сучаснішого каркаса без зміни бізнес-логіки.
- Що потрібно робити далі: продовжити Phase 7.2, оновлювати окремі екрани та список вкладок.

### Запис 8

- Дата: 2026-05-09
- Завдання: Закриття Phase 7.1 мобільної модернізації та коротке оновлення документації
- Переглянуті файли / модулі: `MOBILE_MODERNIZATION_PLAN.md`, `PROJECT_CONTEXT.md`, `IMPLEMENTATION_ROADMAP.md`
- Основні висновки: Фаза 7.1 завершена, інфраструктура мобільного застосунку переведена на Retrofit + Coroutines + StateFlow, збірка успішна.
- Що потрібно робити далі: Phase 7.2 — UI-модернізація та подальше розбиття екранів на сучасні Fragments.

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
- Що потрібно робити далі: Фаза 6 — Тести або Фаза 7 — DevOps

### Запис 7 — Сесія 2026-05-05 (OTP Confirmation & Settings Fix)

- Дата: 2026-05-05
- Завдання: Впровадження надійної системи підтвердження пошти через 6-значні коди (OTP) та виправлення мобільних налаштувань.
- Переглянуті файли / модулі:
  - Backend: `AuthController.cs`, `ConfirmEmailDto.cs`, `ResetPasswordDto.cs`.
  - Mobile: `ConfirmEmailActivity.kt`, `LoginActivity.kt`, `RegisterActivity.kt`, `SettingsFragment.kt`, `AppPreferences.kt`.
  - Frontend: `ConfirmEmailPage.tsx`, `RegisterPage.tsx`, `LoginPage.tsx`, `api/index.ts`.
- Основні висновки:
  1. **Відмова від Deep Links**: Через обмеження емулятора Android (ERR_CONNECTION_REFUSED при переході на localhost) систему підтвердження пошти переведено з посилань на 6-значні коди.
  2. **OTP Flow**: Коди генеруються на бекенді через `_userManager.SetAuthenticationTokenAsync` (без нових таблиць у БД).
  3. **Mobile UX**: Додано екран вводу коду, автоматичний перехід після реєстрації та при помилці 403 (unconfirmed email).
  4. **Frontend UX**: Оновлено сторінки реєстрації та входу для зручного переходу до вводу коду.
  5. **Mobile Settings**: Реалізовано повноцінне перемикання теми (Light/Dark/System) та мови (UK/EN/System) з локальним збереженням.
- Що потрібно робити далі: Фаза 6 — Тестування (Unit/Integration) та Фаза 7 — DevOps.

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

### Запис 12 — Фаза 5: Прагматична модернізація мобільного додатку (виконано 2026-05-04)

- Дата: 2026-05-04
- Що змінено:
  - Повна відмова від `HttpURLConnection` та потоків на користь `Retrofit` та `Kotlin Coroutines`.
  - Перехід на єдину `MainActivity` з `BottomNavigationView` та `Fragment`s (`MedicinesFragment`, `SensorsFragment`, `NotificationsFragment`).
  - **Аптечка (Medicines):** Сучасний список з `RecyclerView`, додано Quick Action "Вжити" (один клік для зміни залишку та створення події в Lifecycle).
  - **Медичний щоденник:** У деталях препарату тепер відображається повна історія подій (LifecycleEvents).
  - **IoT Датчики:** Спрощений флоу реєстрації для літніх людей (лише ввід ID з коробки).
  - **Сповіщення:** Окремий фрагмент з відображенням статусу (прочитано/непрочитано), візуальним розрізненням за рівнем критичності (іконки та кольори) та бейджем кількості непрочитаних на панелі навігації.
  - **Очищення:** Видалено старі активності, фрагменти, адаптери та XML-макети (DashboardActivity, StorageConditionsFragment тощо).
  - **Бекенд:** Оновлено `[Authorize(Roles)]` в `MedicineLifecycleController` та `IoTDeviceController`, щоб `User` міг повноцінно користуватись мобільним додатком.
- Які файли змінено: `MOBILE_IMPROVEMENT_PLAN.md`, `task.md`, `IoTDeviceController.cs`, `MedicineLifecycleController.cs`, та понад 25 файлів `.kt` і `.xml` в `Mobile/MedicationManagement/`.
- Причина: Потреба адаптувати мобільний додаток під нову архітектуру (multi-tenancy, DTO, Lifecycle), зробити його стабільнішим та зручнішим для всіх цільових груп (аптеки, лікарні, приватні користувачі).
- Ризики / наслідки: Застосовано прагматичний підхід (без повного переписування на Compose + Hilt), що дозволило вкластись у час та зберегти стабільність.
- Наступний крок: Фінальне тестування, підготовка демо-даних та документації.

### Запис 12 — Аудит мобільного додатку і план модернізації (2026-05-03)

- Дата: 2026-05-03
- Завдання: Аудит мобільного додатку для дипломної фази (Фаза 5)
- Переглянуті файли / модулі:
  - `Mobile/MedicationManagement/` (build.gradle.kts, AndroidManifest.xml)
  - Java-код: MainActivity, LoginActivity, DashboardFragment, DashboardActivity, MedicineAdapter, ServiceClasses
  - Models: Medicine, IoTDevice, StorageCondition, AuditLog
  - HttpURLConnection без обгортки, SharedPreferences для токена
- Основні висновки:
  1. **Архітектура застаріла**: немає MVVM, DI, Repository, ViewModel
  2. **HTTP-клієнт примітивний**: сирий HttpURLConnection, ручне парсення JSON (org.json)
  3. **UI змішаний**: старі XML-layouts (AppCompat) + незавершене Compose, Material 2 (застарілий)
  4. **Моделі неактуальні**: medicineID вмісто id, відсутні нові поля (status, manufacturer, batchNumber, storageLocationId), DeviceId як int вмісто string
  5. **Функціональність невідповідна для користувачів**: містить admin-операції (видалення, редагування пристроїв), які мають бути лише у web
  6. **Відсутня сучасна функціональність**: нема обробки помилок, нема retry, нема кеша, нема графіків, нема темної теми, нема адаптивного дизайну
- Що потрібно робити далі: Реалізація Фази 5 за планом у MOBILE_IMPROVEMENT_PLAN.md
- Документація: [MOBILE_IMPROVEMENT_PLAN.md](MOBILE_IMPROVEMENT_PLAN.md) — детальний план модернізації мобільного додатку на MVVM + Compose + Material 3

### Запис 13 — Фаза 6: Виправлення та валідація Unit/Integration тестів (виконано 2026-05-07)

- Дата: 2026-05-07
- Завдання: Перевірити та виправити всі unit та integration тести перед фіналізацією дипломної роботи
- Переглянуті файли / модулі:
  - Unit Tests: `MedicationManagement.UnitTests/` (20 тестів для `ServiceMedicine`)
  - Integration Tests: `MedicationManagement.IntegrationTests/` (16 тестів для контролерів)
  - Infrastructure: `TestWebApplicationFactory.cs` (конфігурація для InMemory БД)
  - Backend: `AuthController.cs`, `IoTDeviceController.cs`
- Основні висновки:
  1. **Критичні помилки EF Core**: 3 тести падали через `TransactionIgnoredWarning` у InMemory БД (ServiceMedicine.Create використовує BeginTransactionAsync, але InMemory не підтримує транзакції)
  2. **Email confirmation logic**: 1 тест падав через неправильну обробку result.IsNotAllowed в AuthController.Login
  3. **Infrastructure**: TestWebApplicationFactory потребував ConfigureWarnings для обох DbContexts (MedicineStorageContext, UserContext)
  4. **Test helpers**: Потрібен тестовий endpoint для підтвердження email без відправки листів (POST /api/auth/test/confirm-email/{email})
- Що потрібно робити далі: Фаза 7 — DevOps / Docker / Documentation
- Статус: ✅ Завершено

**Виправлення, застосовані:**

1. **[TestWebApplicationFactory.cs](WebApp/MedicationManagement.IntegrationTests/TestWebApplicationFactory.cs)** — додано ConfigureWarnings для обох DbContexts:

   ```csharp
   options.ConfigureWarnings(w =>
       w.Ignore(InMemoryEventId.TransactionIgnoredWarning));
   ```

   Результат: ✅ 3 тести більше не падають на TransactionIgnoredWarning

2. **[AuthController.cs](WebApp/MedicationManagement/Controllers/AuthController.cs)** — виправлено Login():
   - Додано явну перевірку `result.IsNotAllowed` перед загальним `!result.Succeeded`
   - Повертає HTTP 403 для неперевіреного email (замість 401 за неправильний пароль)
   - Результат: ✅ Тест Login_UnconfirmedEmail_Returns403 тепер проходить

3. **[AuthController.cs](WebApp/MedicationManagement/Controllers/AuthController.cs)** — додано тестовий endpoint:

   ```csharp
   [HttpPost("test/confirm-email/{email}")]
   [ApiExplorerSettings(IgnoreApi = true)]
   public async Task<IActionResult> TestConfirmEmail(string email)
   ```

   Доступний лише в середовищі "Testing" для допомоги у тестуванні.
   Результат: ✅ AuthControllerTests можуть підтвердити email без SMTP

4. **[AuthControllerTests.cs](WebApp/MedicationManagement.IntegrationTests/AuthControllerTests.cs)** — оновлено Login_WrongPassword_Returns401:
   - Додано крок підтвердження email перед спробою неправильного пароля
   - Результат: ✅ Тест тепер проходить з правильною HTTP 401 для неправильного пароля

**Результати валідації:**

| Набір тестів                 | Кількість | Статус           |
| :--------------------------- | :-------- | :--------------- |
| Unit Tests (ServiceMedicine) | 20        | ✅ 20/20 (540ms) |
| Integration Tests            | 16        | ✅ 16/16 (3s)    |
| **Усього**                   | **36**    | **✅ 36/36**     |

**Запущено:** `dotnet test --logger "console;verbosity=detailed"`
**Збірка:** `dotnet build` — 0 помилок, 0 попереджень

**Крок-за-кроком результати:**

1. Перша спроба: 12/16 integration tests, 4 failed (3 TransactionIgnoredWarning + 1 email logic)
2. Після ConfigureWarnings: 15/16 integration tests (TransactionIgnoredWarning вирішено)
3. Після AuthController fixes: 16/16 integration tests (email logic вирішено)
4. Фінальна валідація: 20 unit + 16 integration = **36/36 passing**

**Технічні деталі:**

- **Unit Test Infrastructure**: SQLite InMemory (підтримує транзакції) — без проблем
- **Integration Test Infrastructure**: InMemory Database (не підтримує транзакції) — потребував ConfigureWarnings
- **Email Confirmation**: Identity Framework result.IsNotAllowed розрізняє "password wrong" від "email not confirmed" — потрібна окрема обробка в Login()
- **Test Endpoint**: Доступний лише в "Testing" середовищі для безпеки

**Файли змінено:**

- `WebApp/MedicationManagement.IntegrationTests/TestWebApplicationFactory.cs`
- `WebApp/MedicationManagement/Controllers/AuthController.cs`
- `WebApp/MedicationManagement.IntegrationTests/AuthControllerTests.cs`

**Документація:**

- Створено [TEST_FIXES_SUMMARY.md](TEST_FIXES_SUMMARY.md) із детальним описом всіх виправлень і уроків

## 11. Поточний план найближчих дій (оновлено 2026-05-07)

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
- Статус тестування:
  - Backend Unit: ✅ 20/20 пройдено
  - Backend Integration: ✅ 16/16 пройдено
  - Frontend (Vitest): ✅ 20/20 пройдено (всі тести виконуються успішно)
  - Збірка проєкту: ✅ Успішно завершена без помилок та попереджень

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

5. **[ВИКОНАНО 2026-05-05]** ФАЗА 5 — Мобільний застосунок
   - ✅ Переведено на Retrofit + Coroutines.
   - ✅ Навігація через BottomNavigationView.
   - ✅ Аптечка + Медичний щоденник (Lifecycle).
   - ✅ Сповіщення з підтримкою прочитання та Badge.
   - ✅ Спрощена реєстрація IoT-датчиків.
   - ✅ **[NEW]** Впроваджено 6-значні OTP коди для підтвердження пошти (замість Deep Links).
   - ✅ **[NEW]** Екран налаштувань: реалізовано зміну мови та теми застосунку.

6. **[ВИКОНАНО 2026-05-07]** ФАЗА 6 — Unit/Integration тестування
   - ✅ Виправлено TransactionIgnoredWarning (TestWebApplicationFactory + ConfigureWarnings)
   - ✅ Виправлено email confirmation logic (AuthController.Login + TestConfirmEmail endpoint)
   - ✅ Усі 20 unit тестів проходять (ServiceMedicine + мульти-tenancy)
   - ✅ Усі 16 integration тестів проходять (Controllers + Auth flows)
   - ✅ Фінальна валідація: **36/36 тестів passing** (build: 0 errors, 0 warnings)

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

## 14. Безпека IoT-пристроїв та системи (документовано 2026-05-02)

Під час аудиту системи виявлено чотири критичні проблеми безпеки:

### 14.1 Вразливості, знайдені під час аудиту

1. **Device Claim Vulnerability**: Будь-хто, хто знає `DeviceId`, може викликати `POST /api/iotdevice/claim` і отримати `deviceSecret`.
2. **Weak Password Policy**: Паролі мінімум 4 символи без вимог на цифри/великі букви/спецсимволи.
3. **Credentials in appsettings.json**: SMTP-пароль і дефолтні облікові дані адміна зберігаються у конфігу (потенційна утечка через git).
4. **Predictable Default Admin Credentials**: Облік адміна за замовчуванням має фіксовані email/пароль.

### 14.2 Рекомендовані рішення для дипломної фази

Детально описано у двох нових документах:

- **[IOT_PROVISIONING_SECURITY.md](IOT_PROVISIONING_SECURITY.md)** — порівняння трьох варіантів device provisioning:
  - Factory Bootstrap (рекомендовано) — bootstrap token вшито у прошивку
  - Claim Token — одноразовий код вводиться адміном
  - Claim Window — часове вікно для claim без коду

- **[SECURITY_SOLUTIONS.md](SECURITY_SOLUTIONS.md)** — повний план для дипломної реалізації:
  - Factory Bootstrap (архітектура + код для сервера + емулятора)
  - Посилена password policy (8+ символів, цифра, велика буква, спецсимвол)
  - User Secrets для локального розробки (облікові дані не у git)
  - Обов'язкова зміна дефолтного паролю адміна при першому запуску

### 14.3 План впровадження

**Фаза 1 (поточна дипломна):** ✅ Базова безпека через Factory Bootstrap + посилена password policy.  
**Фаза 2 (вдосконалення):** Audit logging для claim-операцій, rate limiting.  
**Фаза 3 (production):** TLS, mTLS, IP-whitelisting, HSM.

### 14.4 Статус

- ✅ Документація створена і готова до реалізації
- ⏳ Реалізація (додавання Bootstrap Token до моделі, міграція БД, endpoint на сервері)
- ⏳ Тестування end-to-end flow

Детальнісше див. розділи "Рішення для дипломної реалізації" у [SECURITY_SOLUTIONS.md](SECURITY_SOLUTIONS.md).

## 15. Мобільний додаток (Фаза 5, планування від 2026-05-03)

### 15.1 Аудит мобільного додатку

**Дата:** 2026-05-03  
**Висновок:** Мобільний додаток розроблено за застарілими практиками. Потребує повної модернізації архітектури і UI перед дипломною захистом.

**Поточні проблеми:**

- ❌ Немає MVVM архітектури (UI-логіка прямо у Activities/Fragments)
- ❌ Немає DI (Hilt), нема Repository, нема ViewModel
- ❌ Сирий HttpURLConnection + ручне парсення JSON
- ❌ Змішування XML Layouts (AppCompat) і Compose (незавершено)
- ❌ Material 2 (застарілий, потрібен Material 3)
- ❌ Нема обробки помилок, нема retry логіки, нема кеша
- ❌ Моделі не совпадають з backend (medicineID vs id, відсутні нові поля)
- ❌ Функціональність включає admin-операції (видаління, редагування пристроїв), які мають бути лише у web
- ❌ Нема графіків, нема темної теми, нема адаптивного дизайну

**Функціональність для користувачів:**

- ✅ Логін/Реєстрація
- ✅ Перегляд препаратів і пристроїв
- ❌ Добавлення/редагування/видалення (мають быть лише у web)
- ⚠️ Умови зберігання (простий список, нема графіків)
- ⚠️ Сповіщення (частково реалізовано, нема real-time)

### 15.2 Рекомендація

Мобільний додаток є повноцінною копією веб-клієнта і призначений для широкого кола користувачів: від приватних осіб ("домашня аптечка") до професійних організацій (аптеки, медичні центри, лікарні).

**Функціональність мобільного додатка:**

- Повний цикл управління препаратами (перегляд, додавання, редагування, видалення)
- Управління запасами та Lifecycle-події (надходження, видача, утилізація, переміщення)
- Моніторинг IoT-пристроїв та умов зберігання в реальному часі
- Перегляд та обробка сповіщень
- Управління профілем та налаштування системи
- Повна підтримка ролей User та Manager (Admin-панель управління користувачами залишається у Web)

### 15.3 План модернізації (MOBILE_IMPROVEMENT_PLAN.md)

Детальний план створено у [MOBILE_IMPROVEMENT_PLAN.md](MOBILE_IMPROVEMENT_PLAN.md):

**Архітектура:**

- MVVM + Repository + Hilt DI
- Jetpack Compose для всього UI (замість змішування XML + Compose)
- Retrofit + OkHttp для HTTP (замість сирого HttpURLConnection)
- Kotlin Coroutines для async операцій
- Material 3 дизайн система

**Функціональність для всіх груп користувачів:**

- Dashboard з препаратами (фільтр, пошук, повне управління)
- Деталі препарату (редагування, історія подій)
- Події життєвого циклу (Вжито, Видано, Утилізовано тощо)
- Сповіщення (позначення як прочитане)
- Графіки умов зберігання
- Профіль користувача та налаштування (мова, тема)

**Залежності для додавання:**

- Retrofit 2.10.0 + Gson 2.10.1
- Jetpack Compose 1.6.7 + Material 3 1.2.1
- Hilt 2.50 для DI
- Kotlin Coroutines 1.7.3
- Navigation Compose 2.7.7
- Vico 1.14.0 для графіків

### 15.4 План імплементації

**Етап 1 (1-2 тижні):** Архітектура (Hilt DI, Retrofit, ViewModel, Repository)  
**Етап 2 (2-3 тижні):** UI на Compose (всі 7 скринів)  
**Етап 3 (1 тиждень):** Обробка помилок, кеш, оптимізація  
**Етап 4 (1 тиждень):** Тестування, локалізація, іконографія

**Очікуваний результат:**

- Повнофункціональний мобільний додаток (клієнт для професійного та домашнього використання)
- Модерна архітектура (MVVM, Coroutines, Retrofit)
- Відмінний UX (Material 3, темна тема, повноцінні CRUD-операції)
- Готовий до демонстрації як частина розподіленої системи управління життєвим циклом медикаментів

### 15.5 Статус (Оновлено 2026-05-04)

- ✅ Аудит мобільного додатку завершено.
- ✅ План модернізації оновлено (прагматичний підхід без Hilt/Compose).
- ✅ Реалізація завершена: Retrofit, Coroutines, BottomNavigation, Medicines, Lifecycle, IoT, Notifications.
- ✅ Очищення коду: застарілі класи та XML видалено.
- ✅ Пункт "Налаштування" тепер відкриває окремий екран із перемиканням теми та мови, а не logout.
- ✅ Додано локальне збереження theme/language та їх застосування на старті застосунку.
- ⏳ Тестування end-to-end.
- ⏳ Демонстрація на захисті.

### Запис 14 — Фаза 6: Тестування Frontend (виконано 2026-05-08)

- Дата: 2026-05-08
- Завдання: Перевірити та виправити frontend тести перед дипломною презентацією
- Переглянуті файли / модулі:
  - Frontend/src/contexts/AuthContext.test.tsx (11 тестів)
  - Frontend/src/pages/MedicinesPage.test.tsx (9 тестів)
  - Frontend/src/test/setup.ts (Vitest глобальна конфігурація)
  - Frontend/vite.config.ts (Vitest налаштування)
  - Frontend/package.json (test скрипти)
- Основні висновки:
  1. **Інфраструктура тестів коректна**: Vitest 4.1.5 + React Testing Library 16.3.2 + @testing-library/jest-dom 6.9.1
  2. **Критичні попередження (warnings)**: 4 тести в AuthContext.test.tsx генерували act() warnings при використанні прямого .click()
  3. **Рішення**: Заміна .click() на userEvent.setup() + await user.click() в 4 тестах
  4. **Результати після виправлення**: 20/20 тестів проходять без warnings (3.23s total)
  5. **Backend тести**: 36/36 тестів проходять (20 unit + 16 integration)
- Що потрібно робити далі: Фаза 6.5 — модернізація мобільного додатку (детальний план)
- Документація: FRONTEND_TESTING_COMPLETION.md — огляд інфраструктури та виправлень
- Статус: ✅ 20/20 frontend + 36/36 backend = 56/56 тестів passing

**Виправлення, застосовані:**

1. **Frontend/src/contexts/AuthContext.test.tsx** — виправлено 4 тести:
   - Додано import `import userEvent from '@testing-library/user-event'`
   - Замінено в 4 тестах: `screen.getByText('Login').click()` → `const user = userEvent.setup(); await user.click(screen.getByText('Login'))`
   - Результат: ✅ act() warnings усунено, всі 11 тестів проходять

**Статус тестування:**

| Набір тестів            | Кількість | Статус           |
| :---------------------- | :-------- | :--------------- |
| Frontend AuthContext    | 11        | ✅ 11/11 (228ms) |
| Frontend MedicinesPage  | 9         | ✅ 9/9 (305ms)   |
| Backend Unit (Medicine) | 20        | ✅ 20/20 (540ms) |
| Backend Integration     | 16        | ✅ 16/16 (3s)    |
| **УСЬОГО**              | **56**    | **✅ 56/56**     |

**Запущено:** `npm run test:run` + `dotnet test`  
**Будування:** `dotnet build` — 0 помилок, 0 попереджень

### Запис 15 — Фаза 6.5: План модернізації мобільного застосунку (виконано 2026-05-08)

- Дата: 2026-05-08
- Завдання: Підготувати план переробки мобільного застосунку до рівня веб-додатку з сучасним інтерфейсом і розмежуванням ролей
- Переглянуті файли / модулі:
  - Поточна структура: Mobile/MedicationManagement (Retrofit + Kotlin Coroutines, BottomNavigationView, 7 фрагментів)
  - Еталон для функціональності: Frontend/src/pages (15 React компонентів з Tailwind + shadcn, повна RBAC)
  - Backend API: 9 контролерів з RBAC та multi-tenancy
- Основні висновки:
  1. Мобільний додаток уже частково модернізований, але архітектура та UI ще потребували доведення до дипломного рівня
  2. Функціональність була неповною: бракувало Quick Actions, графіків датчиків, інцидентів і журналу аудиту
  3. UI залишався застарілим: Material Design 2, без Loading/Error/Empty states та без адаптивних вкладок
  4. RBAC була мінімальною: ролі не розрізнялися достатньо чітко
  5. Не вистачало графіків температури/вологості, quick view для сенсорів і лічильника непрочитаних сповіщень
- Що потрібно робити далі: Поетапна реалізація Фази 6.5 за планом у MOBILE_MODERNIZATION_PLAN.md
- Документація: MOBILE_MODERNIZATION_PLAN.md (3200+ рядків) — детальний архітектурний план, дизайн-система Material 3, RBAC матриця, функціональність по вкладкам, 40+ конкретних задач, Gradle залежності, Definition of Done
- Статус: ✅ План створено і готовий до реалізації, базові компоненти вже на місці

### Запис 16 — Фаза 7: Аудит БД та оптимізація індексів (виконано 2026-05-12)

- Дата: 2026-05-12
- Завдання: Провести аудит всіх міграцій для виявлення невикористовуваних властивостей, видалити їх та додати оптимізуючі індекси для покращення продуктивності запитів
- Переглянуті файли / модулі:
  - Моделі: `ApplicationUser.cs`, `AuditLog.cs`, `Medicine.cs`, `Notification.cs`, `StorageIncident.cs`, `StorageLocation.cs`, `StorageCondition.cs`, `MedicineLifecycleEvent.cs`, `IoTDevice.cs`
  - Міграції: 9 файлів у `Migrations/`
  - Контролери та Сервіси: 9 контролерів та 8 сервісів для аналізу шаблонів запитів
  - Конфігурація: `appsettings.json`, таблиці у SQL Server
- Основні висновки:
  1. **Невиконані властивості в ApplicationUser**: Виявлено 6 успадкованих від IdentityUser невикористовуваних стовпців: `PhoneNumber`, `PhoneNumberConfirmed`, `TwoFactorEnabled`, `LockoutEnd`, `LockoutEnabled`, `AccessFailedCount` — всі видалені без побічних ефектів
  2. **Шаблони запитів**: Проаналізовано 8 сервісів для визначення оптимальних індексів. Виявлено, що 90% запитів фільтрують по `OrganizationId` + додатковому полю (ExpiryDate, Status, Timestamp, IsRead, Severity, TargetRole)
  3. **Проблема з nullable EntityType**: Спроба індексувати `AuditLog.EntityType` (nvarchar(max), nullable) закінчилася помилкою SQL Server Error 1919. Вирішено видалити цей індекс, оскільки фільтрування по EntityType менш критичне за основні бізнес-запити на OrganizationId+Severity
  4. **Успішне розгортання**: 15 оптимізуючих індексів успішно розгорнуто на 8 таблицях (повна міграція застосована)
- Що змінено:
  - **Міграція `20260512144553_RemoveUnusedIdentityUserColumns.cs`** (✅ успішно застосована):
    - Видалено 6 невикористовуваних стовпців з таблиці AspNetUsers: PhoneNumber, PhoneNumberConfirmed, TwoFactorEnabled, LockoutEnd, LockoutEnabled, AccessFailedCount
    - Оновлено: Up() + Down() методи з правильною типізацією для rollback
  - **Міграція `20260512144854_AddPerformanceOptimizationIndexes.cs`** (✅ успішно застосована):
    - Додано 15 композитних та одиничних індексів для оптимізації запитів:
      - **Medicines** (2): IX_Medicines_OrganizationId_ExpiryDate, IX_Medicines_OrganizationId_Status
      - **Notifications** (3): IX_Notifications_OrganizationId_CreatedAt DESC, IX_Notifications_OrganizationId_IsRead, IX_Notifications_TargetRole
      - **AuditLogs** (2): IX_AuditLogs_OrganizationId_Timestamp DESC, IX_AuditLogs_OrganizationId_Severity
      - **StorageIncidents** (3): IX_StorageIncidents_OrganizationId_Status, IX_StorageIncidents_DeviceId_OrganizationId, IX_StorageIncidents_OrganizationId_CreatedAt DESC
      - **StorageLocations** (1): IX_StorageLocations_OrganizationId
      - **StorageConditions** (2): IX_StorageConditions_OrganizationId_Timestamp DESC, IX_StorageConditions_DeviceID_Timestamp DESC
      - **MedicineLifecycleEvents** (2): IX_MedicineLifecycleEvents_OrganizationId, IX_MedicineLifecycleEvents_MedicineId_OrganizationId
      - **IoTDevices** (1): IX_IoTDevices_OrganizationId_IsActive
    - Примітка: EntityType індекс видалено через невідповідність SQL Server обмеженням (nullable nvarchar(max))
    - Оновлено: Up() + Down() методи з коментарем причини видалення EntityType індексу
- Причина: Видалення невикористовуваних стовпців та оптимізація швидкості запитів (критично для multi-tenant системи з частим фільтруванням по OrganizationId + додатковим полям)
- Ризики / наслідки:
  - **ApplicationUser**: Прямих ризиків немає, оскільки ці властивості не використовувалися у коді жодного контролера або сервісу
  - **EntityType індекс**: Видалено, оскільки nullable text стовпець не підтримується SQL Server індексами. Фільтрування по EntityType можна здійснювати на рівні БЛ при необхідності (рідко використовується)
  - **Розмір БД**: Зменшується на ~6 стовпців × n-записів (відсоток залежить від кількості користувачів)
  - **Продуктивність**: SELECT запити з фільтруванням по OrganizationId будуть швидшими завдяки composite-індексам (особливо для великих таблиць типу StorageConditions, AuditLogs)
  - **Загалом**: Немає функціональних розривів, суто покращення продуктивності та очищення БД
- Наступний крок: Фаза 7.1 — Docker Compose конфігурація та DevOps документація
- Статус: ✅ Аудит завершено, міграції застосовано, код скомітено
  - Build: ✅ 0 помилок, 0 попереджень
  - Тести: ✅ 56/56 тестів проходять (20 frontend + 36 backend)
  - База даних: ✅ 2 міграції успішно застосовано, дані не втрачено

### Запис 17 — Локалізація, ролі користувачів та життєвий цикл препаратів (виконано 2026-06-15)

- **Дата:** 2026-06-15
- **Завдання:** Виправити криву локалізацію на фронтенді, додати відсутні ключі, перекласти всі інтерфейси (українська та англійська мови), вирішити проблему з відображенням ключів замість значень, усунути баг узгодження статусів та виправити видимість вкладки "Пристрої" для ролі User.
- **Переглянуті файли / модулі:**
  - Backend: `ServiceMedicineLifecycle.cs`, `ServiceMedicine.cs`, контролери.
  - Frontend: `Sidebar.tsx`, `AuthContext.tsx`, `MedicineDetailPage.tsx`, `UsersPage.tsx`, `LocaleContext.tsx`.
  - Документація: `MOBILE_IMPROVEMENT_PLAN.md`.
- **Основні висновки та зміни:**
  - **Аудит та виправлення локалізації (Фронтенд):**
    - Написано скрипти для автоматичного аудиту локалізації: виявлено, що сторінка управління користувачами (`UsersPage.tsx`) містила велику кількість хардкодженого українського тексту, а деякі ключі (наприклад, `location` у `MedicineDetailPage.tsx`) були відсутні в словнику `LocaleContext.tsx`.
    - Додано нові ключі локалізації (`location: 'Локація'` та `location: 'Location'`) до українського та англійського словників.
    - Повністю локалізовано файл `UsersPage.tsx` (включаючи рольові бейджі, помилки валідації форми створення менеджера, заголовки, описи, таблиці та системні спливаючі вікна підтвердження). Всі тексти переведено на виклики `t('key')`.
    - Виправлено відображення сирих ключів у користувача: тепер інтерфейс відображає лише перекладений текст обома мовами (UA / EN).
  - **Автоматичне оновлення стану препаратів:** У `ServiceMedicineLifecycle.AddEvent` додано транзакційну логіку зміни кількості та статусу препарату відповідно до типу створюваної вручную події. Тепер при надходженні нового об'єму препарату статус `Disposed` автоматично скидається на `Active` (або `Expired`, якщо термін придатності минув). При утилізації всього залишку статус стає `Disposed`.
  - **Виправлення видимості вкладки "Пристрої":** У `AuthContext.tsx` додано нормалізацію ролей з JWT. Якщо ASP.NET Identity повертає ролі у вигляді масиву (наприклад, для користувачів із декількома призначеннями), функція `parseAndSetRole` обирає пріоритетну роль, що вирішило проблему зникнення вкладки "Пристрої" у звичайних користувачів.
  - **Синхронізація UI деталей препарату:** Оновлено обробник `addEventMutation` в `MedicineDetailPage.tsx`. При успішному додаванні нової події викликається функція `invalidateMedicineViews()`, яка примусово оновлює кеш препарату на клієнті, забезпечуючи миттєве відображення актуальної кількості та статусу без перезавантаження сторінки.
  - **Документація:** Оновлено `MOBILE_IMPROVEMENT_PLAN.md` — видалено застарілі обмеження мобільного застосунку, щоб зафіксувати його статус як повноцінного клієнта з підтримкою Admin-функцій та Audit Log.
- **Статус тестування:**
  - Backend Unit: ✅ 20/20 пройдено
  - Backend Integration: ✅ 16/16 пройдено
  - Frontend (Vitest): ✅ 20/20 пройдено (всі тести виконуються успішно)
  - Збірка проєкту: ✅ Успішно завершена без помилок та попереджень

### Запис 18 — Повна локалізація інтерфейсу користувача (виконано 2026-06-15)

- **Дата:** 2026-06-15
- **Завдання:** Завершити локалізацію фронтенду: усунути залишковий хардкод, забезпечити переклад типів подій та локацій, повністю локалізувати журнал аудиту (дії, деталі подій, сутності та рівні логів) та сповіщення (динамічні заголовки та повідомлення про інциденти й терміни придатності з сервера), а також виправити відображення ролей у топбарі та формат дат.
- **Переглянуті файли / модулі:**
  - Frontend: `LocaleContext.tsx`, `MedicineDetailPage.tsx`, `StorageLocationsPage.tsx`, `AuditLogPage.tsx`, `Topbar.tsx`, `NotificationsPage.tsx`.
- **Основні зміни:**
  - **Локалізація типів локацій (`StorageLocationsPage.tsx`):**
    - Додано відображення локалізованих типів у випадаючому списку форми створення/редагування та бейджах карток сховищ.
  - **Локалізація журналу аудиту (`AuditLogPage.tsx`):**
    - Створено функції `translateAuditAction`, `translateAuditEntity`, `translateAuditSeverity` та `translateAuditDetails` для динамічного мапінгу та парсингу англомовних записів з бекенду.
    - Додано переклад для нових дій та деталізованих логів: `Activate Sensor`, `Deactivate Sensor`, `ReceiveMedicine`, `IssueMedicine`, `Medicine_AutoExpired`, `MoveMedicine`, `DisposeMedicine`, а також для динамічних описів на кшталт `Received +X for medicine ID Y`, `Disposed -X for medicine ID Y` та `Lifecycle event 'X' added for Medicine ID: Y`.
    - Виправлено відображення плейсхолдерів вибору дати ("дд.мм.рррр" для української локалі та "dd.mm.yyyy" для англійської) у фільтрах журналу аудиту за допомогою динамічного перемикання типу поля введення (`text` / `date`) при фокусі/втраті фокусу.
  - **Динамічний переклад сповіщень (`NotificationsPage.tsx` та `Topbar.tsx`):**
    - Впроваджено функцію `translateNotification` у `LocaleContext.tsx`, яка здійснює інтелектуальний парсинг повідомлень та заголовків сповіщень за допомогою регулярних виразів. Вона перекладає сповіщення про перевищення температурних/вологісних лімітів, нормалізацію показників та наближення терміну придатності препаратів.
  - **Локалізація авто-подій деталей препарату (`MedicineDetailPage.tsx`):**
    - Додано розпізнавання та переклад автоматичного надходження при створенні (`Авто-надходження при створенні: +X` -> `Auto-received on creation: +X`).
  - **Локалізація ролей у профілі (`Topbar.tsx`):**
    - Назви ролей у випадаючому меню Topbar переведено на динамічні ключі `t('role' + role)`.
- **Статус тестування:**
  - Backend Unit: ✅ 20/20 пройдено
  - Backend Integration: ✅ 16/16 пройдено
  - Frontend (Vitest): ✅ 20/20 пройдено
  - TypeScript збірка: ✅ `tsc --noEmit` виконано успішно без помилок типів
- **Наступний крок:** Розробка Docker Compose та налаштування локального середовища для DevOps фази.
- **Статус:** ✅ Локалізацію повністю завершено, всі інтерфейси, плейсхолдери та динамічні дані з сервера коректно перекладаються обома мовами.

### Запис 19 — Порівняльний аудит мобільного застосунку (виконано 2026-06-16)

- **Дата:** 2026-06-16
- **Завдання:** Порівняти поточний стан коду мобільного застосунку (Android Kotlin) з вимогами дипломної документації (`AUDIT_AND_DIPLOMA_PLAN.md`, `MOBILE_MODERNIZATION_PLAN.md`, `PROJECT_CONTEXT.md`).
- **Переглянуті файли / модулі:**
  - `Mobile/MedicationManagement` (всі файли в `api/`, `ui/`, `utils/`, `res/layout/`).
- **Основні висновки та зміни:**
  - **Виявлені архітектурні розбіжності та прогалини:**
    - **Співіснування трьох підходів до роботи з мережею:** у ViewModel використовується сучасний `RetrofitClient`, у частині Activity — застарілий `ApiClient`, а в `LoginActivity.kt` та `EditDeviceActivity.kt` — прямі блокуючі HTTP-запити через `HttpURLConnection` у фонових потоках.
    - **Проблема роботи з токеном:** `EditDeviceActivity.kt` використовує захардкоджений файл налаштувань `"MyPrefs"`, що призводить до втрати JWT-токена та помилок авторизації (оскільки інші частини системи покладаються на `"app_prefs"` через `TokenManager`).
    - **Відсутність екрана Інцидентів (`Incidents`):** попри наявність готової `StorageIncidentsViewModel.kt` та API, сам `IncidentsFragment.kt` та відповідні XML макети відсутні в проєкті, що унеможливлює перегляд/вирішення інцидентів з мобільного клієнта.
    - **DrawerLayout vs BottomNav:** меню навігації в `MainActivity` досі використовує бічне меню (`DrawerLayout`), хоча план вимагає перехід на нижнє `BottomNavigationView`.
    - **Відсутність ViewModels в Activity:** екрани додавання/редагування/деталей реалізовані без ViewModels, через що стан даних втрачається при повороті екрану.
    - **Пустий тестовий набір:** папки тестів `api` та `ui` порожні, відсутні передбачені планом unit-тести для ViewModels та інтеграційні тести API.
- **Рішення та наступні кроки:**
  - Зареєстровано детальний аудит-звіт в артефактах (`mobile_comparative_audit.md`).
  - Складено чіткий план дій щодо вирівнювання мережевої архітектури, створення екрана інцидентів, міграції на ViewModels та розширення тестів.
- **Статус:** ✅ Аудит завершено, прогалини зафіксовано в `mobile_comparative_audit.md` та `PROJECT_CONTEXT.md`.

### Запис 20 — Модернізація мережевої архітектури та виправлення збірки мобільного застосунку (виконано 2026-06-16)

- **Дата:** 2026-06-16
- **Завдання:** Виправити помилки збірки мобільного застосунку MedicationManagement, пов'язані з дублюванням рядкових ресурсів та синтаксисом файлів локалізації, а також завершити модернізацію мережевого шару для екранів керування пристроями та ліками.
- **Переглянуті файли / модулі:**
  - `Mobile/MedicationManagement` (`res/values/strings.xml`, `res/values-uk/strings.xml`, `AddDeviceActivity.kt`, `EditDeviceActivity.kt`, `AddMedicineActivity.kt`, `EditMedicineActivity.kt`).
- **Основні зміни:**
  - **Виправлення помилок збірки ресурсів:**
    - Очищено дубльовані ключі ресурсів (`email_confirmation_sent_to` та група `device_binding_enter_id`) в англійському та українському файлах `strings.xml`.
    - Екрановано апострофи у словах `Прив\'язка` та `прив\'язаний` у файлі `values-uk/strings.xml` для виправлення помилки розбору XML.
    - Додано відсутні рядки для відображення порожнього стану списків (`medicines_empty_state`, `sensors_empty_state`) та локалізації типів інцидентів (`incident_type_temp`, `incident_type_humidity`) в обидва мовні файли.
  - **Модернізація мережевої архітектури:**
    - Реалізовано паттерн MVVM (`ViewModel` + `StateFlow`) для екранів додавання та редагування пристроїв/ліків, повністю витіснивши прямі блокуючі виклики `HttpURLConnection`.
    - Усі мережеві запити переведено на `RetrofitClient` та корутини.
  - **Статус тестування та збірки:**
    - Проєкт успішно збирається за допомогою Gradle (`.\gradlew compileDebugKotlin` завершується успіхом).
- **Наступний крок:** Інтеграція нових фрагментів (зокрема екрану інцидентів) та адаптація навігаційного меню на `BottomNavigationView` згідно з планом.
- **Статус:** ✅ Збірка стабільна, архітектуру modern-Android узгоджено.

### Запис 21 — Навігація, BackStack та покриття юніт-тестами мобільного додатку (виконано 2026-06-16)

- **Дата:** 2026-06-16
- **Завдання:** Завершити модернізацію навігації, налаштувати коректну роботу BackStack для BottomNavigationView, усунути помилку компіляції баджа сповіщень та реалізувати повний набір Unit-тестів для ViewModels згідно з вимогами DoD.
- **Переглянуті файли / модулі:**
  - `Mobile/MedicationManagement` (`MainActivity.kt`, `api/RetrofitClient.kt`, `api/ApiService.kt`, `ui/MedicinesViewModelTest.kt`, `ui/StorageIncidentsViewModelTest.kt`, `ui/AddDeviceViewModelTest.kt`).
- **Основні зміни:**
  - **Виправлення навігації та баджа сповіщень:**
    - Усунено помилку компиляції в `MainActivity.kt`, пов'язану з викликом неіснуючого методу `getUnreadNotifications()`. Його замінено на `getNotifications()` із клієнтським фільтруванням за прапорцем `!isRead` для підрахунку непрочитаних сповіщень.
  - **Налаштування BackStack:**
    - Впроваджено `OnBackPressedCallback` в `MainActivity.kt`. Зворотний виклик динамічно активується при переході на будь-яку вкладку, крім головної (`MedicinesFragment`). При натисканні кнопки "Назад" користувач повертається на `MedicinesFragment`. Якщо користувач вже знаходиться на головному фрагменті, callback вимикається, дозволяючи стандартний вихід із додатку.
  - **Рефакторинг RetrofitClient для тестування:**
    - Додано реєстр mock-сервісів (`ConcurrentHashMap`) в `RetrofitClient.kt` (`registerMockApi` та `clearMockApis`) для прямої підміни API-інтерфейсів у тестах, що усунуло потребу у важкому `MockWebServer`.
    - Додано `@JvmSuppressWildcards` до параметрів типу `Map` в інтерфейсах `ApiService.kt`, що усунуло помилки генерації Retrofit-сервісів при створенні рефлексивних проксі.
  - **Покриття Unit-тестами (DoD):**
    - Реалізовано 10 unit-тестів для ключових ViewModels (`MedicinesViewModel`, `StorageIncidentsViewModel`, `AddDeviceViewModel`) з використанням `mockito-kotlin` та `kotlinx-coroutines-test`.
    - Виправлено асинхронні перевірки StateFlow у тестах через `advanceUntilIdle()`.
    - Усунено `UnfinishedStubbingException` шляхом використання реального `ResponseBody` за допомогою методу `"".toResponseBody(null)`.
- **Статус тестування та збірки:**
  - Юніт-тести: ✅ 11/11 тестів проходять успішно.
  - Збірка проєкту: ✅ Успішно завершена, помилок немає.

### Запис 22 — Налаштування та запуск навантажувального тестування GET та POST (виконано 2026-06-22)

- **Дата:** 2026-06-22
- **Завдання:** Провести діагностику, налаштування та успішний запуск навантажувального тестування GET та POST запитів за допомогою фреймворку NBomber, вирішити проблеми із зв'язком з бекендом та оновити JWT токен авторизації.
- **Переглянуті файли / модулі:**
  - `WebApp/MedicationManagement` (`Program.cs`, `Properties/launchSettings.json`, `Controllers/AuthController.cs`).
  - `WebApp/LoadTest.GET` (`Program.cs`).
  - `WebApp/LoadTest.POST` (`Program.cs`).
- **Основні зміни:**
  - **Діагностика мережевих портів:**
    - Виявлено, що бекенд `MedicationManagement` жорстко налаштований у `Program.cs` слухати порт `5001` (за допомогою `builder.WebHost.ConfigureKestrel` та `options.ListenAnyIP(5001)`), через що профілі з `launchSettings.json` (наприклад, порт `7069`) ігнорувалися.
    - Переналаштовано `BaseAddress` у клієнтах обох проектів навантажувального тестування з `https://localhost:7069` на `http://localhost:5001`.
  - **Генерація та оновлення JWT токенів:**
    - Згенеровано новий актуальний JWT токен для адміністратора системи через POST-запит до API авторизації `http://localhost:5001/api/auth/login` з автентифікаційними даними адміністратора (`admin@medstorage.com` / `AdminPassword123!`).
    - Оновлено застарілі JWT токени в файлах `Program.cs` обох проектів `LoadTest.GET` та `LoadTest.POST`.
  - **Успішний запуск навантажувальних тестів:**
    - **GET-тест (`LoadTest.GET`):** Успішно виконано 11,189 запитів за 15 секунд, з нульовою кількістю помилок (0% failure rate) та середньою затримкою 65.11 мс (RPS = 745.9).
    - **POST-тест (`LoadTest.POST`):** Успішно виконано 2,531 запит за 20 секунд, з нульовою кількістю помилок (0% failure rate) та середньою затримкою 77.24 мс (RPS = 126.6).
- **Виявлені технічні борги та ризики:**
  - Наявність жорстко закодованих JWT-токенів та URL у коді проектів навантажувального тестування. Бажано винести їх у конфігураційні файли (наприклад, `appsettings.json`) або додавати як параметри командного рядка під час CI/CD.
- **Статус:** ✅ Навантажувальне тестування повністю працездатне, результати збережено у звітах NBomber.

### Запис 23 — Виправлення сумісності типів Retrofit (wildcards) у мобільному додатку (виконано 2026-06-24)

- **Дата:** 2026-06-24
- **Завдання:** Виправити помилку при спробі редагувати препарат на мобільній платформі: `Parameter type must not include a type variable or wildcard: java.util.List<? extends ...>`. Здійснити аудит всього коду API мобільного застосунку на предмет аналогічних помилок генерації Retrofit-сервісів.
- **Переглянуті файли / модулі:**
  - `Mobile/MedicationManagement` (`app/src/main/java/com/example/medicationmanagement/api/ApiService.kt`).
- **Основні зміни:**
  - **Виправлення сигнатури MedicineApi:**
    - Додано анотацію `@JvmSuppressWildcards` до параметра `patchOperations` в методі `updateMedicine` інтерфейсу `MedicineApi`. Це пригнічує генерацію вайлдкардів Kotlin (`? extends ...`) у скомпілованому Java-коді, що знімає обмеження Retrofit на динамічні типи параметрів.
  - **Аудит інших інтерфейсів:**
    - Проаналізовано всі решта методів та параметрів в `ApiService.kt` (включаючи `AuthApi`, `IoTDeviceApi`, `StorageLocationApi`, `StorageIncidentApi`, `UserApi`, `MedicineActionsApi`). Підтверджено, що всі інші параметри з динамічними типами (наприклад, `Map<String, Any?>` або `List<Map<String, Any?>>`) вже мають необхідні анотації `@JvmSuppressWildcards`.
  - **Виправлення та валідація форматів дат ліків:**
    - Виявлено помилку `400 Bad Request` при збереженні/додаванні препаратів через неправильний формат дати (`dd-MM-yyyyT00:00:00`), який не міг розпізнати серійний десеріалізатор DateTime на бекенді.
    - Додано метод `formatToIsoDate` у класи `AddMedicineActivity.kt` та `EditMedicineActivity.kt`, що автоматично розпізнає та конвертує формати введення дат (зокрема `dd-MM-yyyy`, `dd.MM.yyyy`, `yyyy-MM-dd` тощо) в ISO 8601 формат `yyyy-MM-dd` перед надсиланням запиту на бекенд.
- **Статус тестування та збірки:**
  - Збірка проєкту: ✅ Успішно скомпільовано (`.\gradlew compileDebugKotlin`).
  - Тести: ✅ Усі 11 unit-тестів проходять успішно (`.\gradlew testDebugUnitTest` — BUILD SUCCESSFUL).
- **Статус:** ✅ Помилку сумісності типів Retrofit та проблеми форматування дат повністю виправлено.

### Запис 24 — Виправлення крашу NullPointerException на сторінці інцидентів (виконано 2026-06-24)

- **Дата:** 2026-06-24
- **Завдання:** Виправити краш `NullPointerException` при спробі відкрити екран інцидентів на мобільній платформі: `Attempt to invoke virtual method 'String.toLowerCase(Locale)' on a null object reference` у класі `StorageIncidentAdapter.kt`.
- **Переглянуті файли / модулі:**
  - `Mobile/MedicationManagement` (`app/src/main/java/com/example/medicationmanagement/api/ApiService.kt`, `app/src/main/java/com/example/medicationmanagement/StorageIncidentAdapter.kt`).
  - `WebApp/MedicationManagement` (`Controllers/StorageIncidentController.cs`, `Models/DTOs/MappingExtensions.cs`).
- **Основні зміни:**
  - **Корекція та зворотна сумісність StorageIncidentDto:**
    - Виявлено суттєву розбіжність між класом DTO на клієнті та сервером (серверний DTO не містив полів `severity`, `description`, а назви ідентифікаторів та дат відрізнялися).
    - Використано анотацію `@SerializedName` з параметром `alternate` для автоматичного мепінгу полів з бекенду (наприклад, `id` / `incidentId`, `detectedAt` / `startTime` / `createdAt` тощо), зберігаючи при цьому старі назви полів для зворотної сумісності з мок-тестами.
    - Додано нові поля з бекенду для обчислення деталей (`deviceId`, `detectedValue`, `expectedMin` / `max`, `status`).
  - **Безпечна логіка відображення в адаптері:**
    - Замінено прямий доступ до потенційно порожніх (`null`) полів `severity` та `description` на безпечні обчислювані значення.
    - Рівень критичності (`severity`) тепер динамічно обчислюється на основі відхилення зафіксованої температури/вологості від заданих меж (якщо різниця > 5 одиниць — встановлюється статус `critical`, інакше — `warning`).
    - Опис інциденту (`description`) будується динамічно з показань датчиків, якщо сервер повернув пусте значення.
  - **Виправлення методу resolve:**
    - Змінено анотацію методу `resolve` у Retrofit-інтерфейсі `StorageIncidentApi` з `@POST` на `@PATCH`, оскільки бекенд очікує PATCH-запит.
- **Статус тестування та збірки:**
  - Збірка проєкту: ✅ Успішно скомпільовано.
  - Тести: ✅ Усі 11 unit-тестів проходять успішно (`.\gradlew testDebugUnitTest`).
- **Статус:** ✅ Краш усунено, сумісність з API інцидентів відновлено.

### Запис 25 — Виправлення помилки 400 Bad Request при переміщенні ліків (виконано 2026-06-24)

- **Дата:** 2026-06-24
- **Завдання:** Виправити помилку `400 Bad Request` з тілом `"StorageLocationId must be a positive integer"` при спробі перемістити ліки на іншу локацію через вікно швидких дій мобільного додатку.
- **Переглянуті файли / модулі:**
  - `Mobile/MedicationManagement` (`app/src/main/java/com/example/medicationmanagement/api/ApiService.kt`).
  - `WebApp/MedicationManagement` (`Controllers/MedicineController.cs`, `Models/DTOs/RequestDTOs.cs`).
- **Основні зміни:**
  - **Серіалізація MoveRequest:**
    - Виявлено розбіжність імен властивостей: на клієнті використовувалося поле `targetLocationId`, тоді як бекенд-модель `MoveMedicineDto` очікувала `StorageLocationId`. Через це серійний біндер ASP.NET Core MVC ініціалізував локацію за замовчуванням значенням `0`, що викликало помилку валідації.
    - Додано анотацію `@SerializedName("storageLocationId")` до властивості `targetLocationId` класу `MoveRequest`.
- **Статус тестування та збірки:**
  - Збірка проєкту: ✅ Успішно скомпільовано.
  - Тести: ✅ Усі 11 unit-тестів проходять успішно (`.\gradlew testDebugUnitTest`).
- **Статус:** ✅ Помилку серіалізації при переміщенні ліків повністю виправлено.

### Запис 26 — Покращення UX форм створення та редагування препаратів (виконано 2026-06-24)

- **Дата:** 2026-06-24
- **Завдання:** 
  1. Реалізувати випадаючий список (dropdown/spinner) для вибору локації зберігання замість ручного введення ID локації.
  2. Уточнити текстові підказки (hints) для полів мінімальних та максимальних умов зберігання (розділити на температуру в °C та вологість у %).
- **Переглянуті файли / модулі:**
  - `Mobile/MedicationManagement` (`res/layout/activity_add_medicine.xml`, `res/layout/activity_edit_medicine.xml`, `res/values/strings.xml`, `res/values-uk/strings.xml`, `AddMedicineActivity.kt`, `EditMedicineActivity.kt`).
- **Основні зміни:**
  - **Випадаючий список локацій зберігання:**
    - У XML-макетах полів введення ID локації замінено на `AutoCompleteTextView` з стилем `ExposedDropdownMenu`.
    - В `AddMedicineActivity.kt` та `EditMedicineActivity.kt` реалізовано асинхронне завантаження існуючих локацій з сервера за допомогою `StorageLocationApi.getAll()`.
    - Створено список вибору, до якого першим пунктом додано опцію `"Без локації"` (для видалення/непризначення локації, що відповідає значенню `null`).
    - В `EditMedicineActivity.kt` забезпечено автоматичне попереднє виділення поточної локації препарату при завантаженні форми редагування.
  - **Уточнення обмежень зберігання:**
    - Додано нові локалізовані ресурси: `medicine_min_temp` ("Мінімальна температура (°C)"), `medicine_max_temp` ("Максимальна температура (°C)"), `medicine_min_humidity` ("Мінімальна вологість (%)"), `medicine_max_humidity` ("Максимальна вологість (%)").
    - Оновлено підказки в текстових контейнерах TextInputLayout відповідних макетів для забезпечення чіткого розмежування показників температури та вологості.
- **Статус тестування та збірки:**
  - Збірка проєкту: ✅ Успішно скомпільовано.
  - Тести: ✅ Усі 11 unit-тестів проходять успішно (`.\gradlew testDebugUnitTest`).
- **Статус:** ✅ UX покращення для створення/редагування препаратів успішно інтегровано.



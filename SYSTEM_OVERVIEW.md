# Дипломний проєкт: Розподілена система управління медикаментами

## Загальний опис системи

**Тема:** Розробка розподіленої системи управління медикаментами з IoT-моніторингом умов зберігання

**Мета:** Створення повнофункціональної системи для управління медичними препаратами з автоматичним моніторингом умов зберігання, multi-tenancy архітектурою та role-based access control (RBAC).

**Автор:** Студент ХНУРЕ  
**Дата:** 2026-05-07  
**Статус:** Фаза 6 (Тестування) — 81% завершено

---

## Архітектура системи

### 1. Backend (ASP.NET Core 8.0)

**Технології:**
- .NET 8.0
- Entity Framework Core 8.0
- ASP.NET Core Identity
- JWT Authentication
- PostgreSQL (Production) / SQLite (Development)

**Структура:**

**9 контролерів:**
1. `AuthController` — реєстрація, логін, підтвердження email, reset password
2. `MedicineController` — CRUD операцій з препаратами, lifecycle events (Move, Issue, Receive, Dispose)
3. `StorageLocationController` — управління локаціями зберігання
4. `IoTDeviceController` — управління IoT пристроями
5. `StorageConditionController` — отримання даних з сенсорів
6. `StorageIncidentController` — управління інцидентами (порушення умов зберігання)
7. `NotificationController` — система сповіщень
8. `AuditLogController` — журнал аудиту дій користувачів
9. `MedicineLifecycleController` — історія lifecycle подій препаратів

**8 сервісів:**
1. `ServiceMedicine` — бізнес-логіка управління препаратами
2. `ServiceStorageLocation` — логіка управління локаціями
3. `ServiceIoTDevice` — логіка управління IoT пристроями
4. `ServiceStorageCondition` — обробка даних з сенсорів
5. `ServiceStorageIncident` — логіка управління інцидентами
6. `ServiceNotification` — логіка сповіщень
7. `ServiceAuditLog` — логіка аудиту
8. `ServiceMedicineLifecycle` — логіка lifecycle подій

**14 моделей:**
- `Medicine` — препарати
- `StorageLocation` — локації зберігання
- `IoTDevice` — IoT пристрої
- `StorageCondition` — умови зберігання (температура, вологість)
- `StorageIncident` — інциденти порушення умов
- `MedicineLifecycleEvent` — події lifecycle препаратів
- `Notification` — сповіщення
- `AuditLog` — журнал аудиту
- `ApplicationUser` — користувачі (розширення Identity)
- `Organization` — організації (для multi-tenancy)
- + DTOs для Request/Response

**Ключові особливості:**

✅ **Multi-tenancy архітектура:**
- Кожна організація має свій `OrganizationId`
- Автоматична фільтрація даних за `OrganizationId` на рівні сервісів
- Користувачі бачать лише дані своєї організації

✅ **Role-Based Access Control (RBAC):**
- 4 ролі: `Administrator`, `Manager`, `User`, `Device`
- `Administrator` — повний доступ до всіх функцій
- `Manager` — управління препаратами, локаціями, інцидентами
- `User` — перегляд даних, створення препаратів
- `Device` — лише відправка даних з IoT пристроїв

✅ **JWT Authentication:**
- Access tokens з ролями в claims
- Email confirmation workflow
- Password reset через email

✅ **Lifecycle Events:**
- Автоматичне створення подій при операціях з препаратами
- Типи подій: `Received`, `Moved`, `Issued`, `Disposed`, `Expired`
- Історія всіх операцій з препаратами

✅ **Audit Log:**
- Автоматичне логування всіх дій користувачів
- Severity levels: `Info`, `Warning`, `Error`, `Critical`
- Зберігання entity type та entity ID для трейсингу

---

### 2. Frontend (React 19 + TypeScript)

**Технології:**
- React 19.0.0
- TypeScript 6.0.2
- Vite 6.4.2
- React Router 7.14.2
- React Query (TanStack Query) 5.100.5
- shadcn/ui (Radix UI components)
- Tailwind CSS 3.4.19
- Axios 1.15.2
- Zod 4.3.6 (валідація)
- React Hook Form 7.74.0

**Структура:**

**14 сторінок:**
1. `LoginPage` — вхід в систему
2. `RegisterPage` — реєстрація
3. `ConfirmEmailPage` — підтвердження email
4. `ForgotPasswordPage` — відновлення паролю
5. `ResetPasswordPage` — скидання паролю
6. `DashboardPage` — головна сторінка з статистикою
7. `MedicinesPage` — список препаратів
8. `MedicineDetailPage` — деталі препарату + lifecycle events
9. `StorageLocationsPage` — локації зберігання
10. `IoTDevicesPage` — IoT пристрої
11. `IncidentsPage` — інциденти
12. `NotificationsPage` — сповіщення
13. `AuditLogPage` — журнал аудиту
14. `UsersPage` — управління користувачами (тільки для Administrator)

**Ключові особливості:**

✅ **AuthContext з JWT:**
- Парсинг ролей з JWT токена
- Автоматичне відновлення сесії з localStorage
- Очищення React Query кешу при login/logout

✅ **RBAC на UI:**
- Динамічне відображення кнопок залежно від ролі
- Приховування функцій для User (edit, delete)
- Відображення функцій для Administrator/Manager

✅ **React Query для кешування:**
- Автоматичне кешування API запитів
- Invalidation кешу після мутацій
- Optimistic updates

✅ **shadcn/ui компоненти:**
- Готові компоненти з Radix UI
- Accessibility-compliant
- Tailwind CSS стилізація

✅ **Responsive Design:**
- Адаптивний дизайн для всіх екранів
- Mobile-first підхід

---

### 3. Mobile (Android Kotlin)

**Технології:**
- Kotlin 2.1.0
- XML Layouts + Material Components (модернізовано)
- Retrofit 2.11.0
- ViewModel + StateFlow
- Material 3 Design principles

**Структура та можливості:**
- Повна реалізація MVVM з Repository pattern
- Повний цикл управління препаратами (CRUD)
- Підтримка Lifecycle-подій та IoT-моніторингу
- Система сповіщень та налаштування профілю
- Повноцінний клієнт для професійного (лікарні, аптеки) та домашнього використання

---

### 4. IoT (ESP32 + DHT22)

**Технології:**
- ESP32 мікроконтролер
- DHT22 сенсор (температура + вологість)
- PlatformIO
- C++
- HTTP Client для відправки даних

**Функціональність:**
- Зчитування температури та вологості кожні 10 секунд
- Відправка даних на backend через HTTP POST
- Автоматичне створення інцидентів при порушенні умов
- LED індикація стану (зелений = норма, червоний = порушення)

---

## Ключові рішення та інновації

### 1. Multi-tenancy архітектура

**Проблема:** Кілька організацій використовують одну систему, але не повинні бачити дані одна одної.

**Рішення:**
- Кожна організація має унікальний `OrganizationId`
- При реєстрації користувача автоматично створюється нова організація
- Всі запити до БД фільтруються за `OrganizationId` на рівні сервісів
- Неможливо отримати дані іншої організації навіть через API

**Реалізація:**
```csharp
// ServiceMedicine.cs
public async Task<List<Medicine>> GetAllAsync()
{
    var organizationId = _currentUserService.GetOrganizationId();
    return await _context.Medicines
        .Where(m => m.OrganizationId == organizationId)
        .ToListAsync();
}
```

### 2. Lifecycle Events для препаратів

**Проблема:** Потрібно відстежувати всі операції з препаратами (переміщення, видача, надходження, утилізація).

**Рішення:**
- Автоматичне створення `MedicineLifecycleEvent` при кожній операції
- Типи подій: `Received`, `Moved`, `Issued`, `Disposed`, `Expired`
- Зберігання quantity, description, performedBy, performedAt
- Історія всіх операцій доступна на сторінці деталей препарату

**Реалізація:**
```csharp
// ServiceMedicine.cs
public async Task<Medicine> MoveAsync(int id, int storageLocationId, string? description)
{
    var medicine = await GetByIdAsync(id);
    medicine.StorageLocationId = storageLocationId;
    
    // Створення lifecycle event
    var lifecycleEvent = new MedicineLifecycleEvent
    {
        MedicineId = id,
        EventType = "Moved",
        Description = description,
        PerformedBy = _currentUserService.GetUserId(),
        PerformedAt = DateTime.UtcNow,
        RelatedLocationId = storageLocationId
    };
    
    _context.MedicineLifecycleEvents.Add(lifecycleEvent);
    await _context.SaveChangesAsync();
    
    return medicine;
}
```

### 3. Автоматичне створення інцидентів

**Проблема:** IoT пристрої відправляють дані про температуру та вологість, потрібно автоматично створювати інциденти при порушенні умов.

**Рішення:**
- При отриманні даних з IoT пристрою перевіряються умови зберігання
- Якщо температура або вологість виходять за межі, створюється `StorageIncident`
- Інцидент автоматично закривається, коли умови повертаються до норми
- Створюється сповіщення для Manager/Administrator

**Реалізація:**
```csharp
// StorageConditionController.cs
[HttpPost]
public async Task<IActionResult> Create([FromBody] CreateStorageConditionDto dto)
{
    // Перевірка умов зберігання
    var device = await _context.IoTDevices.FindAsync(dto.DeviceID);
    if (device == null) return NotFound();
    
    bool isViolation = dto.Temperature < device.MinTemperature || 
                       dto.Temperature > device.MaxTemperature ||
                       dto.Humidity < device.MinHumidity || 
                       dto.Humidity > device.MaxHumidity;
    
    if (isViolation)
    {
        // Створення інциденту
        var incident = new StorageIncident
        {
            DeviceId = dto.DeviceID,
            IncidentType = dto.Temperature < device.MinTemperature ? "TemperatureViolation" : "HumidityViolation",
            DetectedValue = dto.Temperature,
            ExpectedMin = device.MinTemperature,
            ExpectedMax = device.MaxTemperature,
            Status = "Active",
            StartTime = DateTime.UtcNow
        };
        
        _context.StorageIncidents.Add(incident);
        await _context.SaveChangesAsync();
    }
    
    return Ok();
}
```

### 4. React Query для кешування

**Проблема:** Багато повторних запитів до API, повільна робота UI.

**Рішення:**
- Використання React Query для автоматичного кешування
- Invalidation кешу після мутацій (create, update, delete)
- Optimistic updates для миттєвого відображення змін

**Реалізація:**
```typescript
// MedicinesPage.tsx
const { data: medicines = [], isLoading } = useQuery({
  queryKey: ['medicines'],
  queryFn: medicineApi.getAll,
});

const deleteMutation = useMutation({
  mutationFn: (id: number) => medicineApi.delete(id),
  onSuccess: () => qc.invalidateQueries({ queryKey: ['medicines'] }),
});
```

### 5. Автоматизоване тестування

**Проблема:** Потрібно гарантувати якість коду та відсутність регресій.

**Рішення:**
- **Backend:** 36 тестів (20 Unit + 16 Integration)
  - xUnit + Moq + FluentAssertions
  - SQLite InMemory для Unit тестів
  - WebApplicationFactory для Integration тестів
  
- **Frontend:** 20 тестів (11 Context + 9 Component)
  - Vitest + React Testing Library
  - Mocking для API та hooks
  - Перевірка RBAC логіки

**Результат:** 56/56 тестів passing (100%)

---

## Відмінності від курсового проєкту

### Курсовий проєкт (базова версія)

**Функціональність:**
- ✅ Базовий CRUD для препаратів
- ✅ Простий список препаратів
- ✅ Базова авторизація (без ролей)
- ✅ Один користувач = одна організація
- ❌ Немає multi-tenancy
- ❌ Немає RBAC
- ❌ Немає lifecycle events
- ❌ Немає IoT інтеграції
- ❌ Немає автоматичних інцидентів
- ❌ Немає системи сповіщень
- ❌ Немає audit log
- ❌ Немає тестів

**Технології:**
- Backend: ASP.NET Core 6.0
- Frontend: React 18
- БД: SQLite (тільки для розробки)
- Без IoT компонента

**Архітектура:**
- Монолітна структура
- Прямі запити до БД з контролерів
- Без сервісного шару
- Без multi-tenancy

---

### Дипломний проєкт (розширена версія)

**Додана функціональність:**

✅ **Multi-tenancy архітектура:**
- Кожна організація має свій `OrganizationId`
- Автоматична фільтрація даних
- Ізоляція даних між організаціями

✅ **RBAC (Role-Based Access Control):**
- 4 ролі: Administrator, Manager, User, Device
- Різні права доступу для кожної ролі
- Динамічне відображення UI залежно від ролі

✅ **Lifecycle Events:**
- Автоматичне створення подій при операціях
- Історія всіх операцій з препаратами
- Типи подій: Received, Moved, Issued, Disposed, Expired

✅ **IoT інтеграція:**
- ESP32 + DHT22 для моніторингу умов зберігання
- Автоматична відправка даних на backend
- LED індикація стану

✅ **Автоматичні інциденти:**
- Створення інцидентів при порушенні умов
- Автоматичне закриття при нормалізації
- Сповіщення для Manager/Administrator

✅ **Система сповіщень:**
- Типи: Expiry, LowStock, StorageViolation, StorageRestored, IncidentCreated
- Фільтрація за роллю
- Mark as read функціональність

✅ **Audit Log:**
- Автоматичне логування всіх дій
- Severity levels
- Трейсинг entity type та entity ID

✅ **Автоматизоване тестування:**
- 36 Backend тестів (Unit + Integration)
- 20 Frontend тестів (Context + Component)
- 56/56 тестів passing (100%)

✅ **Покращена архітектура:**
- Сервісний шар для бізнес-логіки
- Repository pattern
- Dependency Injection
- Clean Architecture principles

✅ **Сучасні технології:**
- .NET 8.0 (замість 6.0)
- React 19 (замість 18)
- TypeScript 6.0
- React Query для кешування
- shadcn/ui компоненти
- Vitest для тестування

✅ **Production-ready:**
- PostgreSQL для production
- Docker containerization (планується)
- CI/CD pipeline (планується)
- Документація API (планується)

---

## Статистика проєкту

### Backend
- **Контролери:** 9
- **Сервіси:** 8
- **Моделі:** 14
- **Тести:** 36 (20 Unit + 16 Integration)
- **Покриття:** 100% критичної бізнес-логіки

### Frontend
- **Сторінки:** 14
- **Компоненти:** ~30
- **Тести:** 20 (11 Context + 9 Component)
- **Покриття:** AuthContext + MedicinesPage

### Mobile
- **Екрани:** 8
- **ViewModels:** 6
- **Тести:** 0 (планується 8)

### IoT
- **Пристрої:** 1 (ESP32 + DHT22)
- **Сенсори:** 2 (температура + вологість)
- **Тести:** 0 (планується 5)

### Загальна статистика
- **Загальна кількість тестів:** 56/69 (81%)
- **Рядків коду (Backend):** ~8000
- **Рядків коду (Frontend):** ~6000
- **Рядків коду (Mobile):** ~3000
- **Рядків коду (IoT):** ~500
- **Загалом:** ~17500 рядків коду

---

## Фази розробки

### ✅ Фаза 1: Проєктування (завершено)
- Архітектура системи
- Вибір технологій
- Дизайн БД

### ✅ Фаза 2: Backend розробка (завершено)
- 9 контролерів
- 8 сервісів
- 14 моделей
- Multi-tenancy + RBAC

### ✅ Фаза 3: Frontend розробка (завершено)
- 14 сторінок
- AuthContext
- React Query
- shadcn/ui

### ✅ Фаза 4: IoT розробка (завершено)
- ESP32 + DHT22
- HTTP Client
- Автоматична відправка даних

### ✅ Фаза 5: Mobile розробка (завершено)
- Повнофункціональний клієнт (копія веб-додатка)
- Повна підтримка життєвого циклу препаратів та IoT
- Модернізація архітектури (MVVM, Retrofit) завершена

### 🔄 Фаза 6: Тестування (81% завершено)
- ✅ Backend тести: 36/36
- ✅ Frontend тести: 20/20
- ⏳ Mobile тести: 0/8
- ⏳ IoT тести: 0/5

### ⏳ Фаза 7: DevOps та документація (планується)
- Docker containerization
- CI/CD pipeline
- API документація
- Deployment

---

## Висновки

Дипломний проєкт значно розширює функціональність курсового проєкту:

1. **Multi-tenancy архітектура** — підтримка кількох організацій
2. **RBAC** — різні права доступу для різних ролей
3. **Lifecycle Events** — повна історія операцій з препаратами
4. **IoT інтеграція** — автоматичний моніторинг умов зберігання
5. **Автоматичні інциденти** — створення та закриття інцидентів
6. **Система сповіщень** — сповіщення про важливі події
7. **Audit Log** — повний журнал аудиту дій
8. **Автоматизоване тестування** — 56 тестів для гарантії якості
9. **Сучасні технології** — .NET 8, React 19, TypeScript 6
10. **Production-ready** — готовність до розгортання в production

Проєкт демонструє розуміння:
- Розподілених систем
- Multi-tenancy архітектури
- RBAC механізмів
- IoT інтеграції
- Автоматизованого тестування
- Clean Architecture principles
- Modern web development practices

**Поточний статус:** 81% завершено, готовий до захисту після завершення Mobile та IoT тестів.

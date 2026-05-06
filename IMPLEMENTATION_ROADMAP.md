# IMPLEMENTATION_ROADMAP.md
> Покроковий план реалізації дипломного проєкту

## Поточний статус: Фаза 5 ЗАВЕРШЕНА ✅
**Останнє оновлення:** 2026-05-05

---

## 🟢 Фаза 1: Безпека та критичні виправлення (ВИКОНАНО)
- [x] Виправлення `GenerateJwtToken` (async/await, термін з config).
- [x] Перенесення JWT-ключа у User Secrets.
- [x] Захист `StorageConditionController` атрибутом `[Authorize]`.
- [x] Виправлення IoT-коду (читання реальних даних з DHT22).

## 🟢 Фаза 2: Розширення предметної моделі (ВИКОНАНО)
- [x] Створення нових сутностей: `StorageLocation`, `StorageIncident`, `MedicineLifecycleEvent`, `Notification`.
- [x] Розширення існуючих моделей `Medicine` та `AuditLog`.
- [x] Впровадження сервісів та контролерів для нових сутностей.
- [x] Застосування міграцій бази даних.

## 🟢 Фаза 3: Рефакторинг Background Services (ВИКОНАНО)
- [x] `StorageConditionMonitoringService`: debounce, створення інцидентів, сповіщень.
- [x] `ExpiryNotificationService`: дедуплікація, сповіщення в БД.
- [x] Параметризація через `appsettings.json`.

## 🟢 Фаза 3.5: Технічний борг (ВИКОНАНО)
- [x] Виправлення 13 критичних зауважень (AsNoTracking, ILogger, XML doc тощо).
- [x] Очищення коду від захардкоджених секретів.

## 🟢 Фаза 4: SPA Frontend (ВИКОНАНО)
- [x] Ініціалізація проєкту (Vite 6, React, TypeScript, Tailwind, shadcn/ui).
- [x] Налаштування CORS (гнучкий localhost).
- [x] Реалізація Auth (Login, Register, User Context).
- [x] Розробка 9 основних сторінок (Dashboard, CRUDs, Logs, Alerts).
- [x] Виправлення багів інтеграції (PascalCase/camelCase, JSON Patch).

## 🟢 Фаза 4.5: Архітектура Multi-Tenancy (ВИКОНАНО)
- [x] Створення `ApplicationUser` з властивістю `OrganizationId` (string/Guid).
- [x] Додавання `OrganizationId` до всіх доменних моделей бази даних.
- [x] Автоматична генерація ізольованого `OrganizationId` для нових користувачів.
- [x] Оновлення Identity та EF Core міграцій.

## 🟢 Фаза 5: Мобільний застосунок (ВИКОНАНО)
- [x] Аудит поточного коду Android.
- [x] Перехід на Retrofit + Coroutines + BottomNavigation.
- [x] Реалізація екранів: Аптечка, Датчики, Сповіщення.
- [x] Впровадження 6-значних OTP кодів підтвердження (замість Deep Links).
- [x] Реалізація екрану налаштувань (Тема, Мова).
- [x] Очищення від застарілого Java-коду та XML.

## ⚪ Фаза 6: Тестування
- [ ] Написання Unit-тестів для бізнес-логіки (xUnit).
- [ ] Integration-тести для API контролерів.
- [ ] Оновлення навантажувальних тестів (NBomber).

## ⚪ Фаза 7: DevOps та фіналізація
- [ ] Створення `docker-compose.yml`.
- [ ] Написання `README.md`.
- [ ] Підготовка ER-діаграм та архітектурної документації (C4).

---
*План є живим документом і оновлюється після кожної фази.*

# Аудит мобільного застосунку — відповідність веб-фронтенду

> **Дата останнього оновлення:** 2026-06-29  
> **Версія аналізу:** 2.0 (Оновлена після впровадження покращень)  
> **Мета:** Повна порівняльна перевірка функціоналу мобільного Android-застосунку відносно веб-фронтенду, який взаємодіє з тим самим бекендом ASP.NET Core.

---

## Методологія

Аудит проведено шляхом порівняння:
1. **Бекенд API** — контролери `AuthController`, `MedicineController`, `IoTDeviceController`, `StorageLocationController`, `StorageIncidentController`, `NotificationController`, `AuditLogController`, `MedicineLifecycleController`, `StorageConditionController`.
2. **Мобільний застосунок** — Retrofit-інтерфейси, активності, фрагменти, ViewModel'и.

Категорії оцінки кожної функції:
- ✅ **Реалізовано** — функція є і коректно інтегрована з API.
- ⚠️ **Частково реалізовано** — функція є, але неповна, або є відомі обмеження.
- ❌ **Відсутнє** — функція є на фронтенді, але відсутня в мобільному.

---

## 1. Автентифікація та авторизація

| Функція | Бекенд API | Мобільний | Статус | Коментар |
|---|---|---|---|---|
| Вхід (Login) | `POST /api/auth/login` | `AuthApi.login()` | ✅ | Повністю реалізовано |
| Реєстрація (Register) | `POST /api/auth/register` | `AuthApi.register()` | ✅ | Реалізовано |
| Підтвердження email | `POST /api/auth/confirm-email` | `ConfirmEmailActivity.kt` | ✅ | Є підтвердження 6-значним кодом |
| Повторна відправка коду | `POST /api/auth/resend-confirmation` | `AuthApi.resendConfirmation()` | ✅ | Реалізовано |
| Вихід (Logout) | — (JWT — stateless, очищення токена) | `SettingsFragment.kt` | ✅ | Токен очищується локально |
| Управління ролями | `POST /api/auth/assign-role` | `UsersFragment.kt` | ✅ | Є зміна ролі (Admin only) |
| Список користувачів | `GET /api/auth/users` | `UsersFragment.kt` | ✅ | Доступно лише Administrator |
| Видалення користувача | `DELETE /api/auth/users/{id}` | `UsersFragment.kt` | ✅ | Реалізовано |
| Створення менеджера | `POST /api/auth/create-manager` | `UsersFragment.showCreateManagerDialog()` | ✅ | Реалізовано |
| Відновлення пароля (Forgot) | `POST /api/auth/forgot-password` | `ForgotPasswordActivity.kt` | ✅ | **Усунено Gap:** Додано форму відновлення |
| Скидання пароля (Reset) | `POST /api/auth/reset-password` | `ResetPasswordActivity.kt` | ✅ | **Усунено Gap:** Реалізовано форму скидання за кодом |
| Отримати профіль (`/me`) | `GET /api/auth/me` | `ProfileFragment.kt` | ✅ | **Усунено Gap:** Сторінка профілю користувача повністю функціонує |
| Рольова видимість кнопок | — | `RoleHelper.kt` | ✅ | Кнопки та меню ховаються/відображаються згідно з роллю |

---

## 2. Препарати (Medicines)

| Функція | Бекенд API | Мобільний | Статус | Коментар |
|---|---|---|---|---|
| Список усіх препаратів | `GET /api/medicine` | `MedicinesFragment.kt` + `MedicineApi.getMedicines()` | ✅ | З пошуком |
| Деталі препарату | `GET /api/medicine/{id}` | `MedicineDetailsActivity.kt` | ✅ | Повні деталі із локацією та температурним діапазоном |
| Створення препарату | `POST /api/medicine` | `AddMedicineActivity.kt` | ✅ | З випадаючим списком локацій |
| Редагування препарату | `PATCH /api/medicine/{id}` | `EditMedicineActivity.kt` | ✅ | JSON Patch, випадаючий список локацій |
| Видалення препарату | `DELETE /api/medicine/{id}` | `MedicineDetailsActivity.kt` | ✅ | З підтвердженням |
| Надходження (Receive) | `POST /api/medicine/{id}/receive` | `MedicineDetailsActivity.kt` | ✅ | Діалог з кількістю |
| Видача (Issue) | `POST /api/medicine/{id}/issue` | `MedicineDetailsActivity.kt` | ✅ | Діалог з кількістю |
| Утилізація (Dispose) | `POST /api/medicine/{id}/dispose` | `MedicineDetailsActivity.kt` | ✅ | 0 = все |
| Переміщення (Move) | `POST /api/medicine/{id}/move` | `MedicineDetailsActivity.kt` | ✅ | Діалог вибору локації |
| Препарати з малим запасом | `GET /api/medicine/low-stock` | `MedicinesFragment.kt` | ✅ | **Усунено Gap:** Реалізовано chip-фільтр "Малий запас" |
| Препарати що закінчуються | `GET /api/medicine/expiring` | `MedicinesFragment.kt` | ✅ | **Усунено Gap:** Реалізовано chip-фільтр "Закінчується" |
| Рекомендації поповнення | `GET /api/medicine/replenishment-recommendations` | ❌ Відсутнє | ❌ | **Gap:** Відсутній розділ рекомендацій |
| Пошук препаратів | — (на клієнті) | `MedicinesFragment.kt` | ✅ | Пошук за назвою та категорією |

---

## 3. Журнал подій (Lifecycle)

| Функція | Бекенд API | Мобільний | Статус | Коментар |
|---|---|---|---|---|
| Переглянути події для препарату | `GET /api/medicinelifecycle/medicine/{id}` | `MedicineDetailsActivity.kt` (RecyclerView) | ✅ | Відображається як "щоденник" препарату, відсортовано за датою |
| Всі lifecycle-події | `GET /api/medicinelifecycle` | ❌ Відсутнє | ❌ | **Gap:** Немає окремого глобального списку |
| Подія за ID | `GET /api/medicinelifecycle/{id}` | ❌ Відсутнє | ⚠️ | Не критично, лише деталі окремої події |
| Додати подію вручну | `POST /api/medicinelifecycle` | ❌ Відсутнє | ❌ | **Gap:** Не реалізовано ручне створення lifecycle-події |

---

## 4. IoT-пристрої (Sensors)

| Функція | Бекенд API | Мобільний | Статус | Коментар |
|---|---|---|---|---|
| Список пристроїв | `GET /api/iotdevice` | `SensorsFragment.kt` | ✅ | Реалізовано |
| Деталі пристрою | `GET /api/iotdevice/{id}` | `DeviceDetailsActivity.kt` | ✅ | Повні деталі + графік показань |
| Додати пристрій | `POST /api/iotdevice` | `AddDeviceActivity.kt` | ✅ | Реалізовано |
| Редагувати пристрій | `PATCH /api/iotdevice/{id}` | `EditDeviceActivity.kt` | ✅ | Реалізовано |
| Видалити пристрій | `DELETE /api/iotdevice/{id}` | `DeviceDetailsActivity.kt` | ✅ | Реалізовано |
| Активувати/деактивувати | `PATCH /api/iotdevice/setstatus/{id}` | `SensorsFragment.kt` + `DeviceAdapter.kt` | ✅ | Toggle-кнопка в картці |
| Показання умов зберігання | `GET /api/iotdevice/conditions/{deviceId}` | `DeviceDetailsActivity.kt` | ✅ | Відображається графік |
| Claim пристрою | `POST /api/iotdevice/claim` | ❌ Відсутнє | ❌ | **Gap:** Процес реєстрації нового фізичного пристрою відсутній |

---

## 5. Умови зберігання (Storage Conditions)

| Функція | Бекенд API | Мобільний | Статус | Коментар |
|---|---|---|---|---|
| Список усіх умов | `GET /api/storagecondition` | ❌ Відсутнє | ❌ | **Gap:** Немає окремого розділу всіх умов зберігання |
| Умови для пристрою | `GET /api/iotdevice/conditions/{deviceId}` | `DeviceDetailsActivity.kt` | ✅ | Є графік для конкретного пристрою |
| Глобальний список показань | `GET /api/storagecondition` | ❌ Відсутнє | ❌ | **Gap:** Окремий розділ "Показання" відсутній |
| Перевірка умов | `GET /api/storagecondition/checkCondition` | ❌ Відсутнє | ❌ | Не критично — фонова функція бекенду |

---

## 6. Локації зберігання (Storage Locations)

| Функція | Бекенд API | Мобільний | Статус | Коментар |
|---|---|---|---|---|
| Список усіх локацій | `GET /api/storagelocation` | `StorageLocationsFragment.kt` | ✅ | Реалізовано |
| Деталі локації | `GET /api/storagelocation/{id}` | `StorageLocationsFragment.kt` (in-line) | ⚠️ | Немає окремого екрану деталей |
| Створити локацію | `POST /api/storagelocation` | `StorageLocationsFragment.kt` | ✅ | Діалог зі всіма полями та вибором пристрою |
| Редагувати локацію | `PUT /api/storagelocation/{id}` | `StorageLocationsFragment.kt` | ✅ | Діалог редагування |
| Видалити локацію | `DELETE /api/storagelocation/{id}` | `StorageLocationsFragment.kt` | ✅ | З підтвердженням |

---

## 7. Інциденти (Storage Incidents)

| Функція | Бекенд API | Мобільний | Статус | Коментар |
|---|---|---|---|---|
| Список усіх інцидентів | `GET /api/storageincident` | `IncidentsFragment.kt` | ✅ | Реалізовано |
| Лише активні / вирішені | `GET /api/storageincident/active` | `IncidentsFragment.kt` | ✅ | **Усунено Gap:** Додано chip-фільтри (Всі / Активні / Вирішені) |
| Інцидент за ID | `GET /api/storageincident/{id}` | ❌ Відсутнє | ⚠️ | Не критично, деталей окремої картки немає |
| Вирішити інцидент | `PATCH /api/storageincident/{id}/resolve` | `IncidentsFragment.showResolveDialog()` | ✅ | Є діалог із коментарем (заходами) |
| Створити інцидент | `POST /api/storageincident` | ❌ Відсутнє | ⚠️ | Не критично — системна функція |
| Видалити інцидент | `DELETE /api/storageincident/{id}` | `IncidentsFragment.kt` | ✅ | **Усунено Gap:** Видалення інциденту по довгому кліку |

---

## 8. Сповіщення (Notifications)

| Функція | Бекенд API | Мобільний | Статус | Коментар |
|---|---|---|---|---|
| Всі сповіщення | `GET /api/notification` | `NotificationsFragment.kt` | ✅ | Реалізовано |
| Непрочитані | `GET /api/notification/unread` | ❌ (не використовується) | ⚠️ | Завантажуються всі, фільтрація клієнтська |
| Фільтрація (Все/Термін/Інцидент) | — | `NotificationsFragment.kt` (Chips) | ✅ | Локальна фільтрація |
| Прочитати одне | `PATCH /api/notification/{id}/read` | `NotificationsFragment.kt` | ✅ | Є позначення при кліку |
| Прочитати всі | `PATCH /api/notification/read-all` | `NotificationsFragment.kt` | ✅ | Кнопка "Прочитати всі" |
| Бейдж (лічильник) | — | `MainActivity.updateNotificationBadge()` | ✅ | Бейдж на іконці навігації |
| Polling сповіщень | — | `NotificationsViewModel.startPolling()` | ✅ | Є автоматичне оновлення |

---

## 9. Журнал аудиту (Audit Log)

| Функція | Бекенд API | Мобільний | Статус | Коментар |
|---|---|---|---|---|
| Список логів | `GET /api/auditlog` | `AuditLogFragment.kt` | ✅ | Реалізовано (Admin only) |
| Фільтрація за типом | `?action=...` | `AuditLogFragment.kt` (Chips) | ✅ | Є chip-фільтри (Medicine, Storage, Device, User, Incident) |
| Фільтрація за датою | `?from=...&to=...` | ❌ Відсутнє | ❌ | **Gap:** Фільтр по діапазону дат відсутній |
| Фільтрація за користувачем | `?user=...` | ❌ Відсутнє | ❌ | **Gap:** Пошук по конкретному користувачу відсутній |
| Деталі логу за ID | `GET /api/auditlog/{id}` | ❌ Відсутнє | ⚠️ | Не критично |

---

## 10. Профіль та налаштування

| Функція | Бекенд API | Мобільний | Статус | Коментар |
|---|---|---|---|---|
| Вибір теми (темна/світла/системна) | — | `SettingsFragment.kt` | ✅ | Є перемикач теми |
| Вибір мови (UK/EN) | — | `SettingsFragment.kt` | ✅ | Є перемикач мови (uk/en) |
| Профіль поточного користувача | `GET /api/auth/me` | `ProfileFragment.kt` | ✅ | **Усунено Gap:** Додано сторінку профілю користувача |
| Зміна пароля | — | ❌ Відсутнє | ❌ | **Gap:** Немає функції зміни пароля |

---

## Навігація та адаптивність інтерфейсу

*   **Навігація:** За запитом користувача повністю повернуто та адаптовано бічне меню навігації (**Navigation Drawer** / `DrawerLayout`), яке є звичним та ергономічним.
*   **Адаптивність:** Оновлено розмітку списків та карток для забезпечення коректної поведінки при зміні орієнтації екрана (підтримка альбомного режиму).

---

## Зведена таблиця залишку прогалин (Gaps)

| # | Пріоритет | Модуль | Відсутня функція | Складність реалізації |
|---|---|---|---|---|
| 1 | 🟡 Середній | Аудит-лог | Фільтр за датою та за користувачем | Середня |
| 2 | 🟡 Середній | Умови зберігання | Загальний список показань всіх датчиків | Середня |
| 3 | 🟡 Середній | Lifecycle | Ручне додавання lifecycle-події | Середня |
| 4 | 🟡 Середній | Препарати | Рекомендації поповнення | Низька |
| 5 | 🔵 Опціональний | IoT | Claim пристрою (фізична реєстрація) | Висока |
| 6 | 🔵 Опціональний | Профіль | Зміна пароля в застосунку | Середня |

---

## Що вже відмінно реалізовано ✅

- **Навігаційне бічне меню** (`DrawerLayout`) з рольовою видимістю (Адмін/Менеджер/Юзер).
- **Повний CRUD для препаратів** — відображення, створення, редагування, видалення.
- **Швидкі дії** — Надходження, Видача, Утилізація, Переміщення.
- **Журнал lifecycle** в контексті кожного препарату.
- **Повний CRUD для IoT-пристроїв** — список, деталі, додавання, редагування, видалення, toggle статусу.
- **Графік показань** для конкретного пристрою.
- **Локації зберігання** — повний CRUD.
- **Сповіщення** — перегляд, фільтрація, позначення прочитаними, polling.
- **Журнал аудиту** — перегляд та фільтрація за типом.
- **Інциденти** — список, вирішення через діалог та видалення по довгому кліку.
- **Управління користувачами** — список, видалення, зміна ролі, створення менеджера.
- **Багатомовність** — укр/англ.
- **Темна/світла/системна тема**.
- **Бейдж непрочитаних** сповіщень.
- **Dropdown для локацій** замість ручного введення ID.

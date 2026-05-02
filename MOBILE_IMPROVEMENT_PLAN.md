# Оптимізований план покращення мобільного застосунку (Фаза 5)

**Дата:** 2026-05-03  
**Область:** Mobile/MedicationManagement (Android)  
**Ціль:** сучасний і зручний застосунок саме для домашнього користувача, без «переписати все»

---

## 1. Принципи (щоб не “роздути” обсяг)

- Не робити повну міграцію на Jetpack Compose як обов’язкову ціль: у коді вже є XML-екрани, їх можна осучаснити без повного переписування.
- Не впроваджувати Hilt/Room/MVVM “заради галочки”: додаємо лише те, що зменшує баги і прискорює розробку.
- Фокус на домашньому сценарії: «мій облік препаратів» (CRUD) + базовий стан/термін придатності. IoT/умови зберігання — опційно.
- Видалити/сховати модулі, які не потрібні домашньому користувачу (IoT devices management, audit log).

---

## 2. Короткий аудит поточного мобільного коду (фактичний стан)

### 2.1 Навігація і структура

- Є дублювання “екранів” для препаратів: [DashboardActivity](file:///d:/Learning/Diploma/Mobile/MedicationManagement/app/src/main/java/com/example/medicationmanagement/DashboardActivity.kt) і [DashboardFragment](file:///d:/Learning/Diploma/Mobile/MedicationManagement/app/src/main/java/com/example/medicationmanagement/DashboardFragment.kt).
- Після CRUD-дій екран часто повертає на `DashboardActivity`, хоча основна навігація побудована через `MainActivity` + фрагменти ([MainActivity](file:///d:/Learning/Diploma/Mobile/MedicationManagement/app/src/main/java/com/example/medicationmanagement/MainActivity.kt)).
- BottomNavigation зараз включає “Devices / Conditions / Logs”, що концептуально не відповідає домашньому застосунку ([bottom_nav_menu.xml](file:///d:/Learning/Diploma/Mobile/MedicationManagement/app/src/main/res/menu/bottom_nav_menu.xml)).

### 2.2 Токен і налаштування

- Використовуються різні `SharedPreferences` назви: `"MyPrefs"` у логіні/фрагментах та `"app_prefs"` у CRUD-екранах, через що частина запитів може виконуватись без токена (приклад: [LoginActivity](file:///d:/Learning/Diploma/Mobile/MedicationManagement/app/src/main/java/com/example/medicationmanagement/LoginActivity.kt#L71-L78) vs [AddMedicineActivity](file:///d:/Learning/Diploma/Mobile/MedicationManagement/app/src/main/java/com/example/medicationmanagement/AddMedicineActivity.kt#L42-L44)).
- Нема нормального logout, токен не очищається як сценарій.

### 2.3 Мережа

- У кожному екрані вручну повторюються: `HttpURLConnection`, заголовки, base URL, окремі `Thread`, парсинг через `org.json.*` (наприклад [DashboardFragment](file:///d:/Learning/Diploma/Mobile/MedicationManagement/app/src/main/java/com/example/medicationmanagement/DashboardFragment.kt#L70-L107)).
- Base URL захардкоджений у багатьох місцях: `http://10.0.2.2:5000/...`.
- PATCH для `Medicine` відправляється як `Content-Type: application/json`, хоча це JSON Patch масив (ризик несумісності) ([EditMedicineActivity](file:///d:/Learning/Diploma/Mobile/MedicationManagement/app/src/main/java/com/example/medicationmanagement/EditMedicineActivity.kt#L60-L90)).

### 2.4 Моделі і сумісність з backend

- `IoTDevice.deviceID` та `StorageCondition.deviceID` у мобільному як `Int`, але в backend `DeviceId` — `string`. Це означає, що поточний екран “Devices” і частина “Conditions” уже не відповідають API і потребують переробки або вилучення ([IoTDevice.kt](file:///d:/Learning/Diploma/Mobile/MedicationManagement/app/src/main/java/com/example/medicationmanagement/model/IoTDevice.kt), [StorageCondition.kt](file:///d:/Learning/Diploma/Mobile/MedicationManagement/app/src/main/java/com/example/medicationmanagement/model/StorageCondition.kt)).
- Для домашнього застосунку це ще один аргумент прибрати “керування IoT” з mobile.

### 2.5 UI

- Тема застосунку базується на `Theme.AppCompat.Light.NoActionBar` ([themes.xml](file:///d:/Learning/Diploma/Mobile/MedicationManagement/app/src/main/res/values/themes.xml)), через що UI виглядає “старим” і не підтримує Day/Night з коробки.
- Compose-плагін увімкнено, але Compose фактично не використовується (є лише шаблонна тема) ([Theme.kt](file:///d:/Learning/Diploma/Mobile/MedicationManagement/app/src/main/java/com/example/medicationmanagement/ui/theme/Theme.kt)).

---

## 3. Цільовий функціонал мобільного застосунку (домашній користувач)

### 3.1 MUST-HAVE (обов’язково)

- Автентифікація: `LoginActivity`, `RegisterActivity`, автологін при наявності токена, logout з очищенням токена.
- Препарати (мій домашній облік):
  - список з пошуком;
  - деталі;
  - додавання / редагування / видалення;
  - валідація полів (кількість, дата придатності).
- Сучасний вигляд: оновлена тема, нормальні стани loading/empty/error, узгоджені тексти UA/EN.

### 3.2 SHOULD-HAVE (якщо не “ламає” план)

- Фільтри по стану (наприклад: “протерміновані”, “закінчується скоро”) — стан можна рахувати на клієнті по `expiryDate`, навіть без `Medicine.Status` у DTO.
- Якісна обробка помилок: показати повідомлення, дати повторити запит, коректно реагувати на 401 (повертати на логін).

### 3.3 OPTIONAL (тільки якщо залишиться час)

- Перегляд умов зберігання (read-only) у спрощеному вигляді, без керування пристроями.
- Список сповіщень (read-only) через `GET /api/notification` або `GET /api/notification/unread`, з “позначити як прочитане”.

---

## 4. Узгодження з backend (важливо для “домашнього” CRUD)

Поточний mobile вже робить `POST/PATCH/DELETE` по `/api/medicine`, але це має сенс лише якщо backend дозволяє домашньому користувачу виконувати ці дії у межах своєї організації.

### 4.1 Рекомендоване дипломне трактування

- Домашній користувач = звичайний користувач з власною `OrganizationId`.
- Дозволити ролі `User` виконувати CRUD над `Medicine` у межах своєї організації (або ввести окрему роль, але без ускладнення).

### 4.2 Якщо backend не можна/не встигаємо змінити

- Альтернатива: зробити мобільний облік “local-only” (без синхронізації), але це гірше підкреслює розподіленість дипломної системи.

---

## 5. План покращень (мінімальний, без “переписування всього”)

### 5.1 Етап A — Стабілізація (обов’язково)

- Один `SharedPreferences` неймспейс для токена і налаштувань; єдиний `TokenStore` (отримати/зберегти/очистити).
- Один `ApiConfig` з base URL; прибрати дублювання `http://10.0.2.2:5000` по екранах.
- Один мінімальний `HttpClient`/`ApiClient`-хелпер для `GET/POST/PATCH/DELETE`:
  - додає `Authorization`;
  - читає `errorStream` і показує адекватну помилку;
  - централізовано обробляє 401 → logout → перехід на `LoginActivity`.
- Вирівняти навігацію: CRUD-екрани після успіху мають повертатись у `MainActivity` (а не `DashboardActivity`), або використовувати `finish()` без “стрибків” між паралельними flow.
- Мінімальна стабільність стану: не втрачати список/пошук після повороту або повернення з деталей (через збереження стану або простий `ViewModel` лише для списку).

### 5.2 Етап B — Оновлення UI (обов’язково)

- Перевести тему на `Theme.MaterialComponents.DayNight.NoActionBar` (або еквівалент наявний у `com.google.android.material:material`) і привести основні екрани до Material-компонентів.
- Уніфікувати стилі списків (карточки/відступи), кнопки, поля вводу, помилки валідації.
- Додати явні стани:
  - loading (progress);
  - empty (нема препаратів);
  - error (мережа/сервер) з кнопкою “Повторити”.

### 5.3 Етап C — Функціональні дрібні покращення (обов’язково/бажано)

- Введення дати придатності через DatePicker замість ручного вводу.
- Фільтри по терміну придатності (протерміновані / закінчується скоро) і підсвітка в списку.
- Валідація кількості (не дозволяти від’ємні значення, не падати на `toInt()`).

---

## 6. Свідомо вилучаємо з плану (щоб не зірвати диплом)

- Повна міграція UI на Jetpack Compose.
- Hilt як “обов’язкова” складова.
- Room/offline-first як must-have.
- Графіки, WebSocket, push-сповіщення, експорт PDF.
- Модулі “керування IoT devices” і “audit log” у mobile для домашнього користувача.

---

## 7. Критерії готовності (Definition of Done)

- Користувач може зареєструватись/увійти, токен зберігається стабільно, logout його очищає.
- Користувач може додати/редагувати/видалити `Medicine` і бачить зміни в списку без “битої” навігації.
- Усі API-запити використовують єдину конфігурацію base URL і єдине місце для додавання токена/обробки помилок.
- UI має Day/Night тему, є loading/empty/error стани.

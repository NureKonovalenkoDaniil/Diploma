# План модернізації мобільного застосунку

## Фаза 6.5: Mobile App as Full Web App Copy

**Дата:** 2026-05-08  
**Версія:** 1.0  
**Ціль:** Перетворити мобільний застосунок на повноцінну копію веб-додатку з красивим та зручним інтерфейсом, збереженням всіх ролей та функцій.

**Поточний стан:** Фаза 7.1 завершена, базова інфраструктура API та стан-менеджменту закрита.

---

## 1. Огляд поточного стану Mobile App

### 1.1 Що є зараз

| Компонент             | Статус          | Проблеми                              |
| :-------------------- | :-------------- | :------------------------------------ |
| **Authentication**    | ✅ Частково     | OTP flow працює, але сам UI старий    |
| **API Integration**   | ✅ Є            | HttpURLConnection — складно розширяти |
| **Navigation**        | ❌ Застаріла    | Menu/DrawerLayout замість BottomNav   |
| **Data Models**       | ✅ Є            | Старі моделі, відсутні нові поля      |
| **UI Design**         | ❌ Очень погана | XML layouts без Material Components   |
| **Role-Based Access** | ❌ Немає        | Всі екрани для всіх користувачів      |
| **Дизайн система**    | ❌ Немає        | Без Material 3 / Compose              |
| **State Management**  | ❌ Немає        | Прямі HTTP-запити без ViewModel       |

### 1.2 Поточні Activities/Screens

```
Activities:
  ├─ LoginActivity              (Login, OTP confirm)
  ├─ RegisterActivity           (Register + Email confirm)
  ├─ MainActivity               (Main navigation)
  ├─ DashboardActivity          (Не використовується)
  ├─ MedicineDetailsActivity    (Деталі препарату)
  ├─ AddMedicineActivity        (Додавання)
  ├─ EditMedicineActivity       (Редагування)
  ├─ DeviceDetailsActivity      (Деталі пристрою)
  ├─ AddDeviceActivity          (Додавання пристрою)
  └─ EditDeviceActivity         (Редагування пристрою)

Fragments (потрібні):
  ├─ FragmentMedicines          (Список препаратів)
  ├─ FragmentStorageLocations   (Датчики/Локації)
  ├─ FragmentIncidents          (Інциденти)
  ├─ FragmentNotifications      (Сповіщення)
  ├─ FragmentAuditLog           (Журнал, лише Admin)
  ├─ FragmentUsers              (Користувачи, лише Admin)
  └─ FragmentSettings           (Налаштування)
```

---

## 2. Архітектурні зміни (Обов'язкові)

### 2.1 Міграція на Retrofit (замість HttpURLConnection)

**Причина:** Поточний HTTP-клієнт мотає, немає перехоплювачів,難管理.

**План:**

1. Додати `build.gradle.kts`:

   ```kotlin
   implementation("com.squareup.retrofit2:retrofit:2.9.0")
   implementation("com.squareup.retrofit2:converter-gson:2.9.0")
   implementation("com.squareup.okhttp3:okhttp:4.11.0")
   implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
   ```

2. Створити `ApiService.kt` з інтерфейсами для всіх контролерів:

   ```kotlin
   interface AuthApi {
       @POST("api/auth/register")
       suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResponse>

       @POST("api/auth/login")
       suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

       @GET("api/auth/me")
       suspend fun getMe(): ApiResponse<UserDto>
   }

   interface MedicineApi {
       @GET("api/medicine")
       suspend fun getAll(): ApiResponse<List<MedicineDto>>

       @POST("api/medicine")
       suspend fun create(@Body dto: CreateMedicineDto): ApiResponse<MedicineDto>

       @PUT("api/medicine/{id}")
       suspend fun update(@Path("id") id: Int, @Body dto: MedicineDto): ApiResponse<MedicineDto>

       @DELETE("api/medicine/{id}")
       suspend fun delete(@Path("id") id: Int): ApiResponse<Unit>

       @POST("api/medicine/{id}/move")
       suspend fun move(@Path("id") id: Int, @Body request: MoveRequest): ApiResponse<MedicineDto>

       @POST("api/medicine/{id}/receive")
       suspend fun receive(@Path("id") id: Int, @Body request: QuantityRequest): ApiResponse<MedicineDto>

       @POST("api/medicine/{id}/issue")
       suspend fun issue(@Path("id") id: Int, @Body request: QuantityRequest): ApiResponse<MedicineDto>

       @POST("api/medicine/{id}/dispose")
       suspend fun dispose(@Path("id") id: Int, @Body request: QuantityRequest): ApiResponse<MedicineDto>
   }

   // Аналогічно для інших контролерів:
   interface StorageLocationApi { ... }
   interface NotificationApi { ... }
   interface AuditLogApi { ... }
   interface UserApi { ... }
   interface IoTDeviceApi { ... }
   interface StorageIncidentApi { ... }
   interface MedicineLifecycleApi { ... }
   ```

3. Створити `RetrofitClient.kt`:

   ```kotlin
   object RetrofitClient {
       private const val BASE_URL = "http://10.0.2.2:5000/"  // Для emulator

       private val client = OkHttpClient.Builder()
           .addInterceptor(TokenInterceptor())
           .addInterceptor(HttpLoggingInterceptor().apply {
               level = HttpLoggingInterceptor.Level.BODY
           })
           .build()

       fun getRetrofit(): Retrofit = Retrofit.Builder()
           .baseUrl(BASE_URL)
           .client(client)
           .addConverterFactory(GsonConverterFactory.create())
           .build()

       fun getAuthApi(): AuthApi = getRetrofit().create(AuthApi::class.java)
       fun getMedicineApi(): MedicineApi = getRetrofit().create(MedicineApi::class.java)
       // ... інші API
   }
   ```

4. Створити `TokenInterceptor.kt`:

   ```kotlin
   class TokenInterceptor(private val tokenManager: TokenManager) : Interceptor {
       override fun intercept(chain: Interceptor.Chain): Response {
           val originalRequest = chain.request()
           val token = tokenManager.getToken()

           val newRequest = if (token != null) {
               originalRequest.newBuilder()
                   .addHeader("Authorization", "Bearer $token")
                   .build()
           } else {
               originalRequest
           }

           return chain.proceed(newRequest)
       }
   }
   ```

### 2.2 Стан-менеджмент (ViewModel + StateFlow / LiveData)

**Причина:** Захист від втрати даних при повороті екрану, одиниця управління станом.

**План:**

1. Для кожного екрана створити свій `ViewModel`:

   ```kotlin
   class MedicinesViewModel(private val medicineApi: MedicineApi) : ViewModel() {
       private val _medicines = MutableStateFlow<List<MedicineDto>>(emptyList())
       val medicines: StateFlow<List<MedicineDto>> = _medicines.asStateFlow()

       private val _isLoading = MutableStateFlow(false)
       val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

       private val _error = MutableStateFlow<String?>(null)
       val error: StateFlow<String?> = _error.asStateFlow()

       fun loadMedicines() {
           viewModelScope.launch {
               _isLoading.value = true
               try {
                   _medicines.value = medicineApi.getAll().data
                   _error.value = null
               } catch (e: Exception) {
                   _error.value = e.message
               } finally {
                   _isLoading.value = false
               }
           }
       }
   }
   ```

2. Додати до `build.gradle.kts`:
   ```kotlin
   implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
   implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
   ```

### 2.3 Global Error Handling & Logout on 401

**План:**

1. Створити `HttpException` handler у `Interceptor`:

   ```kotlin
   class ErrorInterceptor(private val context: Context) : Interceptor {
       override fun intercept(chain: Interceptor.Chain): Response {
           val response = chain.proceed(chain.request())

           if (response.code == 401) {
               // Очистити токен і редиректити на Login
               TokenManager(context).clearToken()
               val intent = Intent(context, LoginActivity::class.java)
               intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
               context.startActivity(intent)
           }

           return response
       }
   }
   ```

---

## 3. UI/UX Модернізація

### 3.1 Дизайн Система (Material 3)

**Залежності:**

```kotlin
implementation("androidx.compose.material3:material3:1.1.1")
implementation("androidx.compose.ui:ui:1.5.4")
implementation("androidx.activity:activity-compose:1.7.2")
implementation("com.google.android.material:material:1.10.0")
```

**Кольорова схема (темна/світла):**

```kotlin
// colors.xml (Light)
<color name="primary">#1976D2</color>
<color name="secondary">#03DAC6</color>
<color name="background">#FFFFFF</color>
<color name="surface">#F5F5F5</color>
<color name="error">#B3261E</color>

// colors.xml (Dark)
<color name="primary">#BB86FC</color>
<color name="secondary">#03DAC6</color>
<color name="background">#121212</color>
<color name="surface">#1E1E1E</color>
<color name="error">#F2B8B5</color>
```

### 3.2 Навігація (BottomNavigationView)

**План:**

1. Замінити `DrawerLayout` на `BottomNavigationView` з 5-6 вкладок:

   ```
   ┌─────────────────────────────────┐
   │  📊 Dashboard / Сповіщення      │
   ├─────────────────────────────────┤
   │  [Вміст вкладки]                │
   │                                 │
   │                                 │
   └─────────────────────────────────┘
   │ 💊 │ 🏠 │ ⚙ │ 📋 │ 👥 │ 🔔 │  (якщо Admin)
   ```

   **Вкладки:**
   - **💊 Препарати** — Список, додавання, редагування, деталі, Quick Actions (Вжити/Видати)
   - **🏠 Датчики** — Список датчиків, їх статус, додавання нових
   - **🔔 Сповіщення** — Всі сповіщення з бейджем, позначення як прочитані
   - **⚙ Налаштування** — Тема, мова, Logout
   - **📋 Інциденти** (для Manager+) — Список, деталі, видалення
   - **👥 Користувачи** (лише Admin) — Список, управління
   - **📊 Журнал** (лише Admin) — Audit log

2. XML:
   ```xml
   <!-- activity_main.xml -->
   <FrameLayout android:id="@+id/fragment_container" />
   <BottomNavigationView
       android:id="@+id/bottom_nav"
       android:layout_gravity="bottom"
       app:menu="@menu/bottom_nav_menu" />
   ```

### 3.3 Компоненти UI (Material Components)

**Видалити:**

- Старі `EditText` → використовувати `TextInputLayout` + `TextInputEditText`
- Звичайні `Button` → `Material Button`
- Простий `RecyclerView` → `RecyclerView` + `Material Card` з тінями
- Діалоги → `MaterialAlertDialog`

**Приклади:**

```xml
<!-- modern_text_input.xml -->
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox">
    <com.google.android.material.textfield.TextInputEditText
        android:hint="@string/medicine_name" />
</com.google.android.material.textfield.TextInputLayout>

<!-- modern_button.xml -->
<com.google.android.material.button.MaterialButton
    android:id="@+id/save_button"
    style="@style/Widget.MaterialComponents.Button"
    android:text="@string/save" />

<!-- modern_card.xml -->
<com.google.android.material.card.MaterialCardView
    app:strokeColor="@color/outline"
    app:strokeWidth="1dp">
    <LinearLayout>
        <!-- Вміст карти -->
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### 3.4 Стани Loading / Error / Empty

**Для кожного списку:**

```xml
<!-- fragment_medicines.xml -->
<FrameLayout>
    <!-- Loading spinner -->
    <ProgressBar
        android:id="@+id/loading"
        android:layout_gravity="center" />

    <!-- Error message -->
    <LinearLayout
        android:id="@+id/error_container"
        android:orientation="vertical"
        android:gravity="center">
        <TextView android:id="@+id/error_text" />
        <Button android:id="@+id/retry_button" />
    </LinearLayout>

    <!-- Empty state -->
    <LinearLayout
        android:id="@+id/empty_container"
        android:orientation="vertical"
        android:gravity="center">
        <ImageView android:src="@drawable/ic_empty" />
        <TextView android:text="@string/no_medicines" />
    </LinearLayout>

    <!-- Successful data -->
    <RecyclerView
        android:id="@+id/medicines_list" />
</FrameLayout>
```

---

## 4. Функціональність (по вкладкам)

### 4.1 💊 ВКЛАДКА: Препарати (MedicinesFragment)

**Функції:**
| # | Функція | Ролі | Деталі |
|:--|:--------|:-----|:-------|
| 1 | Список препаратів | All | Карти з мініатюрами, термін, кількість |
| 2 | Фільтрація / Пошук | All | По назві, категорії, статусу |
| 3 | Додавання | Admin/Mgr | Dialog з формою |
| 4 | Редагування | Admin/Mgr | Edit activity |
| 5 | Видалення | Admin/Mgr | Swipe-to-delete або меню |
| 6 | Деталі препарату | All | Де: термін, кількість, історія (Lifecycle) |
| 7 | **Quick Action: Вжити** | All | Кнопка в деталях, вводить кількість, записує подію |
| 8 | **Quick Action: Видати** | Admin/Mgr | Аналогічно "Вжити" |
| 9 | **Quick Action: Утилізувати** | Admin/Mgr | Аналогічно "Вжити" |
| 10 | **Move препарату** | Admin/Mgr | Вибір датчика/локації |

**Екран деталей препарату:**

```
┌────────────────────────────────┐
│ ← Назва препарату              │ (заголовок)
├────────────────────────────────┤
│ [Фотографія лікарства]         │
├────────────────────────────────┤
│ Термін: 2027-12-31             │
│ Кількість: 50 шт               │
│ Статус: ✅ Активно             │
│ Категорія: Рецептурна          │
│ Виробник: PharmaCorp           │
│ Серія: BATCH001                │
│ Локація: Холодильник           │
├────────────────────────────────┤
│ [🔄 Перемістити] [⬜ Видати]   │ (Quick Actions)
│ [✅ Вжити] [🗑️ Утилізувати]    │
├────────────────────────────────┤
│ Історія (Lifecycle Events):    │
│ • 2026-05-07: Надійшло (100 шт)│
│ • 2026-05-06: Вжито (3 шт)     │
│ • 2026-05-05: Перенесено → ...  │
└────────────────────────────────┘
```

### 4.2 🏠 ВКЛАДКА: Датчики (StorageLocationsFragment)

**Функції:**
| # | Функція | Ролі | Деталі |
|:--|:--------|:-----|:-------|
| 1 | Список датчиків | All | Карти із статусом (✅ OK / ⚠️ Warning / ❌ Error) |
| 2 | Деталі датчика | All | Останні показники T°, Humidity, часова шкала |
| 3 | Додавання датчика | Admin/Mgr | Введення DeviceId (штрих-код / ручне) |
| 4 | Видалення датчика | Admin/Mgr | Confirm dialog |
| 5 | Редагування назви | Admin/Mgr | Inline edit або діалог |

**Екран датчика (деталі):**

```
┌────────────────────────────────┐
│ ← Датчик: Холодильник          │
├────────────────────────────────┤
│ DeviceId: DEV-12345            │
│ Статус: ✅ Активно             │
│ Останнє оновлення: 2 хв тому   │
├────────────────────────────────┤
│ 🌡️  Температура: 4.2°C         │
│    Норма: 2-8°C               │
│                               │
│ 💧 Вологість: 65%             │
│    Норма: 30-70%              │
├────────────────────────────────┤
│ 📊 Графік за 24 години:        │
│ [   Малий графік T° та RH   ] │
│ (можна розгорнути на повний)   │
└────────────────────────────────┘
```

### 4.3 🔔 ВКЛАДКА: Сповіщення (NotificationsFragment)

**Функції:**
| # | Функція | Ролі | Деталі |
|:--|:--------|:-----|:-------|
| 1 | Список сповіщень | All | Хронологічний порядок (нові зверху) |
| 2 | Бейдж на вкладці | All | Кількість непрочитаних |
| 3 | Позначити як прочитане | All | Свайп або меню |
| 4 | Позначити усі як прочитані | All | Кнопка наверху |
| 5 | Типи сповіщень | All | Expiry warning, Storage condition incident |
| 6 | Фільтрація | All | По типу (All / Expiry / Incident) |

**Екран сповіщень:**

```
┌────────────────────────────────┐
│ 🔔 Сповіщення        [👁️: 5]   │
│ [🔽 Фільтр] [✓ Усі читані]     │
├────────────────────────────────┤
│ 🆕 [Паразетамол] Термін через  │  (Свайп для позначення)
│     7 днів: 2026-05-14         │
├────────────────────────────────┤
│ 🆕 [Холодильник] Порушення     │
│     Температура: 15°C (норма  │
│     2-8°C)                     │
├────────────────────────────────┤
│    [Аспірин] Видано 1 шт      │  (Прочитане, затьмарене)
│    2 дні тому                   │
└────────────────────────────────┘
```

### 4.4 ⚙️ ВКЛАДКА: Налаштування (SettingsFragment)

**Функції:**
| # | Функція | Ролі | Деталі |
|:--|:--------|:-----|:-------|
| 1 | Тема (Light/Dark/System) | All | Preference |
| 2 | Мова (UK/EN/System) | All | Preference (потребує перезавантаження app) |
| 3 | Профіль користувача | All | Email, Name, Role (read-only) |
| 4 | Зміна пароля | All | Форма |
| 5 | Logout | All | Confirm dialog, очищення токена |

### 4.5 📋 ВКЛАДКА: Інциденти (IncidentsFragment) — Видима для Manager+

**Функції:**
| # | Функція | Ролі | Деталі |
|:--|:--------|:-----|:-------|
| 1 | Список інцидентів | Mgr/Admin | Карти із статусом (Active / Resolved) |
| 2 | Деталі інциденту | Mgr/Admin | Датчик, тип, показники, опис |
| 3 | Видалення інциденту | Admin | Swipe-to-delete |
| 4 | Розв'язання (Resolve) | Mgr/Admin | Кнопка, Confirm, запис коментаря |

### 4.6 👥 ВКЛАДКА: Користувачи (UsersFragment) — Лише для Admin

**Функції:**
| # | Функція | Ролі | Деталі |
|:--|:--------|:-----|:-------|
| 1 | Список користувачів | Admin | Карти з ім'ям, email, роллю |
| 2 | Видалення користувача | Admin | Confirm dialog |
| 3 | Зміна ролі користувача | Admin | Dialog / inline edit |

### 4.7 📊 ВКЛАДКА: Журнал (AuditLogFragment) — Лише для Admin

**Функції:**
| # | Функція | Ролі | Деталі |
|:--|:--------|:-----|:-------|
| 1 | Список записів | Admin | Таблиця або список подій |
| 2 | Фільтрація по типу | Admin | Entity type, Severity |
| 3 | Пошук по користувачу | Admin | Текстовий пошук |
| 4 | Сортування | Admin | По даті (за замовчуванням - нові) |

---

## 5. Разрешение Ролей (RBAC)

### 5.1 Матриця Доступу (по вкладкам і функціям)

| Вкладка / Функція | User | Manager | Administrator | Device |
| :---------------- | :--: | :-----: | :-----------: | :----: |
| **Препарати:**    |      |         |               |        |
| Перегляд          |  ✅  |   ✅    |      ✅       |   —    |
| Додавання         |  ❌  |   ✅    |      ✅       |   —    |
| Редагування       |  ❌  |   ✅    |      ✅       |   —    |
| Видалення         |  ❌  |   ✅    |      ✅       |   —    |
| Quick Actions     |  ✅  |   ✅    |      ✅       |   —    |
| **Датчики:**      |      |         |               |        |
| Перегляд          |  ✅  |   ✅    |      ✅       |   —    |
| Додавання         |  ❌  |   ✅    |      ✅       |   —    |
| Редагування       |  ❌  |   ✅    |      ✅       |   —    |
| Видалення         |  ❌  |   ✅    |      ✅       |   —    |
| **Сповіщення:**   |      |         |               |        |
| Перегляд          |  ✅  |   ✅    |      ✅       |   —    |
| Позначити читане  |  ✅  |   ✅    |      ✅       |   —    |
| **Інциденти:**    |      |         |               |        |
| Перегляд          |  ❌  |   ✅    |      ✅       |   —    |
| Видалення         |  ❌  |   ❌    |      ✅       |   —    |
| Розв'язання       |  ❌  |   ✅    |      ✅       |   —    |
| **Користувачи:**  |      |         |               |        |
| Перегляд          |  ❌  |   ❌    |      ✅       |   —    |
| Видалення         |  ❌  |   ❌    |      ✅       |   —    |
| Зміна ролі        |  ❌  |   ❌    |      ✅       |   —    |
| **Журнал:**       |      |         |               |        |
| Перегляд          |  ❌  |   ❌    |      ✅       |   —    |
| Фільтрація        |  ❌  |   ❌    |      ✅       |   —    |
| **Налаштування:** |      |         |               |        |
| Тема / Мова       |  ✅  |   ✅    |      ✅       |   —    |
| Зміна пароля      |  ✅  |   ✅    |      ✅       |   —    |
| Logout            |  ✅  |   ✅    |      ✅       |   —    |

### 5.2 Реалізація у коді

```kotlin
// RoleHelper.kt
object RoleHelper {
    fun isAdmin(role: String) = role == "Administrator"
    fun isManager(role: String) = role in listOf("Administrator", "Manager")
    fun canManageMedicines(role: String) = isManager(role)
    fun canViewIncidents(role: String) = isManager(role)
    fun canViewAuditLog(role: String) = isAdmin(role)
    // ... інші методи
}

// Fragment.kt
class MedicinesFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val userRole = authViewModel.currentRole
        val canEdit = RoleHelper.canManageMedicines(userRole)

        binding.addButton.isVisible = canEdit
        binding.deleteButton.isVisible = canEdit
        binding.editButton.isVisible = canEdit
    }
}
```

---

## 6. Дизайн-макети (описові)

### 6.1 Початковий екран (Splash / Auth)

```
┌────────────────────────────────┐
│           [Логотип]            │
│   MedicationManagement         │
│                                │
│ [Email/Phone TextInput]        │
│ [Password TextInput]           │
│ [🔗 Забув пароль]              │
│ [Login Button (Material)]      │
│                                │
│ [Новий користувач? Реєстрація] │
└────────────────────────────────┘
```

### 6.2 Main Activity (BottomNav)

```
┌────────────────────────────────┐
│  [Логотип / Заголовок]         │
│  [Меню ≡]      [🔔] [👤]       │ (Topbar)
├────────────────────────────────┤
│                                │
│    [Вміст вкладки]             │
│                                │
│                                │
├────────────────────────────────┤
│ 💊 │ 🏠 │ 🔔 │ ⚙ │ 📋 │ 👥│   (BottomNav)
└────────────────────────────────┘
```

### 6.3 RecyclerView Item (Препарат)

```
┌────────────────────────────────┐
│ [Мініатюра] │ Назва препарату   │ (Left-click: Details)
│ (Image)    │ Категорія          │ (Right-menu: Edit/Delete)
│            │ Кількість: 50      │
│            │ 📅 Термін: 2027-12 │
└────────────────────────────────┘
```

---

## 7. План реалізації (за пріоритетом)

### ФАЗА 7.1: Infrastructure & API (Тиждень 1)

**Статус:** ✅ Завершено
**Завдання:**

- [x] Додати Retrofit залежності
- [x] Створити ApiService інтерфейси для всіх контролерів
- [x] Реалізувати TokenInterceptor
- [x] Реалізувати ErrorInterceptor (401 logout)
- [x] Налаштувати RetrofitClient singleton
- [x] Створити ViewModel базові класи

**Короткий підсумок:** Реалізовано RetrofitClient, інтерцептори, API-інтерфейси та основні ViewModel/Fragment зв'язки для мобільного застосунку.

**Файли для створення:**

- `api/ApiService.kt` (інтерфейси)
- `api/RetrofitClient.kt`
- `api/TokenInterceptor.kt`
- `api/ErrorInterceptor.kt`
- `viewmodel/MedicinesViewModel.kt`
- `viewmodel/NotificationsViewModel.kt`
- `viewmodel/IoTDevicesViewModel.kt`
- `utils/RoleHelper.kt`

### ФАЗА 7.2: UI Modernization (Тиждень 2)

**Статус:** ✅ Завершено

**Завдання:**

- [x] Замінити DrawerLayout на BottomNavigationView
- [x] Оновити colors.xml з Material 3 палітрою
- [x] Оновити styles.xml (Theme.MaterialComponents)
- [x] Розбити MainActivity на Fragments (по вкладках)
- [x] Оновити всі XML layouts на Material Components
- [x] Додати Loading / Error / Empty state UI

**Підсумок:** завершено оновлення головного екрана, списків препаратів, сповіщень, датчиків, локацій та історії подій препарату у Material 3 стилі.

**Файли для модифікування:**

- `res/layout/activity_main.xml` (BottomNav)
- `res/menu/bottom_nav_menu.xml` (Меню)
- `res/values/colors.xml`
- `res/values/styles.xml`
- `res/layout/fragment_medicines.xml`
- `res/layout/fragment_storage_locations.xml`
- `res/layout/fragment_notifications.xml`
- та ін.

### ФАЗА 7.3: Логіка вкладок (Тиждень 3-4)

**Завдання:**

- [x] MedicinesFragment: Список, фільтр, RBAC
- [x] MedicinesDetailActivity: Деталі, Quick Actions, Lifecycle
- [x] StorageLocationsFragment: Список датчиків, деталі з графіками
- [x] NotificationsFragment: Список, фільтр, позначення читаних
- [x] SettingsFragment: Тема, мова, профіль, logout

**Файли для створення:**

- `fragments/MedicinesFragment.kt`
- `fragments/StorageLocationsFragment.kt`
- `fragments/NotificationsFragment.kt`
- `fragments/SettingsFragment.kt`
- `fragments/IncidentsFragment.kt` (Mgr+)
- `fragments/UsersFragment.kt` (Admin)
- `fragments/AuditLogFragment.kt` (Admin)

### ФАЗА 7.4: Advanced Features (Тиждень 5)

**Завдання:**

- [x] Quick Actions (Вжити/Видати/Утилізувати) з API
- [x] Move препарату між датчиками
- [x] Real-time polling для Notifications (кожні 30 сек)
- [x] Графіки датчиків (MPAndroidChart або OkHttp WebSocket)
- [x] Локалізація (UK/EN)
- [x] Теми (Light/Dark)
- [x] Admin screens: Users (list, delete, role change)

### ФАЗА 7.5: Testing & Polish (Тиждень 6)

**Завдання:**

## 8. Залежності (build.gradle.kts)

- [x] Admin screens: AuditLog (list, filters)

```kotlin
dependencies {
    // AndroidX
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // JWT Decode
    implementation("com.auth0:java-jwt:4.4.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    // Charts (опціонально для графіків датчиків)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
```

---

## 9. Критерії готовності (Definition of Done)

**Мобільний застосунок буде вважатися готовим, коли:**

✅ **Архітектура:**

- [ ] Retrofit інтегрований, всі API тесовані
- [ ] ViewModel + StateFlow запроваджені для всіх вкладок
- [ ] TokenManager та Error Handling працюють надійно

✅ **UI/UX:**

- [ ] BottomNavigationView з 5-7 вкладками функціонує
- [ ] Всі 15+ екранів оновлені на Material Components
- [ ] Loading / Error / Empty states розроблені й протестовані
- [ ] Light & Dark теми перемикаються без помилок

✅ **Функціональність:**

- [ ] Користувач може увійти / зареєструватися / вийти
- [ ] Користувач може переглядати, додавати, редагувати, видаляти препарати
- [ ] Quick Actions (Вжити/Видати) записують Lifecycle события
- [ ] Датчики показуються з останніми показниками T°/RH
- [ ] Сповіщення приходять і можуть бути позначені як прочитані
- [ ] RBAC працює: User не бачить Інциденти, Admin не бачить Users

✅ **Якість:**

- [ ] Жодних 🔴 Red errors у Logcat при нормальному використанні
- [ ] Додаток не падає при повороті екрану
- [ ] Сповіщення оновлюються кожні 30 сек (polling)
- [ ] Токен автоматично очищається при 401 помилці

✅ **Тестування:**

- [ ] Unit Tests для всіх ViewModels (>=10 тестів)
- [ ] Integration Tests для API endpoints (>=5 тестів)
- [ ] UI Tests для основних flow'ів (>=3 тести)

---

## 10. Документація розробника

### 10.1 Запуск проєкту

```bash
# 1. Синхронізація Gradle
./gradlew sync

# 2. Запуск на емуляторі
./gradlew installDebug

# 3. Запуск тестів
./gradlew test

# 4. Build Release
./gradlew assembleRelease
```

### 10.2 Структура проєкту після модернізації

```
Mobile/MedicationManagement/app/src/main/
├─ java/com/example/medicationmanagement/
│  ├─ api/
│  │  ├─ ApiService.kt            (All API interfaces)
│  │  ├─ RetrofitClient.kt        (Singleton)
│  │  ├─ TokenInterceptor.kt
│  │  └─ ErrorInterceptor.kt
│  ├─ viewmodel/
│  │  ├─ AuthViewModel.kt
│  │  ├─ MedicinesViewModel.kt
│  │  ├─ NotificationsViewModel.kt
│  │  ├─ IoTDevicesViewModel.kt
│  │  ├─ IncidentsViewModel.kt
│  │  ├─ UsersViewModel.kt
│  │  └─ AuditLogViewModel.kt
│  ├─ fragments/
│  │  ├─ MedicinesFragment.kt
│  │  ├─ StorageLocationsFragment.kt
│  │  ├─ NotificationsFragment.kt
│  │  ├─ IncidentsFragment.kt
│  │  ├─ UsersFragment.kt
│  │  ├─ AuditLogFragment.kt
│  │  └─ SettingsFragment.kt
│  ├─ activities/
│  │  ├─ LoginActivity.kt
│  │  ├─ RegisterActivity.kt
│  │  ├─ MainActivity.kt
│  │  ├─ MedicineDetailsActivity.kt
│  │  ├─ AddMedicineActivity.kt
│  │  ├─ EditMedicineActivity.kt
│  │  ├─ IoTDeviceDetailsActivity.kt
│  │  ├─ AddIoTDeviceActivity.kt
│  │  └─ EditIoTDeviceActivity.kt
│  ├─ model/
│  │  ├─ MedicineDto.kt
│  │  ├─ StorageLocationDto.kt
│  │  ├─ NotificationDto.kt
│  │  ├─ IncidentDto.kt
│  │  ├─ UserDto.kt
│  │  ├─ AuditLogDto.kt
│  │  ├─ MedicineLifecycleEventDto.kt
│  │  └─ LoginResponse.kt
│  ├─ utils/
│  │  ├─ RoleHelper.kt
│  │  ├─ DateFormatter.kt
│  │  ├─ NotificationHelper.kt
│  │  └─ TokenManager.kt
│  └─ MedicationManagementApp.kt
│
└─ res/
   ├─ layout/
   │  ├─ activity_main.xml
   │  ├─ activity_login.xml
   │  ├─ activity_register.xml
   │  ├─ fragment_medicines.xml
   │  ├─ fragment_storage_locations.xml
   │  ├─ fragment_notifications.xml
   │  ├─ fragment_incidents.xml
   │  ├─ fragment_users.xml
   │  ├─ fragment_audit_log.xml
   │  ├─ fragment_settings.xml
   │  ├─ item_medicine.xml
   │  ├─ item_notification.xml
   │  └─ ...
   ├─ menu/
   │  └─ bottom_nav_menu.xml
   ├─ values/
   │  ├─ colors.xml (Material 3)
   │  ├─ strings.xml (uk/values-en)
   │  ├─ styles.xml (Theme.MaterialComponents)
   │  └─ dimens.xml
   ├─ drawable/
   │  ├─ ic_medicines.xml
   │  ├─ ic_devices.xml
   │  ├─ ic_notifications.xml
   │  ├─ ic_empty_state.xml
   │  └─ ...
   └─ AndroidManifest.xml
```

---

## 11. Наступні кроки після модернізації

1. **Фаза 7.6 (опціонально):** Міграція на Jetpack Compose для 1-2 critical screens
2. **Фаза 8:** DevOps & Deployment (Google Play, CI/CD)
3. **Фаза 9:** Документація & Диплом Presentation

---

## 12. Примітки розробника

- **Базова URL для API:** `http://10.0.2.2:5000/` (для Android Emulator). На реальному пристрої змінити на IP машини.
- **SharedPreferences для токена:** ключ `"token"`, зберігати й очищати при logout.
- **Polling для сповіщень:** Запускається в `onStart()` Fragment'у, зупиняється в `onStop()`.
- **Перевирок стану rotation:** Обов'язково використовувати `ViewModel` для всіх станів, щоб не втратити дані при rotate.
- **Обробка 401 errors:** Глобально у OkHttp Interceptor, не в окремих API-викликах.

---

**Версія документа:** 1.0  
**Остання актуалізація:** 2026-05-08  
**Статус:** Готовий до реалізації

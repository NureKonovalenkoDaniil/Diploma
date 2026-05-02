# План покращення мобільного додатку (Фаза 5)

**Дата:** 2026-05-03  
**Область:** Мобільний додаток для домашніх користувачів  
**Статус:** Планування та аналіз

---

## 1. Поточний стан мобільного додатку

### 1.1 Архітектура

**Проблеми:**

- ❌ Нема MVVM архітектури (нема ViewModel, Live­Data / StateFlow)
- ❌ Нема Repository pattern'у
- ❌ Нема Dependency Injection (Hilt)
- ❌業邏гика завантаження даних прямо у Activities/Fragments
- ❌ Нема обробки помилок і retry логіки
- ❌ UI-стан втрачається при повертанні (нема onSaveInstanceState)

**Поточна структура:**

```
Activities (13):
  - LoginActivity, RegisterActivity
  - MainActivity (контейнер для фрагментів)
  - DashboardActivity, DashboardFragment
  - MedicineDetailsActivity, EditMedicineActivity, AddMedicineActivity
  - DeviceDetailsActivity, EditDeviceActivity, AddDeviceActivity
  - (більше не використовуються: StorageConditionsFragment, LogsFragment)

Fragments (4):
  - DashboardFragment (основна таблиця препаратів)
  - DevicesFragment (таблиця IoT-пристроїв)
  - StorageConditionsFragment (умови зберігання)
  - LogsFragment (аудит)
  - SettingsFragment (мова)

Adapters (4):
  - MedicineAdapter (RecyclerView)
  - DeviceAdapter (RecyclerView)
  - AuditLogAdapter (RecyclerView)
  - StorageConditionAdapter (RecyclerView)

Models (4):
  - Medicine (medicineID, name, type, expiryDate, quantity, category)
  - IoTDevice (deviceID, location, type, parameters, isActive, minTemp, maxTemp, minHumidity, maxHumidity)
  - StorageCondition (deviceID, temperature, humidity, timestamp, status)
  - AuditLog (id, action, timestamp, user)
```

### 1.2 HTTP-клієнт і мережа

**Проблеми:**

- ❌ Сирий `HttpURLConnection` без обгортки (кожна операція в окремому `Thread`)
- ❌ Вручну парсування JSON через `org.json.JSONObject/JSONArray`
- ❌ Нема retry логіки при невдачі мережі
- ❌ Нема кеша даних
- ❌ Хардкод токена у SharedPreferences без видалення при логауту
- ❌ Хардкод адреси сервера (http://10.0.2.2:5000) у кожному Activity

**Токен:**

- Зберігається в SharedPreferences під ключем "token"
- Передається у заголовку "Authorization: Bearer {token}"
- Не видаляється при логауту (потенціальна вразливість)

### 1.3 UI/UX

**Проблеми:**

- ❌ Змішування старих XML-layouts (AppCompat) і Compose (незавершено)
- ❌ Material 2 (застарілий, лучше Material 3)
- ❌ RecyclerView не має жодного animation'ю або smooth transitions
- ❌ Нема dark theme підтримки (лише Material 2 default)
- ❌ Нема адаптивного дизайну (погано на планшетах)
- ❌ Немає пошуку на StorageConditionsFragment, DevicesFragment
- ❌ SettingsFragment дуже мінімальний (тільки мова)
- ❌ Нема індикаторів завантаження (loading spinners)
- ❌ Нема пустих стану (empty state) для списків

**Позитивне:**

- ✅ BottomNavigationView для навігації (основна структура OK)
- ✅ EditText пошуку на DashboardFragment
- ✅ AlertDialog для підтвердження видалення

### 1.4 Модели даних

**Проблеми:**

- ❌ `medicineID` замість `id` (не совпадає з backend моделлю)
- ❌ Нема поля `status` у Medicine (Active, Expired, Disposed)
- ❌ Нема поля `storageLocationId` у Medicine
- ❌ Нема поля `manufacturer`, `batchNumber` у Medicine
- ❌ IoTDevice має int ID, а backend має string (DeviceId)
- ❌ Нема моделі `StorageLocation`
- ❌ Нема моделі `StorageIncident`
- ❌ Нема моделі `MedicineLifecycleEvent`
- ❌ Нема моделі `Notification`

### 1.5 Функціональність

**Реалізовано:**

- ✅ Login / Register
- ✅ Перегляд препаратів (DashboardFragment)
- ✅ Додавання препаратів (AddMedicineActivity)
- ✅ Редагування препаратів (EditMedicineActivity)
- ✅ Видалення препаратів (MedicineDetailsActivity)
- ✅ Перегляд пристроїв (DevicesFragment)
- ✅ Додавання пристроїв (AddDeviceActivity)
- ✅ Редагування пристроїв (EditDeviceActivity)
- ✅ Перегляд умов зберігання (StorageConditionsFragment)
- ✅ Перегляд логів аудиту (LogsFragment)
- ✅ Мультимовність (UA/EN)

**Проблеми з функціональністю:**

- ❌ Додавання/редагування препаратів слід зробити лише для менеджерів/адмінів (не для домашніх користувачів)
- ❌ Управління пристроями слід зробити лише для адмінів (не для домашніх користувачів)
- ❌ Логи аудиту слід приховати для домашніх користувачів
- ❌ Нема сповіщень про терміни придатності препаратів
- ❌ Нема сповіщень про порушення умов зберігання
- ❌ Нема графіків для умов зберігання (температури, вологості)

---

## 2. Концепція мобільного додатку для домашніх користувачів

### 2.1 Цільова аудиторія

**Домашній користувач (User role):**

- Просто переглядає препарати, що зберігаються у його домі
- Отримує сповіщення про терміни придатності
- Отримує сповіщення про проблеми зі зберіганням (температура, вологість)
- Видит умови зберігання у реальному часі
- Не може додавати/редагувати препарати або пристрої (це робить менеджер/адмін у web)

**Менеджер (Manager role):**

- Опціонально для мобільного (якщо часу вистачить)
- Додавання/редагування препаратів
- Управління локаціями зберігання
- Перегляд інцидентів
- Отримання алертів про проблеми

**Адмін (Administrator role):**

- Не потребує мобільного (робить все у web)

### 2.2 Основні сценарії використання (User)

1. **Вхід у систему**
   - Вводить email та пароль
   - Додаток зберігає токен
   - Перенаправляється на dashboard

2. **Перегляд препаратів дома**
   - Видит список препаратів у його організації
   - Видит статус (Active, Expired, Disposed)
   - Видит кількість і термін придатності
   - Фільтрує за статусом (знайти протерміновані препарати)
   - Клікає на препарат → видит детальну інформацію

3. **Отримання сповіщень**
   - Сповіщення про препарати що закінчуються через 3 дні
   - Сповіщення про температуру/вологість, що вихід за межі
   - Список сповіщень у окремій вкладці
   - Позначити як прочитане

4. **Перегляд умов зберігання**
   - Графік температури за останні 24 години
   - Графік вологості за останні 24 години
   - Поточні значення для кожного пристрою
   - Норми для кожного пристрою (min/max)

5. **Профіль**
   - Email, ім'я
   - Опціонально: зміна пароля
   - Логаут

6. **Налаштування**
   - Мова (UA/EN)
   - Темна/світла тема
   - Сповіщення (вкл/вимк)

---

## 3. Рекомендовані зміни

### 3.1 Архітектура (MVVM + Repository)

**Структура папок:**

```
src/main/java/com/example/medicationmanagement/
├── ui/
│   ├── screens/
│   │   ├── LoginScreen.kt (Compose)
│   │   ├── DashboardScreen.kt (Compose)
│   │   ├── MedicineDetailScreen.kt (Compose)
│   │   ├── NotificationsScreen.kt (Compose)
│   │   ├── StorageConditionsScreen.kt (Compose)
│   │   ├── ProfileScreen.kt (Compose)
│   │   └── SettingsScreen.kt (Compose)
│   ├── components/
│   │   ├── MedicineCard.kt
│   │   ├── NotificationCard.kt
│   │   ├── StorageChart.kt
│   │   ├── LoadingIndicator.kt
│   │   ├── ErrorBanner.kt
│   │   └── BottomBar.kt
│   └── viewmodels/
│       ├── LoginViewModel.kt
│       ├── DashboardViewModel.kt
│       ├── MedicineDetailViewModel.kt
│       ├── NotificationsViewModel.kt
│       ├── StorageConditionsViewModel.kt
│       └── ProfileViewModel.kt
├── data/
│   ├── remote/
│   │   ├── ApiClient.kt (Retrofit)
│   │   ├── ApiService.kt (interface)
│   │   └── AuthInterceptor.kt
│   ├── local/
│   │   ├── TokenManager.kt (SharedPreferences wrapper)
│   │   └── LocalDatabase.kt (опціонально: Room DB для кеша)
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── MedicineRepository.kt
│   │   ├── NotificationRepository.kt
│   │   ├── StorageConditionRepository.kt
│   │   └── ProfileRepository.kt
│   └── model/
│       ├── Medicine.kt (оновлена)
│       ├── IoTDevice.kt (оновлена)
│       ├── Notification.kt
│       ├── StorageCondition.kt (оновлена)
│       ├── StorageLocation.kt
│       └── ApiResponse.kt (wrapper)
├── navigation/
│   └── NavGraph.kt (Compose Navigation)
├── di/
│   └── AppModule.kt (Hilt)
└── utils/
    ├── DateFormatter.kt
    ├── Constants.kt
    └── Extension functions.kt
```

### 3.2 Залежності (dependencies)

**Додати до build.gradle.kts:**

```kotlin
dependencies {
    // Retrofit + OkHttp для HTTP
    implementation("com.squareup.retrofit2:retrofit:2.10.0")
    implementation("com.squareup.retrofit2:converter-gson:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Gson для JSON парсування
    implementation("com.google.code.gson:gson:2.10.1")

    // Jetpack Compose (оновити версії)
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel, LiveData, Flow
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Hilt для DI
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")

    // Room для локального кеша (опціонально)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Material Icons
    implementation("androidx.compose.material:material-icons-extended:1.6.7")

    // Charts (для графіків умов зберігання)
    implementation("com.patrykandpatrick.vico:compose:1.14.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

### 3.3 HTTP-клієнт (Retrofit + OkHttp)

**ApiService.kt:**

```kotlin
interface ApiService {
    // Auth
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/logout")
    suspend fun logout(): Unit

    // Medicines (User: read-only)
    @GET("api/medicine")
    suspend fun getMedicines(): List<MedicineDto>

    @GET("api/medicine/{id}")
    suspend fun getMedicineDetail(@Path("id") id: String): MedicineDto

    // Notifications
    @GET("api/notification")
    suspend fun getNotifications(): List<NotificationDto>

    @PATCH("api/notification/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: String): Unit

    // Storage Conditions
    @GET("api/storagecondition")
    suspend fun getStorageConditions(): List<StorageConditionDto>

    // Profile
    @GET("api/auth/me")
    suspend fun getProfile(): UserDto

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Unit
}
```

**ApiClient.kt:**

```kotlin
object ApiClient {
    fun create(tokenManager: TokenManager): ApiService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.medicationmanagement.local/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```

**AuthInterceptor.kt:**

```kotlin
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getToken()
        val request = chain.request().newBuilder()
            .apply {
                if (token != null) {
                    addHeader("Authorization", "Bearer $token")
                }
            }
            .build()
        return chain.proceed(request)
    }
}
```

### 3.4 ViewModel + Repository

**DashboardViewModel.kt (приклад):**

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
    val medicines: StateFlow<List<Medicine>> = _medicines.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _filterStatus = MutableStateFlow<MedicineStatus?>(null)
    val filterStatus: StateFlow<MedicineStatus?> = _filterStatus.asStateFlow()

    init {
        loadMedicines()
    }

    fun loadMedicines() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val medicines = medicineRepository.getMedicines()
                _medicines.value = medicines
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setStatusFilter(status: MedicineStatus?) {
        _filterStatus.value = status
    }
}
```

**MedicineRepository.kt:**

```kotlin
class MedicineRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getMedicines(): List<Medicine> {
        return apiService.getMedicines().map { it.toDomain() }
    }

    suspend fun getMedicineDetail(id: String): Medicine {
        return apiService.getMedicineDetail(id).toDomain()
    }
}
```

### 3.5 UI на Jetpack Compose

**DashboardScreen.kt (приклад):**

```kotlin
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onMedicineClick: (String) -> Unit
) {
    val medicines by viewModel.medicines.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Мої препарати", style = MaterialTheme.typography.headlineMedium)

        // Filter buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = filterStatus == null,
                onClick = { viewModel.setStatusFilter(null) },
                label = { Text("Всі") }
            )
            FilterChip(
                selected = filterStatus == MedicineStatus.EXPIRED,
                onClick = { viewModel.setStatusFilter(MedicineStatus.EXPIRED) },
                label = { Text("Протерміновані") }
            )
        }

        when {
            isLoading -> CircularProgressIndicator()
            error != null -> ErrorBanner(error!!)
            medicines.isEmpty() -> EmptyStateMessage("Препаратів не знайдено")
            else -> {
                LazyColumn {
                    items(medicines) { medicine ->
                        MedicineCard(
                            medicine = medicine,
                            onClick = { onMedicineClick(medicine.id) }
                        )
                    }
                }
            }
        }
    }
}
```

### 3.6 Оновлені моделі

**Medicine.kt:**

```kotlin
data class Medicine(
    val id: String,
    val name: String,
    val type: String,
    val manufacturer: String,
    val batchNumber: String,
    val quantity: Int,
    val expiryDate: LocalDate,
    val status: MedicineStatus,
    val minStorageTemp: Double,
    val maxStorageTemp: Double,
    val description: String,
    val storageLocationId: String,
    val createdAt: LocalDateTime
)

enum class MedicineStatus {
    ACTIVE, EXPIRED, DISPOSED, RECALLED
}
```

**Notification.kt:**

```kotlin
data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: LocalDateTime,
    val targetRole: String,
    val organizationId: String
)

enum class NotificationType {
    EXPIRY_WARNING, STORAGE_CONDITION_BREACH, STORAGE_CONDITION_RESOLVED, STOCK_LOW
}
```

### 3.7 Функціональність для домашніх користувачів

**ЗБЕРЕГТИ:**

- ✅ Перегляд препаратів (фільтр за статусом, пошук)
- ✅ Деталі препарату (без редагування)
- ✅ Перегляд сповіщень (з позначенням як прочитане)
- ✅ Умови зберігання з графіками
- ✅ Профіль користувача (email, дата реєстрації)
- ✅ Зміна пароля (опціонально)
- ✅ Налаштування (мова, темна тема)
- ✅ Логін / Логаут

**ВИДАЛИТИ (тільки для web/менеджерів):**

- ❌ Додавання препаратів
- ❌ Редагування препаратів
- ❌ Видалення препаратів
- ❌ Управління пристроями (додавання, редагування)
- ❌ Управління локаціями
- ❌ Журнал аудиту (лише для админів)
- ❌ Інциденти (лише для менеджерів, можуть бути сповіщення про них)

**ДОДАТИ:**

- ✅ Графіки умов зберігання (temperatura, humidity) за 24 години
- ✅ Алерти при porušenniu умов
- ✅ Сповіщення у темпі реального часу (pull або WebSocket)
- ✅ Пусте стану для списків
- ✅ Індикатори завантаження
- ✅ Обробка помилок з пропозицією retry
- ✅ Темна тема (Material 3)
- ✅ Локалізація (UA/EN)

---

## 4. План імплементації

### Етап 1: Архітектура і побудова фундаменту (1-2 тижні)

1. **Налаштування Hilt DI**
   - Create AppModule
   - Inject ApiService, repositories, viewmodels

2. **HTTP-клієнт (Retrofit)**
   - Create ApiService interface
   - Create ApiClient + AuthInterceptor
   - Create TokenManager (SharedPreferences wrapper)

3. **Основні Repository та ViewModel**
   - AuthRepository, AuthViewModel
   - MedicineRepository, DashboardViewModel
   - NotificationRepository, NotificationsViewModel

4. **Compose Navigation**
   - Setup NavGraph
   - Create basic screens (placeholders)

### Етап 2: UI на Compose (2-3 тижні)

1. **LoginScreen / RegisterScreen (оновити)**
   - Modern Compose UI
   - Material 3 дизайн
   - Обробка помилок

2. **DashboardScreen**
   - Таблиця препаратів
   - Фільтр за статусом
   - Пошук
   - MedicineCard компоненти
   - Empty state, loading, error

3. **MedicineDetailScreen**
   - Деталі препарату
   - БЕЗ кнопок редагування/видалення

4. **NotificationsScreen**
   - Список сповіщень
   - Позначення як прочитане
   - Фільтр за типом

5. **StorageConditionsScreen**
   - Два графіки (température, humidity)
   - Dropdown для вибору пристрою
   - Поточні значення + норми
   - Мінімум за останні 24 години

6. **ProfileScreen**
   - Email, ім'я, дата реєстрації
   - (опціонально) Зміна пароля
   - Логаут

7. **SettingsScreen**
   - Мова (UA/EN)
   - Темна тема (light/dark/system)
   - Сповіщення (вкл/вимк)

### Етап 3: Оптимізація і тестування (1 тиждень)

1. **Обробка помилок**
   - Retry логіка при невдачі мережі
   - Error states у всіх screens
   - Toast / Snackbar сповіщення

2. **Локальний кеш** (опціонально)
   - Room DB для препаратів
   - Offline-first підхід

3. **Перформанс**
   - LazyColumn / LazyRow оптимізація
   - Избігання recomposition
   - Memory leaks перевірка

4. **Testing**
   - Unit тести для ViewModel + Repository
   - UI тести для основних screens

### Етап 4: Фіналізація (1 тиждень)

1. **Іконографія і дизайн**
   - Material Icons для всіх buttons
   - Належна палітра кольорів (Material 3)

2. **Локалізація**
   - String resources для UA/EN
   - Date/Number formatting по локалі

3. **Документація**
   - README для mobile розробників
   - Architecture документація

---

## 5. Очікувані результати

### Потім закінчення Етапу 4:

**Функціональність:**

- ✅ Повнофункціональний мобільний додаток для домашніх користувачів
- ✅ Автентифікація (логін/реєстрація/логаут)
- ✅ Перегляд препаратів з фільтрацією
- ✅ Сповіщення (реальний час або polling)
- ✅ Графіки умов зберігання
- ✅ Профіль та налаштування

**Архітектура:**

- ✅ MVVM + Repository + Hilt DI
- ✅ Jetpack Compose для всього UI
- ✅ Retrofit + OkHttp для HTTP
- ✅ Kotlin Coroutines для async операцій
- ✅ Material 3 дизайн

**Якість:**

- ✅ Нема технічного боргу
- ✅ Unit + UI тести
- ✅ Обробка помилок
- ✅ Responsive design
- ✅ Темна тема
- ✅ Локалізація

---

## 6. Пріоритети

### MUST-HAVE (обов'язково):

1. ✅ MVVM архітектура
2. ✅ Jetpack Compose UI
3. ✅ Retrofit HTTP-клієнт
4. ✅ Перегляд препаратів
5. ✅ Сповіщення
6. ✅ Логін/Логаут

### SHOULD-HAVE (бажано):

7. ✅ Графіки умов зберігання
8. ✅ Темна тема
9. ✅ Обробка помилок + retry
10. ✅ Локальний кеш

### NICE-TO-HAVE (якщо часу вистачить):

11. WebSocket для реальних сповіщень
12. Push-сповіщення
13. Room DB for offline cache
14. Профіль і зміна пароля
15. Export даних (PDF)

---

## 7. Наступні кроки

1. **Погодити рішення** про функціональність з усіма заінтересованими сторонами
2. **Встановити залежності** (Retrofit, Compose, Hilt, etc)
3. **Налаштувати структуру проєкту** (папки, пакети)
4. **Почати Етап 1** (DI + HTTP-клієнт)

---

**Статус:** Готово до обговорення і затвердження  
**Дата:** 2026-05-03

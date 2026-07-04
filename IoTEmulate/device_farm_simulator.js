const fs = require('fs');
const path = require('path');

// Базовий URL бекенду
const BACKEND_URL = 'http://172.16.1.2:5001';

// Налаштування пристроїв для симуляції
const devices = [
  {
    id: 'WOKWI-SENSOR-A1',
    label: 'Холодильник №1',
    baseTemp: 10.0, // Центр норми 2..8
    baseHum: 45.0,
    tempFluctuate: 1.5,
    humFluctuate: 5.0,
  },
  {
    id: 'WOKWI-SENSOR-B2',
    label: 'Основний склад',
    baseTemp: 19.5, // Центр норми 15..25
    baseHum: 50.0,
    tempFluctuate: 2.0,
    humFluctuate: 4.0,
  },
  {
    id: 'WOKWI-SENSOR-C3',
    label: 'Морозильна камера',
    baseTemp: -18.0, // Центр норми -22..-15
    baseHum: 65.0,
    tempFluctuate: 1.2,
    humFluctuate: 3.0,
  },
];

const SECRETS_FILE = path.join(__dirname, 'farm_secrets.json');

// Зчитування або створення локального файлу секретів (імітація NVS)
let farmSecrets = {};
if (fs.existsSync(SECRETS_FILE)) {
  try {
    farmSecrets = JSON.parse(fs.readFileSync(SECRETS_FILE, 'utf8'));
  } catch (e) {
    console.error('Помилка зчитування файлу секретів:', e.message);
  }
}

function saveSecrets() {
  fs.writeFileSync(SECRETS_FILE, JSON.stringify(farmSecrets, null, 2), 'utf8');
}

// Допоміжна функція для HTTP запитів
async function makeRequest(url, method, headers = {}, body = null) {
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
  };
  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${await response.text()}`);
  }
  return response.json();
}

// Запуск симуляції окремого пристрою
async function runDeviceSimulator(device) {
  let deviceSecret = farmSecrets[device.id] || '';
  let token = '';

  console.log(`[${device.id}] Запуск симулятора (${device.label})...`);

  // Крок 1. Claim пристрою, якщо немає секрету
  if (!deviceSecret) {
    console.log(`[${device.id}] Секрет відсутній. Надсилання запиту Claim...`);
    try {
      const claimResult = await makeRequest(
        `${BACKEND_URL}/api/iotdevice/claim`,
        'POST',
        {},
        { deviceId: device.id },
      );
      deviceSecret = claimResult.deviceSecret || claimResult.DeviceSecret;
      if (deviceSecret) {
        farmSecrets[device.id] = deviceSecret;
        saveSecrets();
        console.log(`[${device.id}] Пристрій успішно заклаймлено. Секрет збережено.`);
      }
    } catch (e) {
      console.error(
        `[${device.id}] Помилка Claim: Пристрій вже заклаймлено в БД або не створено в панелі UI.`,
      );
      console.log(`[${device.id}] Спроба увійти за допомогою стандартного з'єднання...`);
    }
  }

  // Крок 2. Отримання JWT токена
  async function authenticate() {
    try {
      console.log(`[${device.id}] Авторизація на сервері...`);
      const authResult = await makeRequest(
        `${BACKEND_URL}/api/auth/device-login`,
        'POST',
        {},
        {
          deviceId: device.id,
          deviceSecret: deviceSecret,
        },
      );
      token = authResult.token || authResult.Token;
      console.log(`[${device.id}] Токен успішно отримано.`);
      return true;
    } catch (e) {
      console.error(`[${device.id}] Помилка авторизації:`, e.message);
      return false;
    }
  }

  const authSuccess = await authenticate();
  if (!authSuccess) {
    console.error(`[${device.id}] Помилка запуску. Симуляцію зупинено.`);
    return;
  }

  // Крок 3. Регулярна відправка телеметрії
  setInterval(async () => {
    // Генерація показань з невеликими випадковими коливаннями
    const tempOffset = (Math.random() - 0.5) * device.tempFluctuate;
    const humOffset = (Math.random() - 0.5) * device.humFluctuate;
    const temperature = parseFloat((device.baseTemp + tempOffset).toFixed(2));
    const humidity = parseFloat((device.baseHum + humOffset).toFixed(2));

    try {
      await makeRequest(
        `${BACKEND_URL}/api/storagecondition`,
        'POST',
        {
          Authorization: `Bearer ${token}`,
        },
        {
          Temperature: temperature,
          Humidity: humidity,
          DeviceID: device.id,
        },
      );
      console.log(
        `[${device.id} - ${device.label}] Надіслано: Temp = ${temperature}°C, Hum = ${humidity}%`,
      );
    } catch (e) {
      console.error(`[${device.id}] Помилка відправки даних:`, e.message);
      // Спроба оновити токен у разі 401 Unauthorized
      if (e.message.includes('401')) {
        console.log(`[${device.id}] Токен застарів, оновлення сесії...`);
        await authenticate();
      }
    }
  }, 10000); // Відправка кожні 10 секунд
}

// Головна функція
async function startFarm() {
  console.log('=== Запуск ферми IoT-емуляторів ===');
  console.log(`Адреса сервера бекенду: ${BACKEND_URL}`);
  console.log(`Кількість симульованих датчиків: ${devices.length}\n`);

  for (const device of devices) {
    await runDeviceSimulator(device);
    // Невелика затримка між запусками пристроїв
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
}

startFarm();

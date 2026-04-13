#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClient.h>
#include <Arduino.h>
#include <Adafruit_Sensor.h>
#include <ArduinoJson.h>
#include <DHT.h>

// DHT Pin and Type
#define DHTPIN 33
#define DHTTYPE DHT22

DHT dht(DHTPIN, DHTTYPE);

// Wi-Fi credentials
const char* ssid = "Wokwi-GUEST";
const char* password = "";

// Server URLs and JWT token
const String deviceConfigUrl = "http://192.168.100.2:5000/api/iotdevice/4";
const String dataSendUrl = "http://192.168.100.2:5000/api/storagecondition";
const String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1laWQiOiI2OGE2N2I1Mi1jZjc3LTQwY2ItYjk0Yy05MTAyYjlmOGZlOTYiLCJ1bmlxdWVfbmFtZSI6ImFkbWluQGdtYWlsLmNvbSIsImVtYWlsIjoiYWRtaW5AZ21haWwuY29tIiwicm9sZSI6IkFkbWluaXN0cmF0b3IiLCJuYmYiOjE3NTA1MjU2NDQsImV4cCI6MTc4MjA2MTY0NCwiaWF0IjoxNzUwNTI1NjQ0LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjUwMDAiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjUwMDAifQ.NpAquCWZXkgKwzIlNpy5USWrGa44F5Q2ypoYDY-4bPI";

// Sensor parameters
int deviceID = 4;
const int buzzerPin = 12; // GPIO для п'єзодинаміка

// Граничні значення, які прийдуть із сервера
float minTemperature = 0.0;
float maxTemperature = 0.0;
float minHumidity = 0.0;
float maxHumidity = 0.0;

WiFiClient client; // HTTPS клієнт

unsigned long lastCheckTime = 0;
unsigned long lastSendTime = 0;

const unsigned long checkInterval = 5000;
const unsigned long sendInterval = 10000; // 10 секунд

// Функція для отримання порогових значень із сервера
void fetchDeviceConfig() {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.begin(client, deviceConfigUrl);
    http.addHeader("Authorization", "Bearer " + jwtToken); // JWT токен
    //http.addHeader("ngrok-skip-browser-warning", "1");

    int httpCode = http.GET();
    if (httpCode == 200) {
      String payload = http.getString();
      Serial.println("Device config received:");
      Serial.println(payload);

      // Парсимо JSON
      JsonDocument doc;
      deserializeJson(doc, payload);

      minTemperature = doc["minTemperature"];
      maxTemperature = doc["maxTemperature"];
      minHumidity = doc["minHumidity"];
      maxHumidity = doc["maxHumidity"];

      Serial.printf("Min Temp: %.2f, Max Temp: %.2f, Min Humidity: %.2f, Max Humidity: %.2f\n",
                    minTemperature, maxTemperature, minHumidity, maxHumidity);
    } else {
      Serial.printf("Failed to fetch device config. HTTP code: %d\n", httpCode);
    }

    http.end();
  } else {
    Serial.println("WiFi not connected!");
  }
}

// Функція для відправки даних на сервер
void sendDataToServer(float temperature, float humidity) {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.begin(client, dataSendUrl);
    http.addHeader("Content-Type", "application/json");
    http.addHeader("Authorization", "Bearer " + jwtToken); // JWT токен
    //http.addHeader("ngrok-skip-browser-warning", "1");

    // Перевірка, чи значення коректні
    if (isnan(temperature) || isnan(humidity)) {
      Serial.println("Invalid sensor data, skipping HTTP POST.");
      return;
    }

    String jsonPayload = "{";
    jsonPayload += "\"Temperature\": " + String(temperature) + ", ";
    jsonPayload += "\"Humidity\": " + String(humidity) + ", ";
    jsonPayload += "\"DeviceID\": " + String(deviceID);
    jsonPayload += "}";

    Serial.println("Sending payload:");
    Serial.println(jsonPayload);

    int httpCode = http.POST(jsonPayload);

    if (httpCode > 0) {
      if (httpCode == 200) {
        Serial.println("Data sent successfully!");
      } else {
        Serial.printf("Failed to send data. HTTP code: %d\n", httpCode);
        Serial.println(http.getString());
      }
    } else {
      Serial.printf("HTTP POST failed, error: %s\n", http.errorToString(httpCode).c_str());
    }

    http.end();
  } else {
    Serial.println("WiFi not connected!");
  }
}

void setup() {
  Serial.begin(115200);
  dht.begin(); // Ініціалізація DHT22 датчика

  pinMode(buzzerPin, OUTPUT); // Налаштування п'єзодинаміка
  digitalWrite(buzzerPin, LOW); // Вимкнути сигнал

  // Ініціалізація LEDC (канал 0, частота 800 Гц, 8-бітний PWM)
  ledcSetup(0, 800, 8);
  ledcAttachPin(buzzerPin, 0); // Підключаємо GPIO12 до каналу 0

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
  }

  Serial.println("\nWiFi connected!");

  //client.setInsecure(); // Встановлення ненадійного з'єднання

  // Отримання порогових значень із сервера
  fetchDeviceConfig();
}

void loop() {
  unsigned long currentTime = millis();

  if (currentTime - lastCheckTime >= checkInterval) {
    lastCheckTime = currentTime;

    float temperature = dht.readTemperature(); // Читання температури з DHT22
    float humidity = dht.readHumidity();        // Читання вологості з DHT22

    if (isnan(temperature) || isnan(humidity)) {
      Serial.println("Failed to read from DHT sensor!");
      return;
    }

    Serial.printf("Temperature: %.2f°C, Humidity: %.2f%%\n", temperature, humidity);

    if (temperature < minTemperature || temperature > maxTemperature || 
        humidity < minHumidity || humidity > maxHumidity) {
      Serial.println("Storage conditions violated!");
      ledcWriteTone(0, 800);
      delay(800);
      ledcWriteTone(0, 0);
    }
  }

  if (currentTime - lastSendTime >= sendInterval) {
    lastSendTime = currentTime;

    float temperature = dht.readTemperature(); // Читання температури з DHT22
    float humidity = dht.readHumidity();        // Читання вологості з DHT22

    if (!isnan(temperature) && !isnan(humidity)) {
      sendDataToServer(temperature, humidity);
    } else {
      Serial.println("Invalid sensor data, skipping data send.");
    }
  }
}
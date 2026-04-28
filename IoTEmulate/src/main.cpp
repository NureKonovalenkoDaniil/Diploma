#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClient.h>
#include <Arduino.h>
#include <Adafruit_Sensor.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include "config.h"  // <-- Всі чутливі константи тут (не в VCS)

// DHT Pin and Type
#define DHTPIN 33
#define DHTTYPE DHT22

DHT dht(DHTPIN, DHTTYPE);

// Server URLs — будуються з config.h
const String deviceConfigUrl = String(SERVER_BASE_URL) + "/api/iotdevice/" + String(DEVICE_ID);
const String dataSendUrl     = String(SERVER_BASE_URL) + "/api/storagecondition";
const String jwtToken        = JWT_TOKEN;

// Sensor parameters
const String deviceID = DEVICE_ID;
const int buzzerPin = 12; // GPIO-пін бузера

// Порогові значення — завантажуються з сервера via fetchDeviceConfig()
float minTemperature = 0.0;
float maxTemperature = 0.0;
float minHumidity    = 0.0;
float maxHumidity    = 0.0;

WiFiClient client;

unsigned long lastCheckTime = 0;
unsigned long lastSendTime  = 0;

const unsigned long checkInterval = 5000;
const unsigned long sendInterval  = 10000; // 10 секунд

// Отримати конфігурацію пристрою: порогові значення з сервера
void fetchDeviceConfig() {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.begin(client, deviceConfigUrl);
    http.addHeader("Authorization", "Bearer " + jwtToken);

    int httpCode = http.GET();
    if (httpCode == 200) {
      String payload = http.getString();
      Serial.println("Device config received:");
      Serial.println(payload);

      JsonDocument doc;
      deserializeJson(doc, payload);

      minTemperature = doc["minTemperature"];
      maxTemperature = doc["maxTemperature"];
      minHumidity    = doc["minHumidity"];
      maxHumidity    = doc["maxHumidity"];

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

// Надіслати дані датчика на сервер
void sendDataToServer(float temperature, float humidity) {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.begin(client, dataSendUrl);
    http.addHeader("Content-Type", "application/json");
    http.addHeader("Authorization", "Bearer " + jwtToken);

    if (isnan(temperature) || isnan(humidity)) {
      Serial.println("Invalid sensor data, skipping HTTP POST.");
      return;
    }

    String jsonPayload = "{";
    jsonPayload += "\"Temperature\": " + String(temperature) + ", ";
    jsonPayload += "\"Humidity\": "    + String(humidity)    + ", ";
    jsonPayload += "\"DeviceID\": \"" + deviceID + "\"";
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
  dht.begin();

  pinMode(buzzerPin, OUTPUT);
  digitalWrite(buzzerPin, LOW);

  // Ініціалізація LEDC (канал 0, частота 800 Гц, 8-бітний PWM)
  ledcSetup(0, 800, 8);
  ledcAttachPin(buzzerPin, 0);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
  }

  Serial.println("\nWiFi connected!");

  // Завантажити конфігурацію порогів з сервера
  fetchDeviceConfig();
}

void loop() {
  unsigned long currentTime = millis();

  if (currentTime - lastCheckTime >= checkInterval) {
    lastCheckTime = currentTime;

    float temperature = dht.readTemperature();
    float humidity    = dht.readHumidity();

    if (isnan(temperature) || isnan(humidity)) {
      Serial.println("Failed to read from DHT sensor!");
      return;
    }

    Serial.printf("Temperature: %.2fC, Humidity: %.2f%%\n", temperature, humidity);

    if (temperature < minTemperature || temperature > maxTemperature ||
        humidity    < minHumidity    || humidity    > maxHumidity) {
      Serial.println("Storage conditions violated!");
      ledcWriteTone(0, 800);
      delay(800);
      ledcWriteTone(0, 0);
    }
  }

  if (currentTime - lastSendTime >= sendInterval) {
    lastSendTime = currentTime;

    float temperature = dht.readTemperature();
    float humidity    = dht.readHumidity();

    if (!isnan(temperature) && !isnan(humidity)) {
      sendDataToServer(temperature, humidity);
    } else {
      Serial.println("Invalid sensor data, skipping data send.");
    }
  }
}
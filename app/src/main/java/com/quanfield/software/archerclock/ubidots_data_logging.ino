#include "UbidotsMicroESP8266.h"

#define TOKEN  "A1E-XeoZ9jrsFuj7lD3l7gUq2MvvSWyWyw"
#define ID_1 "59f9e1b6c03f970a8dba698d"
#define WIFISSID "Free WiFi"
#define PASSWORD "1988acca"

Ubidots client(TOKEN);

void setup() {
    Serial.begin(115200);
    client.wifiConnection(WIFISSID, PASSWORD);
    //client.setDebug(true);
}

void loop() {
    float value1 = analogRead(0);
    client.add(ID_1, 1000 - value1);
    client.sendAll(false);
    ESP.deepSleep(30e6);
}

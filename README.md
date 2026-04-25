# Keeper-Eye

**Guia segura en cada paso**

App multiplataforma (Android + iOS) de accesibilidad para personas con discapacidad visual. Detecta texto y obstaculos con la camara en tiempo real, con alertas por voz y vibracion. Todo funciona offline sin backend.

## Funcionalidades

### Escaneo OCR
- OCR en tiempo real con Google ML Kit Text Recognition (on-device)
- Lectura por voz automatica con Flutter TTS
- Bounding boxes visuales sobre el texto detectado
- Deteccion de palabras clave (peligro, alto, salida, emergencia) con alertas prioritarias
- Control de velocidad de voz (0.5x - 2.0x)
- Vibracion tactil al detectar texto
- Historial de textos en SQLite local

### Detector de Obstaculos
- ML Kit Object Detection (on-device) en tiempo real
- Analisis de posicion: izquierda / centro / derecha
- Estimacion de proximidad: cerca / media / lejos
- Alertas por voz: "Obstaculo al frente, muy cerca"
- Vibracion variable por distancia
- Bounding boxes con colores: rojo (cerca), naranja (medio), azul (lejos)

### Accesibilidad
- UI de alto contraste con tema oscuro
- Botones grandes con iconos
- Vibracion como feedback tactil

## Stack

| Componente | Tecnologia |
|---|---|
| Framework | Flutter 3.x |
| Lenguaje | Dart |
| OCR | google_mlkit_text_recognition |
| Obstaculos | google_mlkit_object_detection |
| TTS | flutter_tts |
| BD | sqflite |
| Camara | camera (CameraX / AVFoundation) |

## Build

### Android (APK)
```bash
flutter pub get
flutter build apk --debug
# APK en: build/app/outputs/flutter-apk/app-debug.apk
```

### iOS (requiere Mac con Xcode)
```bash
flutter pub get
cd ios && pod install && cd ..
flutter build ios
```

## Requisitos
- Flutter SDK 3.x
- Android: Min SDK 24 (Android 7.0)
- iOS: Min iOS 12.0

# KEEPER EYE

Aplicacion movil Android nativa enfocada en accesibilidad para personas con discapacidad visual. Utiliza OCR en tiempo real y lectura por voz, funcionando completamente offline sin necesidad de backend.

## Caracteristicas

- **OCR en tiempo real** - Deteccion de texto con la camara usando Google ML Kit (on-device)
- **Lectura por voz** - Reproduccion automatica del texto detectado usando Android TTS
- **Deteccion de palabras clave** - Prioriza la lectura de palabras como "peligro", "alto", "salida"
- **Accesibilidad** - Botones grandes, alto contraste, vibracion al detectar texto
- **Historial** - Guarda los ultimos textos detectados en SQLite local
- **100% Offline** - Todo el procesamiento es local, sin envio de datos a internet
- **Privacidad** - Ningun dato sale del dispositivo

## Stack Tecnologico

| Componente | Tecnologia |
|---|---|
| Lenguaje | Kotlin |
| UI | Material Design 3 |
| Camara | CameraX |
| OCR | Google ML Kit Text Recognition (on-device) |
| Voz | Android TextToSpeech API |
| Base de datos | SQLite |
| Min SDK | API 24 (Android 7.0) |
| Target SDK | API 34 (Android 14) |

## Estructura del Proyecto

```
app/src/main/
├── java/com/keepereye/app/
│   ├── MainActivity.kt          # Pantalla principal
│   ├── ScanActivity.kt          # Camara + OCR + TTS
│   ├── HistoryActivity.kt       # Historial de textos
│   ├── ocr/
│   │   ├── TextRecognitionAnalyzer.kt  # Procesamiento ML Kit
│   │   └── TextOverlayView.kt         # Bounding boxes
│   ├── tts/
│   │   └── TextToSpeechManager.kt     # Gestion de voz
│   ├── ai/
│   │   └── KeywordDetector.kt         # Deteccion de palabras clave
│   └── history/
│       ├── HistoryEntry.kt            # Modelo de datos
│       ├── HistoryDatabaseHelper.kt   # SQLite helper
│       ├── HistoryRepository.kt       # Repositorio
│       └── HistoryAdapter.kt          # RecyclerView adapter
└── res/
    ├── layout/         # Layouts XML
    ├── values/         # Strings, colors, themes
    └── drawable/       # Iconos vectoriales
```

## Compilacion

### Requisitos
- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17
- Android SDK 34

### Generar APK Debug
```bash
./gradlew assembleDebug
```
El APK se generara en `app/build/outputs/apk/debug/app-debug.apk`

### Generar APK Release
```bash
./gradlew assembleRelease
```

## Permisos

- `CAMERA` - Para escaneo OCR en tiempo real
- `VIBRATE` - Para feedback tactil al detectar texto

## Palabras Clave Detectadas

| Palabra | Prioridad | Tipo |
|---|---|---|
| peligro, danger, cuidado, emergencia | CRITICA | Alerta inmediata con vibracion larga |
| alto, stop, pare, no pasar, prohibido, precaucion | ALTA | Alerta con vibracion |
| salida, exit, entrada, escalera, ascensor, bano | MEDIA | Aviso informativo |

## Licencia

Proyecto privado - Todos los derechos reservados.

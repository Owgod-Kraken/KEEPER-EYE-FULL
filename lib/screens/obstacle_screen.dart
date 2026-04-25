import 'dart:ui' as ui;
import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:google_mlkit_object_detection/google_mlkit_object_detection.dart';
import 'package:vibration/vibration.dart';
import '../theme/app_theme.dart';
import '../services/tts_service.dart';
import '../models/detected_obstacle.dart';
import '../widgets/obstacle_overlay_painter.dart';

class ObstacleScreen extends StatefulWidget {
  const ObstacleScreen({super.key});

  @override
  State<ObstacleScreen> createState() => _ObstacleScreenState();
}

class _ObstacleScreenState extends State<ObstacleScreen> {
  CameraController? _cameraController;
  late final ObjectDetector _objectDetector;
  final _ttsService = TtsService();

  bool _isDetecting = false;
  List<DetectedObstacle> _obstacles = [];
  String _lastAlertMessage = '';
  int _lastAlertTime = 0;
  double _speechRate = 1.0;
  Size _imageSize = const Size(480, 640);

  @override
  void initState() {
    super.initState();

    final options = ObjectDetectorOptions(
      mode: DetectionMode.stream,
      classifyObjects: true,
      multipleObjects: true,
    );
    _objectDetector = ObjectDetector(options: options);

    _initCamera();
    _ttsService.initialize();
  }

  Future<void> _initCamera() async {
    final cameras = await availableCameras();
    if (cameras.isEmpty) return;

    final backCamera = cameras.firstWhere(
      (c) => c.lensDirection == CameraLensDirection.back,
      orElse: () => cameras.first,
    );

    _cameraController = CameraController(
      backCamera,
      ResolutionPreset.medium,
      enableAudio: false,
      imageFormatGroup: ImageFormatGroup.nv21,
    );

    await _cameraController!.initialize();
    if (!mounted) return;
    setState(() {});

    _cameraController!.startImageStream(_processImage);
  }

  void _processImage(CameraImage image) {
    if (_isDetecting) return;
    _isDetecting = true;

    final inputImage = _convertCameraImage(image);
    if (inputImage == null) {
      _isDetecting = false;
      return;
    }

    _objectDetector.processImage(inputImage).then((objects) {
      if (!mounted) return;

      final sensorOrientation =
          _cameraController!.description.sensorOrientation;
      final int imgWidth;
      final int imgHeight;
      if (sensorOrientation == 90 || sensorOrientation == 270) {
        imgWidth = image.height;
        imgHeight = image.width;
      } else {
        imgWidth = image.width;
        imgHeight = image.height;
      }

      final obstacles = objects.map((obj) {
        return _processDetectedObject(obj, imgWidth, imgHeight);
      }).toList();

      setState(() {
        _obstacles = obstacles;
        _imageSize = Size(imgWidth.toDouble(), imgHeight.toDouble());
      });

      if (obstacles.isNotEmpty) {
        obstacles.sort((a, b) {
          final aArea = a.boundingBox.width * a.boundingBox.height;
          final bArea = b.boundingBox.width * b.boundingBox.height;
          return bArea.compareTo(aArea);
        });
        _processAlerts(obstacles.first);
      }

      _isDetecting = false;
    }).catchError((_) {
      _isDetecting = false;
    });
  }

  DetectedObstacle _processDetectedObject(
    DetectedObject obj,
    int imgWidth,
    int imgHeight,
  ) {
    final box = obj.boundingBox;
    final position = _analyzePosition(box, imgWidth);
    final proximity = _analyzeProximity(box, imgWidth, imgHeight);

    String label;
    double confidence;
    if (obj.labels.isNotEmpty) {
      final mlLabel = obj.labels.first;
      label = _classifyObject(mlLabel.index, proximity);
      confidence = mlLabel.confidence;
    } else {
      label = _classifyBySize(proximity);
      confidence = 0.5;
    }

    return DetectedObstacle(
      label: label,
      boundingBox: ui.Rect.fromLTRB(
        box.left.toDouble(),
        box.top.toDouble(),
        box.right.toDouble(),
        box.bottom.toDouble(),
      ),
      position: position,
      proximity: proximity,
      confidence: confidence,
      trackingId: obj.trackingId,
    );
  }

  String _classifyObject(int categoryIndex, ObstacleProximity proximity) {
    return switch (categoryIndex) {
      0 => proximity == ObstacleProximity.near ? 'Persona' : 'Persona u objeto',
      1 => 'Objeto cercano',
      2 => 'Mueble',
      3 => 'Estructura',
      4 => 'Planta',
      _ => 'Obstaculo',
    };
  }

  String _classifyBySize(ObstacleProximity proximity) {
    return switch (proximity) {
      ObstacleProximity.near => 'Obstaculo',
      ObstacleProximity.medium => 'Objeto',
      ObstacleProximity.far => 'Objeto detectado',
    };
  }

  ObstaclePosition _analyzePosition(Rect box, int imgWidth) {
    final centerX = box.center.dx;
    final relativeX = centerX / imgWidth;
    if (relativeX < 0.33) return ObstaclePosition.left;
    if (relativeX > 0.66) return ObstaclePosition.right;
    return ObstaclePosition.center;
  }

  ObstacleProximity _analyzeProximity(Rect box, int imgWidth, int imgHeight) {
    final frameArea = imgWidth * imgHeight;
    final boxArea = box.width * box.height;
    final ratio = boxArea / frameArea;
    if (ratio > 0.12) return ObstacleProximity.near;
    if (ratio > 0.04) return ObstacleProximity.medium;
    return ObstacleProximity.far;
  }

  void _processAlerts(DetectedObstacle obstacle) {
    final now = DateTime.now().millisecondsSinceEpoch;
    final cooldown = switch (obstacle.proximity) {
      ObstacleProximity.near => 1500,
      ObstacleProximity.medium => 3000,
      ObstacleProximity.far => 5000,
    };

    if (now - _lastAlertTime < cooldown) return;

    final alertMessage = obstacle.buildAlertMessage();
    if (alertMessage == _lastAlertMessage && now - _lastAlertTime < cooldown * 2) {
      return;
    }

    _lastAlertTime = now;
    _lastAlertMessage = alertMessage;

    _vibrateForProximity(obstacle.proximity);

    if (obstacle.proximity == ObstacleProximity.near) {
      _ttsService.speakWithPriority(alertMessage);
    } else {
      _ttsService.speak(alertMessage);
    }
  }

  void _vibrateForProximity(ObstacleProximity proximity) {
    switch (proximity) {
      case ObstacleProximity.near:
        Vibration.vibrate(pattern: [0, 200, 100, 200, 100, 200], intensities: [0, 255, 0, 255, 0, 255]);
        break;
      case ObstacleProximity.medium:
        Vibration.vibrate(pattern: [0, 150, 150, 150], intensities: [0, 150, 0, 150]);
        break;
      case ObstacleProximity.far:
        Vibration.vibrate(duration: 80, amplitude: 60);
        break;
    }
  }

  InputImage? _convertCameraImage(CameraImage image) {
    final camera = _cameraController;
    if (camera == null) return null;

    final sensorOrientation = camera.description.sensorOrientation;
    final rotation = InputImageRotationValue.fromRawValue(sensorOrientation);
    if (rotation == null) return null;

    final plane = image.planes.first;
    return InputImage.fromBytes(
      bytes: plane.bytes,
      metadata: InputImageMetadata(
        size: Size(image.width.toDouble(), image.height.toDouble()),
        rotation: rotation,
        format: InputImageFormat.nv21,
        bytesPerRow: plane.bytesPerRow,
      ),
    );
  }

  @override
  void dispose() {
    _cameraController?.dispose();
    _objectDetector.close();
    _ttsService.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          Expanded(
            child: Stack(
              fit: StackFit.expand,
              children: [
                if (_cameraController?.value.isInitialized ?? false)
                  CameraPreview(_cameraController!)
                else
                  const Center(
                    child: CircularProgressIndicator(color: AppColors.cyan),
                  ),
                if (_obstacles.isNotEmpty &&
                    _cameraController?.value.isInitialized == true)
                  CustomPaint(
                    painter: ObstacleOverlayPainter(
                      obstacles: _obstacles,
                      imageSize: _imageSize,
                    ),
                  ),
                Positioned(
                  top: MediaQuery.of(context).padding.top + 8,
                  left: 16,
                  child: _backButton(),
                ),
                Positioned(
                  top: MediaQuery.of(context).padding.top + 8,
                  right: 16,
                  child: _badge('Detector de Obstaculos', AppColors.cyan),
                ),
                if (_obstacles.isNotEmpty)
                  Positioned(
                    top: MediaQuery.of(context).padding.top + 48,
                    right: 16,
                    child: _badge(
                      '${_obstacles.length} objeto(s)',
                      AppColors.textPrimary,
                    ),
                  ),
                if (_obstacles.isEmpty)
                  Positioned(
                    bottom: 16,
                    left: 0,
                    right: 0,
                    child: Center(
                      child: _badge(
                        'Buscando obstaculos...',
                        AppColors.textPrimary,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          _controlPanel(),
        ],
      ),
    );
  }

  Widget _backButton() {
    return Material(
      color: AppColors.overlay,
      shape: const CircleBorder(),
      child: InkWell(
        onTap: () => Navigator.pop(context),
        customBorder: const CircleBorder(),
        child: const Padding(
          padding: EdgeInsets.all(12),
          child: Icon(Icons.arrow_back, color: AppColors.textPrimary, size: 28),
        ),
      ),
    );
  }

  Widget _badge(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: AppColors.overlay,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        text,
        style: TextStyle(
          color: color,
          fontSize: 14,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _controlPanel() {
    return Container(
      color: AppColors.panel,
      padding: const EdgeInsets.all(16),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(
              height: 60,
              child: SingleChildScrollView(
                child: Text(
                  _lastAlertMessage.isNotEmpty ? _lastAlertMessage : '',
                  style: const TextStyle(
                    color: AppColors.cyan,
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: SizedBox(
                    height: 56,
                    child: ElevatedButton.icon(
                      onPressed: () {
                        if (_lastAlertMessage.isNotEmpty) {
                          _ttsService.speak(_lastAlertMessage);
                        }
                      },
                      icon: const Icon(Icons.replay, size: 24),
                      label: const Text('Repetir', style: TextStyle(fontSize: 18)),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.cyan,
                        foregroundColor: AppColors.background,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: SizedBox(
                    height: 56,
                    child: ElevatedButton.icon(
                      onPressed: () => _ttsService.stop(),
                      icon: const Icon(Icons.stop, size: 24),
                      label: const Text('Detener', style: TextStyle(fontSize: 18)),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.red,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Text(
                  'Velocidad: ${_speechRate.toStringAsFixed(1)}x',
                  style: const TextStyle(
                    color: AppColors.textSecondary,
                    fontSize: 14,
                  ),
                ),
                Expanded(
                  child: Slider(
                    value: _speechRate,
                    min: 0.5,
                    max: 2.0,
                    activeColor: AppColors.cyan,
                    onChanged: (value) {
                      setState(() => _speechRate = value);
                      _ttsService.setSpeechRate(value);
                    },
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

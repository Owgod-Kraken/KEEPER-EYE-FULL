import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:vibration/vibration.dart';
import '../theme/app_theme.dart';
import '../services/tts_service.dart';
import '../services/history_service.dart';
import '../services/keyword_detector.dart';
import '../widgets/text_overlay_painter.dart';

class ScanScreen extends StatefulWidget {
  const ScanScreen({super.key});

  @override
  State<ScanScreen> createState() => _ScanScreenState();
}

class _ScanScreenState extends State<ScanScreen> {
  CameraController? _cameraController;
  final _textRecognizer = TextRecognizer(script: TextRecognitionScript.latin);
  final _ttsService = TtsService();
  final _historyService = HistoryService();

  bool _isDetecting = false;
  String _detectedText = '';
  String _lastSpokenText = '';
  RecognizedText? _recognizedText;
  double _speechRate = 1.0;

  @override
  void initState() {
    super.initState();
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

    _textRecognizer.processImage(inputImage).then((recognized) {
      if (!mounted) return;

      final fullText = recognized.text.trim();
      setState(() {
        _recognizedText = recognized;
        _detectedText = fullText;
      });

      if (fullText.isNotEmpty && fullText != _lastSpokenText) {
        _lastSpokenText = fullText;

        Vibration.vibrate(duration: 100);

        final keywordMatch = KeywordDetector.detectKeywords(fullText);
        if (keywordMatch != null) {
          _ttsService.speakWithPriority(
            'Alerta: ${keywordMatch.keyword} detectado. $fullText',
          );
          Vibration.vibrate(duration: 300, amplitude: 255);
        } else {
          _ttsService.speak(fullText);
        }

        _historyService.insertEntry(fullText);
      }

      _isDetecting = false;
    }).catchError((_) {
      _isDetecting = false;
    });
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
    _textRecognizer.close();
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
                    child: CircularProgressIndicator(color: AppColors.blue),
                  ),
                if (_recognizedText != null &&
                    _cameraController?.value.isInitialized == true)
                  CustomPaint(
                    painter: TextOverlayPainter(
                      recognizedText: _recognizedText!,
                      imageSize: Size(
                        _cameraController!.value.previewSize!.height,
                        _cameraController!.value.previewSize!.width,
                      ),
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
                  child: _badge('Escaneo OCR', AppColors.lightBlue),
                ),
                if (_detectedText.isEmpty)
                  Positioned(
                    bottom: 16,
                    left: 0,
                    right: 0,
                    child: Center(
                      child: _badge('Buscando texto...', AppColors.textPrimary),
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
          fontSize: 16,
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
              height: 80,
              child: SingleChildScrollView(
                child: Text(
                  _detectedText.isNotEmpty ? _detectedText : '',
                  style: const TextStyle(
                    color: AppColors.lightBlue,
                    fontSize: 16,
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
                        if (_lastSpokenText.isNotEmpty) {
                          _ttsService.speak(_lastSpokenText);
                        }
                      },
                      icon: const Icon(Icons.replay, size: 24),
                      label: const Text('Repetir', style: TextStyle(fontSize: 18)),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.blue,
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
                    activeColor: AppColors.lightBlue,
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

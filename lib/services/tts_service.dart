import 'package:flutter_tts/flutter_tts.dart';

class TtsService {
  final FlutterTts _tts = FlutterTts();
  bool _isInitialized = false;
  double _speechRate = 1.0;

  Future<void> initialize() async {
    if (_isInitialized) return;
    await _tts.setLanguage('es-ES');
    await _tts.setSpeechRate(_speechRate);
    await _tts.setVolume(1.0);
    await _tts.setPitch(1.0);
    _isInitialized = true;
  }

  Future<void> speak(String text) async {
    await initialize();
    await _tts.speak(text);
  }

  Future<void> speakWithPriority(String text) async {
    await initialize();
    await _tts.stop();
    await _tts.speak(text);
  }

  Future<void> stop() async {
    await _tts.stop();
  }

  Future<void> setSpeechRate(double rate) async {
    _speechRate = rate;
    await _tts.setSpeechRate(rate);
  }

  double get speechRate => _speechRate;

  Future<void> dispose() async {
    await _tts.stop();
  }
}

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../theme/app_theme.dart';
import '../services/tts_service.dart';
import '../services/history_service.dart';
import '../models/history_entry.dart';

class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  final _historyService = HistoryService();
  final _ttsService = TtsService();
  List<HistoryEntry> _entries = [];

  @override
  void initState() {
    super.initState();
    _loadHistory();
    _ttsService.initialize();
  }

  Future<void> _loadHistory() async {
    final entries = await _historyService.getAll();
    if (mounted) {
      setState(() => _entries = entries);
    }
  }

  Future<void> _clearHistory() async {
    await _historyService.clearAll();
    setState(() => _entries = []);
  }

  @override
  void dispose() {
    _ttsService.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.panel,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Historial',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        actions: [
          TextButton(
            onPressed: _entries.isEmpty ? null : _clearHistory,
            child: const Text(
              'Limpiar',
              style: TextStyle(color: AppColors.red, fontSize: 16),
            ),
          ),
        ],
      ),
      body: _entries.isEmpty
          ? const Center(
              child: Text(
                'No hay textos en el historial',
                style: TextStyle(
                  color: AppColors.textSecondary,
                  fontSize: 20,
                ),
              ),
            )
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _entries.length,
              itemBuilder: (context, index) {
                return _buildHistoryCard(_entries[index]);
              },
            ),
    );
  }

  Widget _buildHistoryCard(HistoryEntry entry) {
    final dateFormat = DateFormat('dd/MM/yyyy HH:mm');
    return Card(
      color: AppColors.card,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppColors.cardStroke),
      ),
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 0,
      child: InkWell(
        onTap: () => _ttsService.speak(entry.text),
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                entry.text,
                maxLines: 3,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  color: AppColors.textPrimary,
                  fontSize: 16,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                dateFormat.format(entry.timestamp),
                style: const TextStyle(
                  color: AppColors.textSecondary,
                  fontSize: 13,
                ),
              ),
              const SizedBox(height: 4),
              const Text(
                'Toca para escuchar',
                style: TextStyle(
                  color: AppColors.lightBlue,
                  fontSize: 13,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

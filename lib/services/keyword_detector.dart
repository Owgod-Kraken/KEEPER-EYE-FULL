class KeywordMatch {
  final String keyword;
  final int priority;

  const KeywordMatch({required this.keyword, required this.priority});
}

class KeywordDetector {
  static const Map<String, int> _keywords = {
    'peligro': 3,
    'danger': 3,
    'alto': 2,
    'stop': 2,
    'salida': 2,
    'exit': 2,
    'emergencia': 3,
    'emergency': 3,
    'cuidado': 2,
    'caution': 2,
    'precaución': 2,
    'warning': 2,
    'no pasar': 3,
    'prohibido': 2,
  };

  static KeywordMatch? detectKeywords(String text) {
    final lower = text.toLowerCase();
    KeywordMatch? bestMatch;

    for (final entry in _keywords.entries) {
      if (lower.contains(entry.key)) {
        if (bestMatch == null || entry.value > bestMatch.priority) {
          bestMatch = KeywordMatch(keyword: entry.key, priority: entry.value);
        }
      }
    }
    return bestMatch;
  }
}

import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../models/history_entry.dart';

class HistoryService {
  static Database? _database;

  Future<Database> get database async {
    _database ??= await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'keeper_eye_history.db');
    return openDatabase(
      path,
      version: 1,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE history(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            text TEXT NOT NULL,
            timestamp INTEGER NOT NULL
          )
        ''');
      },
    );
  }

  Future<void> insertEntry(String text) async {
    final db = await database;
    await db.insert('history', {
      'text': text,
      'timestamp': DateTime.now().millisecondsSinceEpoch,
    });
  }

  Future<List<HistoryEntry>> getAll() async {
    final db = await database;
    final maps = await db.query('history', orderBy: 'timestamp DESC');
    return maps.map((m) => HistoryEntry.fromMap(m)).toList();
  }

  Future<void> clearAll() async {
    final db = await database;
    await db.delete('history');
  }
}

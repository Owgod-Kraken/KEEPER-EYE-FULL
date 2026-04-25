import 'package:flutter/material.dart';
import '../models/detected_obstacle.dart';

class ObstacleOverlayPainter extends CustomPainter {
  final List<DetectedObstacle> obstacles;
  final Size imageSize;

  ObstacleOverlayPainter({
    required this.obstacles,
    required this.imageSize,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (imageSize.width == 0 || imageSize.height == 0) return;

    final scaleX = size.width / imageSize.width;
    final scaleY = size.height / imageSize.height;

    for (final obstacle in obstacles) {
      final rect = Rect.fromLTRB(
        obstacle.boundingBox.left * scaleX,
        obstacle.boundingBox.top * scaleY,
        obstacle.boundingBox.right * scaleX,
        obstacle.boundingBox.bottom * scaleY,
      );

      final (strokeColor, fillColor) = switch (obstacle.proximity) {
        ObstacleProximity.near => (
            const Color(0xFFEF5350),
            const Color(0x40EF5350),
          ),
        ObstacleProximity.medium => (
            const Color(0xFFFFB74D),
            const Color(0x30FFB74D),
          ),
        ObstacleProximity.far => (
            const Color(0xFF42A5F5),
            const Color(0x2042A5F5),
          ),
      };

      final fillPaint = Paint()
        ..color = fillColor
        ..style = PaintingStyle.fill;
      final strokePaint = Paint()
        ..color = strokeColor
        ..style = PaintingStyle.stroke
        ..strokeWidth = obstacle.proximity == ObstacleProximity.near ? 4.0 : 2.0;

      canvas.drawRRect(
        RRect.fromRectAndRadius(rect, const Radius.circular(8)),
        fillPaint,
      );
      canvas.drawRRect(
        RRect.fromRectAndRadius(rect, const Radius.circular(8)),
        strokePaint,
      );

      final label = '${obstacle.label} - ${obstacle.proximity.label}';
      final textPainter = TextPainter(
        text: TextSpan(
          text: label,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 14,
            fontWeight: FontWeight.bold,
          ),
        ),
        textDirection: TextDirection.ltr,
      );
      textPainter.layout();

      if (rect.top > 30) {
        final bgRect = Rect.fromLTWH(
          rect.left,
          rect.top - 24,
          textPainter.width + 12,
          22,
        );
        canvas.drawRRect(
          RRect.fromRectAndRadius(bgRect, const Radius.circular(4)),
          Paint()..color = const Color(0xCC000000),
        );
        textPainter.paint(canvas, Offset(rect.left + 6, rect.top - 22));
      }
    }
  }

  @override
  bool shouldRepaint(ObstacleOverlayPainter oldDelegate) {
    return oldDelegate.obstacles != obstacles;
  }
}

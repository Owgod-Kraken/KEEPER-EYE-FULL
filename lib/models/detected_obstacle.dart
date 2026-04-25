import 'dart:ui';

enum ObstaclePosition { left, center, right }

enum ObstacleProximity {
  near('muy cerca'),
  medium('a media distancia'),
  far('lejos');

  final String label;
  const ObstacleProximity(this.label);
}

class DetectedObstacle {
  final String label;
  final Rect boundingBox;
  final ObstaclePosition position;
  final ObstacleProximity proximity;
  final double confidence;
  final int? trackingId;

  const DetectedObstacle({
    required this.label,
    required this.boundingBox,
    required this.position,
    required this.proximity,
    required this.confidence,
    this.trackingId,
  });

  String buildAlertMessage() {
    final posLabel = switch (position) {
      ObstaclePosition.left => 'a la izquierda',
      ObstaclePosition.center => 'al frente',
      ObstaclePosition.right => 'a la derecha',
    };
    return switch (proximity) {
      ObstacleProximity.near => '$label $posLabel, muy cerca',
      ObstacleProximity.medium => '$label $posLabel',
      ObstacleProximity.far => '$label $posLabel, lejos',
    };
  }
}

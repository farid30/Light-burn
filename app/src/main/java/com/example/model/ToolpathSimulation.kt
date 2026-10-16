package com.example.model

import androidx.compose.ui.graphics.Color

data class ToolpathSegment(
  val startX: Float,
  val startY: Float,
  val endX: Float,
  val endY: Float,
  val isRapid: Boolean,
  val powerPercent: Float,
  val speedMmMin: Double,
  val layerColor: Color,
  val layerId: String,
  val passIndex: Int = 1
) {
  val lengthMm: Float
    get() {
      val dx = endX - startX
      val dy = endY - startY
      return kotlin.math.sqrt(dx * dx + dy * dy)
    }

  val durationSeconds: Double
    get() {
      val speedMmSec = if (isRapid) 100.0 else (speedMmMin / 60.0).coerceAtLeast(1.0)
      return (lengthMm / speedMmSec)
    }
}

data class SimulationResult(
  val segments: List<ToolpathSegment> = emptyList(),
  val totalCutDistanceMm: Float = 0f,
  val totalRapidDistanceMm: Float = 0f,
  val totalDurationSeconds: Double = 0.0,
  val boundsMinX: Float = 0f,
  val boundsMinY: Float = 0f,
  val boundsMaxX: Float = 0f,
  val boundsMaxY: Float = 0f
)

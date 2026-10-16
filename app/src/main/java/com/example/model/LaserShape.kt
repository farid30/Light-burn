package com.example.model

import java.util.UUID

enum class ShapeType(val displayName: String) {
  RECTANGLE("Rectangle"),
  CIRCLE("Circle / Ellipse"),
  STAR("Star / Polygon"),
  TEXT("Text"),
  LIVING_HINGE("Living Hinge"),
  CALIBRATION_GRID("Test Matrix"),
  FRAME("Border Frame")
}

data class LaserShape(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val type: ShapeType,
  val layerId: String,          // e.g. "C00"
  val x: Float,                 // mm on bed (left)
  val y: Float,                 // mm on bed (top)
  val width: Float,             // mm
  val height: Float,            // mm
  val rotation: Float = 0f,     // degrees
  val text: String = "",
  val isLocked: Boolean = false,
  val cornerRadius: Float = 0f, // mm for rounded rectangle
  val polygonPoints: Int = 5    // for star / polygon
) {
  val centerX: Float get() = x + width / 2f
  val centerY: Float get() = y + height / 2f

  fun containsPoint(px: Float, py: Float): Boolean {
    return px in x..(x + width) && py in y..(y + height)
  }
}
